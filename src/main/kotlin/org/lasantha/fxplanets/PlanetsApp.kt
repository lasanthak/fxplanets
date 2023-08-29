package org.lasantha.fxplanets

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.util.Duration
import kotlin.math.cos
import kotlin.math.sin


class PlanetsApp : Application() {
    private val width = 1400.0
    private val height = 1000.0
    private val imageLib = ImageLib()
    private val musicLib = MusicLib()

    private var animate = true

    private val gameLoop = Timeline()

    init {
        gameLoop.cycleCount = Timeline.INDEFINITE
    }

    override fun start(stage: Stage) {
        stage.title = "Space"
        stage.icons.add(0, imageLib.icon())
        val root = StackPane()
        val scene = Scene(root)
        stage.setScene(scene)
        val staticCanvas = Canvas(width, height)
        val dynamicCanvas = Canvas(width, height)
        root.children.addAll(staticCanvas, dynamicCanvas)
        root.background = Background(
            BackgroundImage(
                imageLib.bgImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, BackgroundSize(100.0, 100.0, true, true, true, true)
            )
        )

        val centerX = width / 2.0
        val centerY = height / 2.0
        staticCanvas.graphicsContext2D.drawImage(imageLib.sun(), centerX, centerY)
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

        val earthRadius = 330.0
        val earthRand = Math.random() * 7.2
        val earthShape = ImageWriter(gc, imageLib.earth, centerX, centerY)
        val moonRadius = 45.0
        val moonRand = Math.random() * 7.2
        val moonShape = ImageWriter(gc, imageLib.moon, centerX, centerY)
        val shipRadius = 145.0
        val shipRand = Math.random() * 7.2
        val shipShape = ImageWriter(gc, imageLib.ships, centerX, centerY)
        val explosions = mutableListOf<ImageWriter>()
        var moonExploding = false
        var lastT = 0.0
        val startTime = System.currentTimeMillis()
        val keyFrame = KeyFrame(Duration.seconds(0.03),  //0.017 -> 60 FPS, 0.02 -> 50 FPS, 0.3 -> 33 FPS
            {
                val elapsedMS = System.currentTimeMillis() - startTime
                val t = lastT + 0.03

                val earthT = t * 0.73
                earthShape.clear()
                earthShape.draw(
                    elapsedMS,
                    centerX + earthRadius * cos(earthT + earthRand) * 1.5,
                    centerY + earthRadius * sin(earthT + earthRand)
                )

                val moonT = t * 3.77
                moonShape.clear()
                moonShape.draw(
                    elapsedMS,
                    earthShape.getLastX() + moonRadius * cos(moonT + moonRand),
                    earthShape.getLastY() + moonRadius * sin(moonT + moonRand) * 1.4
                )

                val shipT = t * -1.1
                shipShape.clear()
                shipShape.draw(
                    elapsedMS,
                    centerX + shipRadius * cos(shipT + shipRand),
                    centerY + shipRadius * sin(shipT + shipRand) * 1.9
                )

                if (!moonExploding && shipShape.clip(moonShape)) {
                    moonExploding = true
                    explosions.add(ImageWriter(gc, imageLib.explosion(), moonShape.getLastX(), moonShape.getLastY()))
                    musicLib.explosion.play()
                }

                if (explosions.isNotEmpty()) {
                    val itr = explosions.iterator()
                    while (itr.hasNext()) {
                        val exp = itr.next()
                        if (exp.running(elapsedMS)) {
                            exp.clear()
                            exp.draw(
                                elapsedMS, exp.mapX(moonShape.getLastCenterX()), exp.mapY(moonShape.getLastCenterY())
                            )
                        } else {
                            exp.clear()
                            itr.remove()
                            moonExploding = false
                        }
                    }
                }

                lastT = t
            })
        gameLoop.keyFrames.add(keyFrame)
        gameLoop.play()
        stage.show()
    }
}

fun main() {
    Application.launch(PlanetsApp::class.java)
}