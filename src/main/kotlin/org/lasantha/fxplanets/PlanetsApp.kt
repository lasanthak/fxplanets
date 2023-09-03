package org.lasantha.fxplanets

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object AppConfig {
    //60 fps -> frame duration 17 ms
    //50 fps -> frame duration 20 ms
    //40 fps -> frame duration 25 ms
    //33 fps -> frame duration 30 ms
    //25 fps -> frame duration 40 ms
    var fps = 60

    var mainAudioEnabled = false
    var soundEnabled = true
    var musicEnabled = true

    var width = 1400.0
    var height = 1000.0

    var debug = true
}

class PlanetsApp : Application() {
    private val scopeThreads = Executors.newFixedThreadPool(2, GameThreadFactory())
    private val scopeJobs = Job()
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)

    private val imageLib = ImageLib()
    private val musicLib = MusicLib()
    private val frameDuration = 1000L / AppConfig.fps
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
        stage.icons.add(0, imageLib.icon())
        stage.isResizable = false
        val root = StackPane()
        val scene = Scene(root, AppConfig.width, AppConfig.height, false)
        stage.setScene(scene)
        val bgCanvas = Canvas(AppConfig.width * 1.4, AppConfig.height * 1.4)
        val staticCanvas = Canvas(AppConfig.width, AppConfig.height)
        val dynamicCanvas = Canvas(AppConfig.width, AppConfig.height)
        root.children.addAll(bgCanvas, staticCanvas, dynamicCanvas)

        val centerX = AppConfig.width / 2.0
        val centerY = AppConfig.height / 2.0
        val gcBg = bgCanvas.graphicsContext2D
        gcBg.isImageSmoothing = false
        val gcStatic = staticCanvas.graphicsContext2D
        gcStatic.isImageSmoothing = false
        val gc = dynamicCanvas.graphicsContext2D
        gc.isImageSmoothing = false

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
            if (AppConfig.debug) {
                logAsync("Key Pressed: ${e.code}")
            }
            when (e.code) {
                KeyCode.ESCAPE -> stage.close()
                else -> {}
            }
        }

        val sunShape = FXShape("Sun", gc, imageLib.sun, object : FXLocator {
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> = centerX to centerY
        }, clipW = 40.0, clipH = 40.0)

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

        val planetShape = FXShape("Planet", gc, imageLib.planet, object : FXLocator {
            private val r = 145.0
            private val phase = fxRandom.nextDouble() * 0.0072
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> {
                val t = time * -0.0011
                return (centerX + r * cos(t + phase)) to (centerY + r * sin(t + phase) * 1.9)
            }
        })
        val longLivedShapes = LinkedHashSet<FXShape>()
        val shortLivedShapes = LinkedHashSet<FXShape>()
        val collidingShapes = LinkedHashSet<FXShape>()
        val collisionMap = mutableMapOf<FXShape, FXShape>()

        longLivedShapes.addAll(listOf(sunShape, moonShape, earthShape, planetShape))
        collidingShapes.addAll(listOf(planetShape))

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

            longLivedShapes.forEach { it.clear() }
            shortLivedShapes.forEach { it.clear() }

            longLivedShapes.forEach { it.draw(linearT) }
            shortLivedShapes.filterNot { it.drawIfRunning(linearT) }.forEach {
                shortLivedShapes.remove(it)
                collidingShapes.remove(it) // remove if present
                collisionMap.remove(it) // remove if present
                if (AppConfig.debug) {
                    logAsync("Removed: $it", elapsedT)
                }
            }

            collisionMap.filter { (a, b) -> !a.clipBox(b) }.forEach { (a, _) -> collisionMap.remove(a) }
            for (a in collidingShapes) {
                longLivedShapes.firstOrNull { b -> a != b && !collisionMap.contains(a) && a.clip(b) }?.let { b ->
                    collisionMap[a] = b
                    shortLivedShapes.add(randomExplosion(a, b, linearT, gc))
                    if (!longLivedShapes.contains(a)) {
                        a.stopRunning()
                    }
                    if (AppConfig.debug) {
                        logAsync("Collision: ${a.name} -> ${b.name}", elapsedT)
                    }
                    if (AppConfig.mainAudioEnabled && AppConfig.soundEnabled) {
                        musicLib.explosion.play()
                    }
                }
            }

            if (linearT > nextAsteroidTick) {
                nextAsteroidTick = linearT + 5000 + fxRandom.nextLong(5000)
                val a = randomAsteroid(linearT, gc)
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
                if (AppConfig.debug) {
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

    private fun asteroidLocator(startTime: Long): FXLocator {
        fun locFun(maxLen: Double, forward: Boolean, add: Boolean): (time: Long, shape: FXShape) -> Double {
            val v = 0.05 + 0.1 * fxRandom.nextDouble()
            val c = if (add) (maxLen * 0.8 * fxRandom.nextDouble()) else 0.0
            return if (forward) {
                { time, _ -> c + v * (time - startTime) }
            } else {
                { time, _ -> maxLen - c - (v * (time - startTime)) }
            }
        }

        val addC = fxRandom.nextBoolean()
        return object : FXLocator {
            private val fx = locFun(AppConfig.width, fxRandom.nextBoolean(), addC)
            private val fy = locFun(AppConfig.height, fxRandom.nextBoolean(), !addC)

            override fun location(time: Long, shape: FXShape): Pair<Double, Double> = fx(time, shape) to fy(time, shape)

            override fun running(time: Long, shape: FXShape): Boolean =
                !(shape.getX() < 0 || shape.getY() < 0 || shape.getX() > AppConfig.width || shape.getY() > AppConfig.height)
        }
    }

    private fun randomAsteroid(startTime: Long, gc: GraphicsContext): FXShape {
        val image = imageLib.rocks[fxRandom.nextInt(imageLib.rocks.size)]
        return FXShape("UFO-$startTime", gc, image, asteroidLocator(startTime))
    }

    private fun randomExplosion(s1: FXShape, s2: FXShape, startTime: Long, gc: GraphicsContext): FXShape {
        val image = if (fxRandom.nextBoolean()) imageLib.explosion1(startTime) else imageLib.explosion2(startTime)
        return FXShape("Explosion<${s1.name}, ${s2.name}>", gc, image, object : FXLocator {
            override fun location(time: Long, shape: FXShape): Pair<Double, Double> =
                shape.mapX(s2.getCenterX()) to shape.mapY(s2.getCenterY())
        })
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

lateinit var fxRandom: Random

fun main() {
    fxRandom = Random(666) // 62439, 234
    Application.launch(PlanetsApp::class.java)
}
