package org.lasantha.fxplanets

import javafx.scene.image.Image
import javafx.scene.image.WritableImage

class ImageLib {
    val sun = StaticImage(image("sun.png"))
    val moon = StaticImage(image("moon.png"))
    val ship = MultiLoopImage(
        durationMS = 100, frames = arrayOf(
            image("ship/s1.png"), image("ship/s2.png"),
            image("ship/s3.png"), image("ship/s4.png"),
            image("ship/s5.png"), image("ship/s6.png"),
        )
    )

    val earth = run {
        val reader = image("earth.png").pixelReader
        MultiLoopImage(
            durationMS = 50,
            frames = Array(256) { i -> WritableImage(reader, (i % 16) * 40, (i / 16) * 40, 40, 40) }
        )
    }

    private val explosion1Frames: Array<Image> =
        Array(48) { i -> WritableImage(image("explosion1.png").pixelReader, i * 256, 0, 256, 256) }

    fun explosion1(startTime: Long): SingleLoopImage =
        SingleLoopImage(startTime = startTime, durationMS = 40, frames = explosion1Frames)

    private val explosion2Frames: Array<Image> =
        Array(64) { i -> WritableImage(image("explosion2.png").pixelReader, i * 192, 0, 192, 192) }

    fun explosion2(startTime: Long): SingleLoopImage =
        SingleLoopImage(startTime = startTime, durationMS = 40, frames = explosion2Frames)

    val rock1 = run {
        val reader = image("rock1.png").pixelReader
        MultiLoopImage(
            durationMS = 100,
            frames = Array(16) { i -> WritableImage(reader, i * 64, 0, 64, 64) }
        )
    }

    val rock2 = run {
        val reader = image("rock2.png").pixelReader
        MultiLoopImage(
            durationMS = 100,
            frames = Array(16) { i -> WritableImage(reader, i * 32, 0, 32, 32) }
        )
    }

    fun bgImage(): Image = image("space.png")
    fun icon(): Image = image("galaxy.png")

    private fun image(name: String): Image =
        Image(ImageLib::class.java.getResourceAsStream("img/$name"), 0.0, 0.0, true, true)
}

sealed interface ImageWrapper {
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

class MultiLoopImage(private val durationMS: Long, private val frames: Array<Image>) : ImageWrapper {
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

class SingleLoopImage(
    private val startTime: Long,
    private val durationMS: Long,
    private val frames: Array<Image>
) : ImageWrapper {
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