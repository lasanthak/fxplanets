package org.lasantha.fxplanets

import javafx.scene.canvas.GraphicsContext
import kotlin.math.absoluteValue

class FXShape(
    private val gc: GraphicsContext, private val image: ImageWrapper,
    initX: Double, initY: Double
) {
    private var lastX = initX
    private var lastY = initY

    private val halfWidth = image.width / 2.0
    private val halfHeight = image.height / 2.0

    fun clear() {
        gc.clearRect(lastX, lastY, image.width, image.height)
    }

    fun draw(time: Long, x: Double, y: Double) {
        if (image.hasFrame(time)) {
            gc.drawImage(image.frame(time), x, y)
            lastX = x
            lastY = y
        }
    }

    fun running(time: Long): Boolean = image.hasFrame(time)

    fun clip(target: FXShape): Boolean = (
            lastX.minus(target.getLastX()).absoluteValue <= image.width &&
                    lastY.minus(target.getLastY()).absoluteValue <= image.height
            )

    fun getLastX() = lastX
    fun getLastY() = lastY

    fun getLastCenterX() = lastX + halfWidth
    fun getLastCenterY() = lastY + halfHeight

    fun mapX(centerX: Double) = centerX - halfWidth
    fun mapY(centerY: Double) = centerY - halfHeight
}