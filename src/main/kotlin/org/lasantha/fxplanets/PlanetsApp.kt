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
import kotlin.random.Random


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

        val sunShape = FXShape("Sun", gc, imageLib.sun, object : FXLocator {
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> = centerX to centerY
        })

        val earthShape = FXShape("Earth", gc, imageLib.earth, object : FXLocator {
            val r = 320.0
            val phase = fxRandom.nextDouble() * 7.2
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val t = time * 0.00073
                return (centerX + r * cos(t + phase) * 1.5) to (centerY + r * sin(t + phase))
            }
        })

        val moonShape = FXShape("Moon", gc, imageLib.moon, object : FXLocator {
            private val r = 45.0
            private val phase = fxRandom.nextDouble() * 7.2
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val t = time * 0.00377
                return (earthShape.getX() + r * cos(t + phase)) to (earthShape.getY() + r * sin(t + phase) * 1.5)
            }
        })
        val shipShape = FXShape("Space ship", gc, imageLib.ship, object : FXLocator {
            private val r = 145.0
            private val phase = fxRandom.nextDouble() * 0.0072
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val t = time * -0.0011
                return (centerX + r * cos(t + phase)) to (centerY + r * sin(t + phase) * 1.9)
            }
        })
        val longLivedShapes = listOf(sunShape, earthShape, moonShape, shipShape)
        val shortLivedShapes = mutableListOf<FXShape>()
        val collidingShapes = mutableListOf(shipShape)
        val collisionMap = mutableMapOf<FXShape, FXShape>()

        var lastLinearTime = 0L
        var nextBigTick = 0L
        var nextAsteroidTick = 0L
        val startTime = System.currentTimeMillis()

        val keyFrame = KeyFrame(Duration.millis(frameDuration.toDouble()), {
            val systemTime = System.currentTimeMillis()
            val elapsedTime = systemTime - startTime
            val linearTime = lastLinearTime + frameDuration

            longLivedShapes.forEach { it.update(linearTime) }
            shortLivedShapes.forEach { it.update(linearTime) }

            longLivedShapes.forEach { it.clear() }
            shortLivedShapes.forEach { it.clear() }

            longLivedShapes.forEach { it.draw(linearTime) }
            if (shortLivedShapes.isNotEmpty()) {
                val itr = shortLivedShapes.iterator()
                while (itr.hasNext()) {
                    val s = itr.next()
                    if (!s.drawIfRunning(linearTime)) {
                        itr.remove()
                        collidingShapes.remove(s) // remove if present
                    }
                }
            }


            val toBeRemoved = collisionMap.filter { e -> !e.value.clipBox(e.key) }.map { it.key }.toSet()
            toBeRemoved.forEach { s -> collisionMap.remove(s) }
            for (cs in collidingShapes) {
                longLivedShapes.filter { s -> cs != s && !collisionMap.contains(s) && cs.clip(s) }.forEach { s ->
                    logAsync("Collision! (${cs.name}, ${s.name})", elapsedTime)
                    collisionMap[s] = cs
                    shortLivedShapes.add(
                        FXShape(
                            "Explosion (${cs.name}, ${s.name})", gc, imageLib.explosion3(linearTime),
                            object : FXLocator {
                                override fun location(time: Long, shape: FXShape): Pair<Double, Double> =
                                    shape.mapX(s.getCenterX()) to shape.mapY(s.getCenterY())
                            })
                    )
                    musicLib.explosion.play()
                }
            }

            // Move BG Canvas slowly for star movement effect
            val translateId = ((linearTime % 60000) / (60000 / bgTransX.size)).toInt()
            bgCanvas.translateX += bgTransX[translateId]
            bgCanvas.translateY += bgTransY[translateId]

            if (linearTime > nextAsteroidTick) {
                nextAsteroidTick = linearTime + 5000 + fxRandom.nextLong(5000)

                val asteroid = FXShape("Asteroid-$linearTime", gc, imageLib.rock1, object : FXLocator {
                    private val vx = 6.0 * fxRandom.nextDouble()
                    private val vy = 6.0 * fxRandom.nextDouble()
                    override fun location(time: Long, shape: FXShape): Pair<Double, Double> =
                        shape.getX() + vx to shape.getY() + vy

                    override fun running(time: Long, shape: FXShape): Boolean =
                        shape.getX() < width && shape.getY() < height
                })
                shortLivedShapes.add(asteroid)
                collidingShapes.add(asteroid)
            }

            if (linearTime > nextBigTick) {
                logAsync("Loop time: ${System.currentTimeMillis() - systemTime} ms", elapsedTime)
                nextBigTick = linearTime + 60_000
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

lateinit var fxRandom: Random

fun main() {
    fxRandom = Random(234)
    Application.launch(PlanetsApp::class.java)
}