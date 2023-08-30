package org.lasantha.fxplanets

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.util.Duration
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin


class PlanetsApp : Application() {
    private val scopeJobs = Job()
    private val scopeThreads = Executors.newFixedThreadPool(2)
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)

    private val width = 1400.0
    private val height = 1000.0
    private val imageLib = ImageLib()
    private val musicLib = MusicLib()
    private val frameDuration = 30L //17 -> 59 FPS,  20 -> 50 FPS, 30 -> 33 FPS
    private val gameLoop = Timeline()
    private val appStartTime = System.currentTimeMillis()

    private var animate = true

    init {
        gameLoop.cycleCount = Timeline.INDEFINITE
    }

    override fun start(stage: Stage) {
        stage.title = "Space"
        stage.icons.add(0, imageLib.icon())
        val root = StackPane()
        val scene = Scene(root)
        stage.setScene(scene)
        val bgCanvas = Canvas(width, height)
        val staticCanvas = Canvas(width, height)
        val dynamicCanvas = Canvas(width, height)
        root.children.addAll(bgCanvas, staticCanvas, dynamicCanvas)

        val centerX = width / 2.0
        val centerY = height / 2.0
        val gcBg = bgCanvas.graphicsContext2D
        gcBg.isImageSmoothing = false
        val gcStatic = staticCanvas.graphicsContext2D
        gcStatic.isImageSmoothing = false
        val gc = dynamicCanvas.graphicsContext2D
        gc.isImageSmoothing = false

        root.style = "-fx-background-color:black"
        gcBg.drawImage(imageLib.bgImage(), 0.0, 0.0)
        val bgTransX = arrayOf(-0.15, -0.07, 0.07, 0.15, 0.07, -0.07, -0.15, -0.07, 0.07, 0.15, 0.07, -0.07)
        val bgTransY = arrayOf(-0.15, -0.07, -0.15, -0.07, 0.07, 0.15, 0.07, -0.07, 0.07, 0.15, 0.07, -0.07)

        scene.setOnMousePressed { e ->
            when (e.button) {
                MouseButton.SECONDARY -> {
                    animate = if (animate) {
                        gameLoop.stop()
                        musicLib.bgMusicPlayer.pause()
                        false
                    } else {
                        gameLoop.play()
                        musicLib.bgMusicPlayer.play()
                        true
                    }
                }

                MouseButton.PRIMARY -> {
                    if (animate) {
                        val effect = javafx.scene.effect.MotionBlur()
                        if (gc.getEffect(effect) == null) {
                            gc.setEffect(effect)
                        } else {
                            gc.setEffect(null)
                            gc.clearRect(0.0, 0.0, dynamicCanvas.width, dynamicCanvas.height)
                        }
                    }
                }

                else -> {}
            }
        }

        scene.setOnKeyPressed { e ->
            logAsync("Key Pressed: ${e.code}")
            when (e.code) {
                KeyCode.ESCAPE -> stage.close()
                else -> {}
            }
        }

        val sunShape = FXShape(gc, imageLib.sun, object : FXLocator {
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> =
                Pair(centerX, centerY)
        })

        val earthR = 310.0
        val earthRand = Math.random() * 7.2
        val earthShape = FXShape(gc, imageLib.earth, object : FXLocator {
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val earthT = time * 0.00073
                return Pair(
                    centerX + earthR * cos(earthT + earthRand) * 1.5,
                    centerY + earthR * sin(earthT + earthRand)
                )
            }
        })

        val moonShape = FXShape(gc, imageLib.moon, object : FXLocator {
            private val moonR = 45.0
            private val moonRand = Math.random() * 7.2
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val moonT = time * 0.00377
                return Pair(
                    earthShape.getLastX() + moonR * cos(moonT + moonRand),
                    earthShape.getLastY() + moonR * sin(moonT + moonRand) * 1.4
                )
            }
        })
        val shipShape = FXShape(gc, imageLib.ship, object : FXLocator {
            private val shipR = 145.0
            private val shipRand = Math.random() * 0.0072
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val shipT = time * -0.0011
                return Pair(
                    centerX + shipR * cos(shipT + shipRand),
                    centerY + shipR * sin(shipT + shipRand) * 1.9
                )
            }
        })
        val multiLoopShapes = listOf(sunShape, earthShape, moonShape, shipShape)
        val singleLoopShapes = mutableListOf<FXShape>()

        var moonExploding = false
        var lastLinearTime = 0L
        var nextBigTick = 0L
        val startTime = System.currentTimeMillis()
        val keyFrame = KeyFrame(Duration.millis(frameDuration.toDouble()),
            {
                val systemTime = System.currentTimeMillis()
                val elapsedTime = systemTime - startTime
                val linearTime = lastLinearTime + frameDuration

                multiLoopShapes.forEach {
                    it.clear()
                }
                singleLoopShapes.forEach {
                    it.clear()
                }

                if (singleLoopShapes.isNotEmpty()) {
                    val itr = singleLoopShapes.iterator()
                    while (itr.hasNext()) {
                        val current = itr.next()
                        if (current.running(linearTime)) {
                            current.draw(linearTime)
                        } else {
                            itr.remove()
                            moonExploding = false
                        }
                    }
                }

                multiLoopShapes.forEach {
                    it.draw(linearTime)
                }

                if (!moonExploding && shipShape.clip(moonShape)) {
                    logAsync("Collision! Moon & Ship", elapsedTime)
                    moonExploding = true
                    singleLoopShapes.add(FXShape(gc, imageLib.explosion(linearTime), object : FXLocator {
                        override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                            return Pair(
                                shape.mapX(moonShape.getLastCenterX()), shape.mapY(moonShape.getLastCenterY())
                            )
                        }
                    }))
                    musicLib.explosion.play()
                }

                // Move BG Canvas slowly for star movement effect
                val translateId = ((linearTime % 60000) / (60000 / bgTransX.size)).toInt()
                bgCanvas.translateX += bgTransX[translateId]
                bgCanvas.translateY += bgTransY[translateId]

                // ticks roughly every second
                if ( linearTime > nextBigTick ) {
                    logAsync("Loop time: ${System.currentTimeMillis() - systemTime} ms", elapsedTime)
                    nextBigTick = linearTime + 60_000

                    val rockShape = FXShape(gc, imageLib.rock1, object : FXLocator {
                        private val vx = 6 * Math.random()
                        private val vy = 6 * Math.random()
                        override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                            return Pair(shape.getLastX() + vx, shape.getLastY() + vy)
                        }

                        override fun running(time: Long, shape: FXShape): Boolean {
                            return shape.getLastX() < width && shape.getLastY() < height
                        }
                    })
                    singleLoopShapes.add(rockShape)
                }

                lastLinearTime = linearTime
            })

        gameLoop.keyFrames.add(keyFrame)
        gameLoop.play()
        //musicLib.bgMusicPlayer.play()

        stage.show()
    }

    override fun stop() {
        logAsync("Stopping...")
        runBlocking {
            for (i in 1..10) {
                if (gameScope.isActive && scopeJobs.children.count() > 0) {
                    delay(500)
                } else {
                    break
                }
            }
        }
        gameScope.cancel()
        scopeThreads.shutdown()
        println("Done")
    }

    private fun logAsync(msg: String, time: Long = (System.currentTimeMillis() - appStartTime)) {
        gameScope.launch {
            println("[${Thread.currentThread().name}][$time] $msg")
        }
    }
}

fun main() {
    Application.launch(PlanetsApp::class.java)
}