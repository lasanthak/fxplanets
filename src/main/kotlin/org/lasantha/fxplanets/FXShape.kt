package org.lasantha.fxplanets

import javafx.scene.canvas.GraphicsContext
import kotlin.math.pow

class FXShape(
    val name: String,
    private val gc: GraphicsContext,
    private val image: ImageWrapper,
    private val locator: FXLocator,
) {
    private var running = true

    private var x = 0.0
    private var y = 0.0

    private var lastX = Double.NaN
    private var lastY = Double.NaN

    private val halfW = image.width / 2.0
    private val halfH = image.height / 2.0

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
            target.getX() - x < image.width
        } else {
            x - target.getX() < target.image.width
        }

        if (xClips) {
            return if (y < target.getY()) {
                target.getY() - y < image.height
            } else {
                y - target.getY() < target.image.height
            }
        }

        return false
    }

    fun clipCircle(target: FXShape): Boolean =
        ((x - target.getX()).pow(2) + (y - target.getY()).pow(2)) < (halfH + target.halfH).pow(2)

    fun getX() = x
    fun getY() = y

    fun getCenterX() = x + halfW
    fun getCenterY() = y + halfH

    fun mapX(centerX: Double) = centerX - halfW
    fun mapY(centerY: Double) = centerY - halfH

    override fun toString(): String =
        "${javaClass.simpleName}{name=$name, x=${String.format("%.2f", x)}, y=${String.format("%.2f", y)}}"
}

interface FXLocator {
    fun location(time: Long, shape: FXShape): Pair<Double, Double>

    fun running(time: Long, shape: FXShape): Boolean = true
}
