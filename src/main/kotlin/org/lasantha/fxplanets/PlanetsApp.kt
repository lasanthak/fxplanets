package org.lasantha.fxplanets

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.util.Duration
import kotlin.math.cos
import kotlin.math.sin

class PlanetsApp : Application() {
    private val width = 1400.0
    private val height = 1000.0
    private val sun = img("sun.png")
    private val earth = img("earth.png")
    private val moon = img("moon.png")
    private val ships = AnimatedImage(
        duration = 0.1, frames = arrayOf(
            img("ship/s1.png"), img("ship/s2.png"), img("ship/s3.png"),
            img("ship/s4.png"), img("ship/s5.png"), img("ship/s6.png"),
        )
    )
    private var animate = true

    private val gameLoop = Timeline()

    init {
        gameLoop.cycleCount = Timeline.INDEFINITE
    }

    override fun start(stage: Stage) {
        stage.title = "Space"
        stage.icons.add(0, img("galaxy.png"))
        val root = StackPane()
        val scene = Scene(root)
        stage.setScene(scene)
        val staticCanvas = Canvas(width, height)
        val dynamicCanvas = Canvas(width, height)
        root.children.addAll(staticCanvas, dynamicCanvas)
        root.background = Background(
            BackgroundImage(
                img("space.png"), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, BackgroundSize(100.0, 100.0, true, true, true, true)
            )
        )

        val centerX = width / 2.0
        val centerY = height / 2.0
        staticCanvas.graphicsContext2D.drawImage(sun, centerX, centerY)
        val gc = dynamicCanvas.getGraphicsContext2D()
        gc.isImageSmoothing = false

        scene.setOnMousePressed {e ->
            when(e.button) {
                MouseButton.SECONDARY -> {
                    animate = if (animate) {
                        gameLoop.stop()
                        false
                    } else {
                        gameLoop.play()
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

        val startTime = System.currentTimeMillis()
        val earthRadius = 300.0
        val earthRand = Math.random() * 7.2
        val earthShape = DynamicShape(gc, centerX, centerY)
        val moonRadius = 45.0
        val moonRand = Math.random() * 7.2
        val moonShape = DynamicShape(gc, centerX, centerY)
        val shipRadius = 145.0
        val shipRand = Math.random() * 7.2
        val shipShape = DynamicShape(gc, centerX, centerY)
        var lastT = 0.0
        val keyFrame = KeyFrame(Duration.seconds(0.03),  //0.017 -> 60 FPS, 0.02 -> 50 FPS, 0.3 -> 33 FPS
            {
                val tSec = (System.currentTimeMillis() - startTime) / 1000.0
                val t = lastT + 0.03

                val earthT = t * 0.73
                earthShape.clear(earth)
                earthShape.draw(earth, centerX + earthRadius * cos(earthT + earthRand) * 1.5,
                    centerY + earthRadius * sin(earthT + earthRand) )

                val moonT = t * 3.77
                moonShape.clear(moon)
                moonShape.draw(
                    moon, earthShape.getLastX() + moonRadius * cos(moonT + moonRand),
                    earthShape.getLastY() + moonRadius * sin(moonT + moonRand) * 1.4
                )

                val shipT = t * -1.1
                val ship = ships.frame(tSec)
                shipShape.clear(ship)
                shipShape.draw(ship, centerX + shipRadius * cos(shipT + shipRand),
                    centerY + shipRadius * sin(shipT + shipRand) * 1.9)

                lastT = t
            })
        gameLoop.keyFrames.add(keyFrame)
        gameLoop.play()
        stage.show()
    }
}

private fun img(name: String): Image =
    Image(PlanetsApp::class.java.getResourceAsStream("img/$name"))

fun main() {
    Application.launch(PlanetsApp::class.java)
}