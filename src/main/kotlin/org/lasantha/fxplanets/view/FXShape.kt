package org.lasantha.fxplanets.view

import javafx.scene.canvas.GraphicsContext
import org.lasantha.fxplanets.AppConf
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class FXShape(
    val name: String,
    val image: ImageWrapper,
    val locator: FXLocator,
) {
    private var running = true


    fun update(time: Long) {
        if (running(time)) {
            locator.location(time)
        }
    }

    fun clear(gc: GraphicsContext) {
        gc.clearRect(locator.getLastX(), locator.getLastY(), image.width, image.height)
    }

    fun draw(gc: GraphicsContext, time: Long) {
        gc.drawImage(image.frame(time), locator.getX(), locator.getY())
    }

    fun drawIfRunning(gc: GraphicsContext, time: Long): Boolean {
        val running = running(time)
        if (running) {
            gc.drawImage(image.frame(time), locator.getX(), locator.getY())
        }
        return running
    }


    fun running(time: Long): Boolean = this.running && image.hasFrame(time) && locator.running(time)

    fun stopRunning() {
        this.running = false
    }

    fun clip(target: FXShape): Boolean = clipBox(target) && clipCircle(target)

    fun clipBox(target: FXShape): Boolean {
        val x = locator.getX()
        val tx = target.locator.getX()
        val xClips = if (x < tx) {
            tx - x < image.width
        } else {
            x - tx < target.image.width
        }

        if (xClips) {
            val y = locator.getY()
            val ty = target.locator.getY()
            return if (y < ty) {
                ty - y < image.height
            } else {
                y - ty < target.image.height
            }
        }

        return false
    }

    private fun clipCircle(target: FXShape): Boolean {
        val x = locator.getX()
        val y = locator.getY()
        val tx = target.locator.getX()
        val ty = target.locator.getY()
        val length = max(image.width, image.height)
        val targetLength = max(target.image.width, target.image.height)
        return ((x - tx).pow(2) + (y - ty).pow(2)) < ((length + targetLength) / 2.0).pow(2)
    }

    override fun toString(): String {
        val x = locator.getX().roundToInt()
        val y = locator.getY().roundToInt()
        return "${javaClass.simpleName}{name=$name, x=$x, y=$y}"
    }
}

class FXLocator(
    private val destroyIfOutOfBounds: Boolean = false,
    private val fLocation: (time: Long) -> Pair<Double, Double>,
) {
    private var x = 0.0
    private var y = 0.0

    private var lastX = Double.NaN
    private var lastY = Double.NaN

    fun location(time: Long): Pair<Double, Double> {
        val newPoint = fLocation(time)
        lastX = x
        lastY = y
        x = newPoint.first
        y = newPoint.second

        return newPoint
    }

    fun running(time: Long): Boolean {
        if (destroyIfOutOfBounds && (x < 0 || y < 0 || x > AppConf.width || y > AppConf.height)) {
            return false
        }
        return true
    }

    fun getX() = x
    fun getY() = y

    fun getLastX() = lastX
    fun getLastY() = lastY
}
