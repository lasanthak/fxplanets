package org.lasantha.fxplanets

import javafx.scene.image.Image

class AnimatedImage(private val duration: Double, private val frames: Array<Image>) {
    fun frame(time: Double): Image {
        val index = (time % (frames.size * duration) / duration).toInt()
        return frames[index]
    }
}