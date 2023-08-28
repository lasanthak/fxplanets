package org.lasantha.fxplanets

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

class DynamicShape(private val gc: GraphicsContext, x: Double, y: Double) {
    private var lastX = x
    private var lastY = y

    fun clear(image: Image) {
        gc.clearRect(lastX, lastY, image.width, image.height)
    }

    fun draw(image: Image, x: Double, y: Double) {
        gc.drawImage(image, x, y)
        lastX = x
        lastY = y
    }

    fun getLastX() = lastX
    fun getLastY() = lastY
}