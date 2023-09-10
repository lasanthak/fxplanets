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
import org.lasantha.fxplanets.controller.GameController
import org.lasantha.fxplanets.model.Game
import org.lasantha.fxplanets.service.EntityService
import org.lasantha.fxplanets.service.PresentationLib
import org.lasantha.fxplanets.view.ImageLib
import org.lasantha.fxplanets.view.MusicLib
import java.util.concurrent.Executors

class PlanetsApp : Application() {
    private val scopeThreads = Executors.newFixedThreadPool(2, GameThreadFactory())
    private val scopeJobs = Job()
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)
    private val prLib = PresentationLib()
    private val musicLib = MusicLib()
    private val imageLib = ImageLib(prLib)
    private val context = Context(prLib, musicLib, imageLib)
    private val frameDuration = 1000L / context.game.fps
    private val gameLoop = Timeline()

    private var animate = true
    private var loopCount = 0L
    private var totalTimeNano = 0L
    private var linearT = 0L

    private val appStartTime = System.currentTimeMillis()

    init {
        gameLoop.cycleCount = Timeline.INDEFINITE
    }

    override fun start(stage: Stage) {
        stage.title = "Planets & Asteroids"
        stage.isResizable = false
        val root = StackPane()
        val scene = Scene(root, context.game.width, context.game.height, false)
        stage.setScene(scene)
        val bgCanvas = Canvas(context.game.width * 1.4, context.game.height * 1.4)
        val staticCanvas = Canvas(context.game.width, context.game.height)
        val dynamicCanvas = Canvas(context.game.width, context.game.height)
        root.children.addAll(bgCanvas, staticCanvas, dynamicCanvas)

        val gcBg = bgCanvas.graphicsContext2D
        gcBg.isImageSmoothing = false
        val gcStatic = staticCanvas.graphicsContext2D
        gcStatic.isImageSmoothing = false
        val gc = dynamicCanvas.graphicsContext2D
        gc.isImageSmoothing = false

        context.controller.initGC(gc)

        stage.icons.add(0, imageLib.icon())
        root.style = "-fx-background-color:black"
        gcBg.drawImage(imageLib.bgImage(), 0.0, 0.0)
        val bgTransX = arrayOf(-0.12, -0.06, 0.06, 0.12, 0.06, -0.06, -0.12, -0.06, 0.06, 0.12, 0.06, -0.06)
        val bgTransY = arrayOf(-0.12, -0.06, -0.12, -0.06, 0.06, 0.12, 0.06, -0.06, 0.06, 0.12, 0.06, -0.06)
        for (i in bgTransX.indices) {
            bgTransX[i] = frameDuration.toDouble() * bgTransX[i] / 30.0
            bgTransY[i] = frameDuration.toDouble() * bgTransY[i] / 30.0
        }

        scene.setOnMousePressed { e ->
            when (e.button) {
                MouseButton.SECONDARY -> {
                    if (animate) {
                        gameLoop.stop()
                        context.controller.handlePause(linearT)
                    } else {
                        gameLoop.play()
                        context.controller.handleResume(linearT)
                    }
                    animate = !animate
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
            when (e.code) {
                KeyCode.ESCAPE -> stage.close()
                else -> {}
            }
            context.controller.handleKeyPress(linearT, e.code)
        }

        var nextBigTick = 3000L
        val gameStartT = System.currentTimeMillis()
        val keyFrame = KeyFrame(Duration.millis(frameDuration.toDouble()), {
            val loopStartTNano = System.nanoTime()
            val elapsedT = System.currentTimeMillis() - gameStartT
            linearT += frameDuration

            context.controller.update(linearT)
            context.controller.clear()
            context.controller.makeNewEntities(linearT)
            context.controller.draw(linearT)

            // Move BG Canvas slowly for star movement effect
            val translateId = ((linearT % 120_000) / (120_000 / bgTransX.size)).toInt()
            bgCanvas.translateX += bgTransX[translateId]
            bgCanvas.translateY += bgTransY[translateId]

            loopCount++
            totalTimeNano += (System.nanoTime() - loopStartTNano)
            if (linearT > nextBigTick) {
                nextBigTick = linearT + 60_000
                val totalTMs = totalTimeNano / 1_000_000.0
                val averageTime = String.format("%.3f", totalTMs / loopCount.toDouble())
                logAsync(
                    "Metrics: totalTime=${totalTMs}ms, loopCount=$loopCount, averageTime=${averageTime}ms", elapsedT
                )
                if (context.game.debug) {
                    val totalMemory = Runtime.getRuntime().totalMemory() / 1_000_000L
                    logAsync("totalMemory=${totalMemory}MB", elapsedT)
                }
            }
        })

        gameLoop.keyFrames.add(keyFrame)
        gameLoop.play()
        musicLib.playMusic(context.game)
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

    private fun logAsync(msg: String, time: Long? = null) {
        gameScope.launch {
            val sysT = System.currentTimeMillis() - appStartTime
            val tStr = time?.toString() ?: ""
            val thread = Thread.currentThread().name
            println("[$thread][$sysT][$tStr] $msg")
        }
    }
}

class Context(val prLib: PresentationLib, val musicLib: MusicLib, val imageLib: ImageLib) {
    val game = Game(width = 1600.0, height = 1200.0)
    val entityService = EntityService(game, prLib)
    val controller = GameController(imageLib, musicLib, entityService)
}

fun main() {
    Application.launch(PlanetsApp::class.java)
}
