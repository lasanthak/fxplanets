package org.lasantha.fxplanets

import javafx.scene.image.Image
import javafx.scene.image.WritableImage

class ImageLib {
    val sun = StaticImage(image("sun.png"))
    val moon = StaticImage(image("moon.png"))
    val ship = MultiLoopImage(
        duration = 100, frames = arrayOf(
            image("ship/s1.png"), image("ship/s2.png"), image("ship/s3.png"),
            image("ship/s4.png"), image("ship/s5.png"), image("ship/s6.png"),
        )
    )

    val earth = run {
        val r = image("earth.png").pixelReader
        MultiLoopImage(duration = 50,
            frames = Array(256) { i -> WritableImage(r, (i % 16) * 40, (i / 16) * 40, 40, 40) })
    }

    private val exp1Frames: Array<Image> =
        Array(48) { i -> WritableImage(image("explosion1.png").pixelReader, i * 256, 0, 256, 256) }

    fun explosion1(startTime: Long): SingleLoopImage =
        SingleLoopImage(startTime = startTime, duration = 50, frames = exp1Frames)

    private val exp2Frames: Array<Image> =
        Array(64) { i -> WritableImage(image("explosion2.png").pixelReader, i * 192, 0, 192, 192) }

    fun explosion2(startTime: Long): SingleLoopImage =
        SingleLoopImage(startTime = startTime, duration = 50, frames = exp2Frames)

    val rocks = run {
        val r1 = image("rock1.png").pixelReader
        val r2 = image("rock2.png").pixelReader
        listOf(
            MultiLoopImage(duration = 100, frames = Array(16) { i -> WritableImage(r1, i * 64, 0, 64, 64) }),
            MultiLoopImage(duration = 100, frames = Array(16) { i -> WritableImage(r1, (15 - i) * 64, 0, 64, 64) }),
            MultiLoopImage(duration = 100, frames = Array(16) { i -> WritableImage(r2, i * 32, 0, 32, 32) }),
            MultiLoopImage(duration = 100, frames = Array(16) { i -> WritableImage(r2, (15 - i) * 32, 0, 32, 32) })
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
    override fun frame(time: Long) = image
}

class MultiLoopImage(private val duration: Long, private val frames: Array<Image>) : ImageWrapper {
    init {
        assert(duration > 0)
        assert(frames.isNotEmpty())
        val w = frames[0].width.toInt()
        val h = frames[0].height.toInt()
        for (f in frames) {
            assert(w == f.width.toInt())
            assert(h == f.height.toInt())
        }
    }

    private val totalDuration = frames.size * duration

    override val width = frames[0].width
    override val height = frames[0].height

    override fun frame(time: Long) = frames[((time % totalDuration) / duration).toInt()]
}

class SingleLoopImage(private val startTime: Long, private val duration: Long, private val frames: Array<Image>) :
    ImageWrapper {
    init {
        assert(duration > 0)
        assert(frames.isNotEmpty())
        val w = frames[0].width.toInt()
        val h = frames[0].height.toInt()
        for (f in frames) {
            assert(w == f.width.toInt())
            assert(h == f.height.toInt())
        }
    }

    override val width = frames[0].width
    override val height = frames[0].height

    private val endTime = startTime + frames.size * duration

    override fun hasFrame(time: Long) = time < endTime

    override fun frame(time: Long) = frames[((time - startTime).coerceAtLeast(0) / duration).toInt()]
}
