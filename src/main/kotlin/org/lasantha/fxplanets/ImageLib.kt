package org.lasantha.fxplanets

import javafx.scene.image.Image
import javafx.scene.image.WritableImage

class ImageLib {
    val sun = StaticImage(image("sun.png"))
    val earth = StaticImage(image("earth.png"))
    val moon = StaticImage(image("moon.png"))
    val ships = MultiLoopImage(
        durationMS = 100, frames = arrayOf(
            image("ship/s1.png"), image("ship/s2.png"),
            image("ship/s3.png"), image("ship/s4.png"),
            image("ship/s5.png"), image("ship/s6.png"),
        )
    )

    fun explosion(startTime: Long): SingleLoopImage {
        val reader = image("explosion.png").pixelReader
        return SingleLoopImage(
            startTime = startTime, durationMS = 100,
            frames = arrayOf(
                WritableImage(reader, 0, 0, 128, 128), WritableImage(reader, 128, 0, 128, 128),
                WritableImage(reader, 256, 0, 128, 128), WritableImage(reader, 384, 0, 128, 128),
                WritableImage(reader, 0, 128, 128, 128), WritableImage(reader, 128, 128, 128, 128),
                WritableImage(reader, 256, 128, 128, 128), WritableImage(reader, 384, 128, 128, 128),
                WritableImage(reader, 0, 256, 128, 128), WritableImage(reader, 128, 256, 128, 128),
                WritableImage(reader, 256, 256, 128, 128), WritableImage(reader, 384, 256, 128, 128),
                WritableImage(reader, 0, 384, 128, 128), WritableImage(reader, 128, 384, 128, 128)
            )
        )
    }

    fun bgImage(): Image = image("space.png")
    fun icon(): Image = image("galaxy.png")

    private fun image(name: String): Image =
        Image(ImageLib::class.java.getResourceAsStream("img/$name"), 0.0, 0.0, true, true)
}

interface ImageWrapper {
    val width: Double
    val height: Double
    fun hasFrame(time: Long): Boolean = true
    fun frame(time: Long): Image
}

class StaticImage(private val image: Image) : ImageWrapper {
    override val width = image.width
    override val height = image.height
    override fun frame(time: Long): Image = image
}

class MultiLoopImage(private val durationMS: Int, private val frames: Array<Image>) : ImageWrapper {
    init {
        assert(durationMS > 0)
        assert(frames.isNotEmpty())
        val w = frames[0].width.toInt()
        val h = frames[0].height.toInt()
        for (frame in frames) {
            assert(w == frame.width.toInt())
            assert(h == frame.height.toInt())
        }
    }

    override val width = frames[0].width
    override val height = frames[0].height

    private val totalDuration = frames.size * durationMS
    override fun frame(time: Long): Image = frames[((time % totalDuration) / durationMS).toInt()]
}

class SingleLoopImage(private val startTime: Long,
                      private val durationMS: Int,
                      private val frames: Array<Image>) : ImageWrapper {
    init {
        assert(durationMS > 0)
        assert(frames.isNotEmpty())
        val w = frames[0].width.toInt()
        val h = frames[0].height.toInt()
        for (frame in frames) {
            assert(w == frame.width.toInt())
            assert(h == frame.height.toInt())
        }
    }

    override val width = frames[0].width
    override val height = frames[0].height

    private val endTime = startTime + frames.size * durationMS

    override fun hasFrame(time: Long): Boolean {
        return time < endTime
    }

    override fun frame(time: Long): Image {
        val id = ((time - startTime).coerceAtLeast(0) / durationMS).toInt()
        return frames[id]
    }
}