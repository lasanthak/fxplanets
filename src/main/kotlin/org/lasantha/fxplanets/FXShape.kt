package org.lasantha.fxplanets

import javafx.scene.canvas.GraphicsContext
import kotlin.math.pow

class FXShape(
    val name: String,
    private val gc: GraphicsContext,
    private val image: ImageWrapper,
    private val locator: FXLocator,
    private val clipW: Double = image.width,
    private val clipH: Double = image.height
) {
    private var running = true

    private var x = 0.0
    private var y = 0.0

    private var lastX = Double.NaN
    private var lastY = Double.NaN

    private val clipHalfW = clipW / 2.0
    private val clipHalfH = clipH / 2.0

    fun update(time: Long) {
        if (running(time)) {
            val (newX, newY) = locator.location(time, this)
            lastX = x
            lastY = y
            x = newX
            y = newY
        }
    }

    fun clear() {
        gc.clearRect(lastX, lastY, image.width, image.height)
    }

    fun draw(time: Long) {
        gc.drawImage(image.frame(time), x, y)
    }

    fun drawIfRunning(time: Long): Boolean {
        if (running(time)) {
            gc.drawImage(image.frame(time), x, y)
            return true
        }
        return false
    }


    fun running(time: Long): Boolean = this.running && image.hasFrame(time) && locator.running(time, this)

    fun stopRunning() {
        this.running = false
    }

    fun clip(target: FXShape): Boolean = clipBox(target) && clipCircle(target)

    fun clipBox(target: FXShape): Boolean {
        val xClips = if (x < target.getX()) {
            target.getX() - x < clipW
        } else {
            x - target.getX() < target.clipW
        }

        if (xClips) {
            return if (y < target.getY()) {
                target.getY() - y < clipH
            } else {
                y - target.getY() < target.clipH
            }
        }

        return false
    }

    fun clipCircle(target: FXShape): Boolean =
        ((x - target.getX()).pow(2) + (y - target.getY()).pow(2)) < (clipHalfH + target.clipHalfH).pow(2)

    fun getX() = x
    fun getY() = y

    fun getCenterX() = x + clipHalfW
    fun getCenterY() = y + clipHalfH

    fun mapX(centerX: Double) = centerX - clipHalfW
    fun mapY(centerY: Double) = centerY - clipHalfH

    override fun toString(): String =
        "${javaClass.simpleName}{name=$name, x=${String.format("%.2f", x)}, y=${String.format("%.2f", y)}}"
}

interface FXLocator {
    fun location(time: Long, shape: FXShape): Pair<Double, Double>

    fun running(time: Long, shape: FXShape): Boolean = true
}
