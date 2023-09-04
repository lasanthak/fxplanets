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

class PlanetsApp : Application() {
    private val scopeThreads = Executors.newFixedThreadPool(2, GameThreadFactory())
    private val scopeJobs = Job()
    private val gameScope = CoroutineScope(scopeThreads.asCoroutineDispatcher() + scopeJobs)

    private val imageLib = ImageLib()
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
        stage.icons.add(0, imageLib.icon())
        stage.isResizable = false
        val root = StackPane()
        val scene = Scene(root, AppConf.width, AppConf.height, false)
        stage.setScene(scene)
        val bgCanvas = Canvas(AppConf.width * 1.4, AppConf.height * 1.4)
        val staticCanvas = Canvas(AppConf.width, AppConf.height)
        val dynamicCanvas = Canvas(AppConf.width, AppConf.height)
        root.children.addAll(bgCanvas, staticCanvas, dynamicCanvas)

        val centerX = AppConf.width / 2.0
        val centerY = AppConf.height / 2.0
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
            if (AppConf.debug) {
                logAsync("Key Pressed: ${e.code}")
            }
            when (e.code) {
                KeyCode.ESCAPE -> stage.close()
                else -> {}
            }
        }


        val fighterShape = FXShape("Fighter", gc, imageLib.fighter, FXLocator(fLocation = { _ ->
            centerX - imageLib.fighter.width / 2.0 to AppConf.height - 100.0
        }))

        val sunShape = FXShape("Sun", gc, imageLib.sun, FXLocator(fLocation = { _ ->
            centerX - imageLib.sun.width / 2.0 to centerY - imageLib.sun.height / 2.0
        }))

        fun earthLocator(): FXLocator {
            val r = 320.0
            val phase = AppConf.rand.nextDouble() * 7.2
            return FXLocator(fLocation = { time ->
                val t = time * 0.00073
                (centerX + r * cos(t + phase) * 1.5) to (centerY + r * sin(t + phase))
            })
        }

        val earthLoc = earthLocator()
        val earthShape = FXShape("Earth", gc, imageLib.earth, earthLoc)

        fun moonLocator(): FXLocator {
            val r = 45.0
            val phase = AppConf.rand.nextDouble() * 7.2
            return FXLocator(fLocation = { time ->
                val t = time * 0.00377
                (earthLoc.getX() + r * cos(t + phase)) to (earthLoc.getY() + r * sin(t + phase) * 1.5)
            })
        }

        val moonShape = FXShape("Moon", gc, imageLib.moon, moonLocator())

        fun planetLocator(): FXLocator {
            val r = 145.0
            val phase = AppConf.rand.nextDouble() * 0.0072
            return FXLocator(fLocation = { time ->
                val t = time * -0.0011
                (centerX + r * cos(t + phase)) to (centerY + r * sin(t + phase) * 1.9)
            })
        }

        val planetShape = FXShape("Planet", gc, imageLib.planet, planetLocator())

        val longLivedShapes = LinkedHashSet<FXShape>()
        val shortLivedShapes = LinkedHashSet<FXShape>()
        val collidingShapes = LinkedHashSet<FXShape>()
        val collisionMap = mutableMapOf<FXShape, FXShape>()

        longLivedShapes.addAll(listOf(sunShape, earthShape, moonShape, planetShape))
        shortLivedShapes.addAll(listOf(fighterShape))
        collidingShapes.addAll(listOf(planetShape, fighterShape))

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
                if (AppConf.debug) {
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

    private fun asteroidLocator(startTime: Long): FXLocator {
        fun locFun(maxLen: Double, forward: Boolean, add: Boolean): (time: Long) -> Double {
            val v = 0.05 + 0.1 * AppConf.rand.nextDouble()
            val c = if (add) (maxLen * 0.8 * AppConf.rand.nextDouble()) else 0.0
            return if (forward) {
                { time -> c + v * (time - startTime) }
            } else {
                { time -> maxLen - c - (v * (time - startTime)) }
            }
        }

        val addC = AppConf.rand.nextBoolean()

        val fx = locFun(AppConf.width, AppConf.rand.nextBoolean(), addC)
        val fy = locFun(AppConf.height, AppConf.rand.nextBoolean(), !addC)

        return FXLocator(destroyIfOutOfBounds = true, fLocation = { time -> fx(time) to fy(time) })
    }

    private fun randomAsteroid(startTime: Long, gc: GraphicsContext): FXShape {
        val image = imageLib.rocks[AppConf.rand.nextInt(imageLib.rocks.size)]
        return FXShape("UFO-$startTime", gc, image, asteroidLocator(startTime))
    }

    private fun randomExplosion(s1: FXShape, s2: FXShape, startTime: Long, gc: GraphicsContext): FXShape {
        val img = if (AppConf.rand.nextBoolean()) imageLib.explosion1(startTime)
        else imageLib.explosion2(startTime)

        val gapW = s2.image.halfW() - img.halfW()
        val gapH = s2.image.halfH() - img.halfH()
        return FXShape(
            "Explosion<${s1.name}, ${s2.name}>", gc, img,
            FXLocator(fLocation = { _ -> (s2.locator.getX() + gapW) to (s2.locator.getY() + gapH) })
        )
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
