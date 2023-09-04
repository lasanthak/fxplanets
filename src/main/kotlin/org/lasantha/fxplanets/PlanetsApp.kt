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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class PlanetsApp : Application() {
    private val scopeThreads = Executors.newFixedThreadPool(2, GameThreadFactory())
    private val scopeJobs = Job()
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)

    private val shapeLib = ShapeLib()
    private val musicLib = MusicLib()
    private val frameDuration = 1000L / AppConf.fps
    private val gameLoop = Timeline()
    private val appStartTime = System.currentTimeMillis()

    private var animate = true
    private var loopCount = 0L
    private var totalTimeNano = 0L

    init {
        gameLoop.cycleCount = Timeline.INDEFINITE
    }

    override fun start(stage: Stage) {
        stage.title = "Planets & Asteroids"
        stage.icons.add(0, shapeLib.imageLib.icon())
        stage.isResizable = false
        val root = StackPane()
        val scene = Scene(root, AppConf.width, AppConf.height, false)
        stage.setScene(scene)
        val bgCanvas = Canvas(AppConf.width * 1.4, AppConf.height * 1.4)
        val staticCanvas = Canvas(AppConf.width, AppConf.height)
        val dynamicCanvas = Canvas(AppConf.width, AppConf.height)
        root.children.addAll(bgCanvas, staticCanvas, dynamicCanvas)

        val gcBg = bgCanvas.graphicsContext2D
        gcBg.isImageSmoothing = false
        val gcStatic = staticCanvas.graphicsContext2D
        gcStatic.isImageSmoothing = false
        val gc = dynamicCanvas.graphicsContext2D
        gc.isImageSmoothing = false

        root.style = "-fx-background-color:black"
        gcBg.drawImage(shapeLib.imageLib.bgImage(), 0.0, 0.0)
        val bgTransX = arrayOf(-0.12, -0.06, 0.06, 0.12, 0.06, -0.06, -0.12, -0.06, 0.06, 0.12, 0.06, -0.06)
        val bgTransY = arrayOf(-0.12, -0.06, -0.12, -0.06, 0.06, 0.12, 0.06, -0.06, 0.06, 0.12, 0.06, -0.06)
        for (i in bgTransX.indices) {
            bgTransX[i] = frameDuration.toDouble() * bgTransX[i] / 30.0
            bgTransY[i] = frameDuration.toDouble() * bgTransY[i] / 30.0
        }

        scene.setOnMousePressed { e ->
            when (e.button) {
                MouseButton.SECONDARY -> {
                    animate = if (animate) {
                        gameLoop.stop()
                        musicLib.pauseMusic()
                        false
                    } else {
                        gameLoop.play()
                        musicLib.playMusic()
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
            if (AppConf.debug) {
                logAsync("Key Pressed: ${e.code}")
            }
            when (e.code) {
                KeyCode.ESCAPE -> stage.close()
                else -> {}
            }
        }

        val longLivedShapes = LinkedHashSet<FXShape>()
        val shortLivedShapes = LinkedHashSet<FXShape>()
        val collidingShapes = LinkedHashSet<FXShape>()
        val collisionMap = mutableMapOf<FXShape, FXShape>()

        longLivedShapes.addAll(listOf(shapeLib.sunShape, shapeLib.earthShape, shapeLib.moonShape, shapeLib.planetShape))
        shortLivedShapes.addAll(listOf(shapeLib.fighterShape))
        collidingShapes.addAll(listOf(shapeLib.planetShape, shapeLib.fighterShape))

        var lastLinearT = 0L
        var nextAsteroidTick = 1500L
        var nextBigTick = 3000L
        val gameStartT = System.currentTimeMillis()
        val keyFrame = KeyFrame(Duration.millis(frameDuration.toDouble()), {
            val loopStartTNano = System.nanoTime()
            val elapsedT = System.currentTimeMillis() - gameStartT
            val linearT = lastLinearT + frameDuration

            longLivedShapes.forEach { it.update(linearT) }
            shortLivedShapes.forEach { it.update(linearT) }

            longLivedShapes.forEach { it.clear(gc) }
            shortLivedShapes.forEach { it.clear(gc) }

            longLivedShapes.forEach { it.draw(gc, linearT) }
            shortLivedShapes.filterNot { it.drawIfRunning(gc, linearT) }.forEach {
                shortLivedShapes.remove(it)
                collidingShapes.remove(it) // remove if present
                collisionMap.remove(it) // remove if present
                if (AppConf.debug) {
                    logAsync("Removed: $it", elapsedT)
                }
            }

            collisionMap.filter { (a, b) -> !a.clipBox(b) }.forEach { (a, _) -> collisionMap.remove(a) }
            for (a in collidingShapes) {
                longLivedShapes.firstOrNull { b -> a != b && !collisionMap.contains(a) && a.clip(b) }?.let { b ->
                    collisionMap[a] = b
                    shortLivedShapes.add(shapeLib.randomExplosion(a, b, linearT))
                    if (!longLivedShapes.contains(a)) {
                        a.stopRunning()
                    }
                    if (AppConf.debug) {
                        logAsync("Collision: ${a.name} -> ${b.name}", elapsedT)
                    }
                    if (AppConf.mainAudioEnabled && AppConf.soundEnabled) {
                        musicLib.explosion.play()
                    }
                }
            }

            if (linearT > nextAsteroidTick) {
                nextAsteroidTick = linearT + 5000 + AppConf.rand.nextLong(5000)
                val a = shapeLib.randomAsteroid(linearT)
                shortLivedShapes.add(a)
                collidingShapes.add(a)
            }

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
                if (AppConf.debug) {
                    logAsync("longLivedShapes=$longLivedShapes", elapsedT)
                    logAsync("shortLivedShapes=$shortLivedShapes", elapsedT)
                    logAsync("collidingShapes=$collidingShapes", elapsedT)
                    logAsync("collisionMap=$collisionMap", elapsedT)
                    val totalMemory = Runtime.getRuntime().totalMemory() / 1_000_000L
                    logAsync("totalMemory=${totalMemory}MB", elapsedT)
                }
            }

            lastLinearT = linearT
        })

        gameLoop.keyFrames.add(keyFrame)
        gameLoop.play()
        musicLib.playMusic()

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


fun main() {
    Application.launch(PlanetsApp::class.java)
}
