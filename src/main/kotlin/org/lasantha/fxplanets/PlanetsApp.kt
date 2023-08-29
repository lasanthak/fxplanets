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
    private val scopeThreads = Executors.newFixedThreadPool(4)
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)

    private val width = 1400.0
    private val height = 1000.0
    private val imageLib = ImageLib()
    private val musicLib = MusicLib()
    private val frameDurationS = 0.03 //0.017 -> 60 FPS, 0.02 -> 50 FPS, 0.3 -> 33 FPS
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
        root.style = "-fx-background-color:black"
        bgCanvas.graphicsContext2D.drawImage(imageLib.bgImage(), 0.0, 0.0)
        val bgTransX = arrayOf(-0.15, -0.07, 0.07, 0.15, 0.07, -0.07, -0.15, -0.07, 0.07, 0.15, 0.07, -0.07)
        val bgTransY = arrayOf(-0.15, -0.07, -0.15, -0.07, 0.07, 0.15, 0.07, -0.07, 0.07, 0.15, 0.07, -0.07)

        val centerX = width / 2.0
        val centerY = height / 2.0
        staticCanvas.graphicsContext2D.drawImage(imageLib.sun.frame(0), centerX, centerY)
        val gc = dynamicCanvas.graphicsContext2D
        gc.isImageSmoothing = false

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

        val earthR = 310.0
        val earthRand = Math.random() * 7.2
        val earthShape = FXShape(gc, imageLib.earth, centerX, centerY)
        val moonR = 45.0
        val moonRand = Math.random() * 7.2
        val moonShape = FXShape(gc, imageLib.moon, centerX, centerY)
        val shipR = 145.0
        val shipRand = Math.random() * 7.2
        val shipShape = FXShape(gc, imageLib.ships, centerX, centerY)
        val explosions = mutableListOf<FXShape>()
        var moonExploding = false
        var lastT = 0.0
        val startTime = System.currentTimeMillis()
        val keyFrame = KeyFrame(Duration.seconds(frameDurationS),
            {
                val systemTime = System.currentTimeMillis()
                val elapsedMS = systemTime - startTime
                val t = lastT + frameDurationS

                earthShape.clear()
                moonShape.clear()
                shipShape.clear()
                if (explosions.isNotEmpty()) {
                    for (exp in explosions) {
                        exp.clear()
                    }
                }

                if (explosions.isNotEmpty()) {
                    val itr = explosions.iterator()
                    while (itr.hasNext()) {
                        val exp = itr.next()
                        if (exp.running(elapsedMS)) {
                            exp.draw(
                                elapsedMS, exp.mapX(moonShape.getLastCenterX()), exp.mapY(moonShape.getLastCenterY())
                            )
                        } else {
                            itr.remove()
                            moonExploding = false
                        }
                    }
                }

                val earthT = t * 0.73
                earthShape.draw(
                    elapsedMS,
                    centerX + earthR * cos(earthT + earthRand) * 1.5,
                    centerY + earthR * sin(earthT + earthRand)
                )

                val moonT = t * 3.77
                moonShape.draw(
                    elapsedMS,
                    earthShape.getLastX() + moonR * cos(moonT + moonRand),
                    earthShape.getLastY() + moonR * sin(moonT + moonRand) * 1.4
                )

                val shipT = t * -1.1
                shipShape.draw(
                    elapsedMS,
                    centerX + shipR * cos(shipT + shipRand),
                    centerY + shipR * sin(shipT + shipRand) * 1.9
                )

                if (!moonExploding && shipShape.clip(moonShape)) {
                    logAsync("Collision! Moon & Ship", elapsedMS)
                    moonExploding = true
                    explosions.add(
                        FXShape(
                            gc,
                            imageLib.explosion(elapsedMS),
                            moonShape.getLastX(),
                            moonShape.getLastY()
                        )
                    )
                    musicLib.explosion.play()
                }

                // Move BG Canvas slowly for star movement effect
                val translateId = ((elapsedMS % 60000) / (60000 / bgTransX.size)).toInt()
                bgCanvas.translateX += bgTransX[translateId]
                bgCanvas.translateY += bgTransY[translateId]

                // ticks roughly every second
//                if (t.toInt() > lastT.toInt()) {
//                    logAsync("loop time: ${System.currentTimeMillis() - systemTime}", elapsedMS)
//                }

                lastT = t
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
            println("[$time] $msg")
        }
    }
}

fun main() {
    Application.launch(PlanetsApp::class.java)
}