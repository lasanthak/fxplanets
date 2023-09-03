package org.lasantha.fxplanets

import javafx.scene.image.Image
import javafx.scene.image.WritableImage

class ImageLib {
    val sun = StaticImage(image("sun.png"))

    val planet = StaticImage(image("planet1.png"))

    val moon = run {
        val r = image("moon.png").pixelReader
        MultiLoopImage(duration = 160,
            frames = Array(19) { i -> WritableImage(r, (i % 4) * 24, (i / 4) * 24, 24, 24) })
    }

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
        val r3r = Array(48) { image("rock3/$it.png").pixelReader }
        val blr = Array(60) { image("blob/$it.png").pixelReader }
        listOf(
            MultiLoopImage(100, Array(16) { WritableImage(r1, (it * 64) + 11, 11, 40, 40) }),
            MultiLoopImage(100, Array(16) { WritableImage(r1, (15 - it) * 64 + 11, 11, 40, 40) }),
            MultiLoopImage(100, Array(16) { WritableImage(r2, it * 32, 0, 32, 32) }),
            MultiLoopImage(100, Array(16) { WritableImage(r2, (15 - it) * 32, 0, 32, 32) }),
            MultiLoopImage(70, Array(48) { i -> WritableImage(r3r[i], 2, 2, 36, 36) }),
            MultiLoopImage(70, Array(48) { i -> WritableImage(r3r[47 - i], 2, 2, 36, 36) }),
            MultiLoopImage(50, Array(60) { i -> WritableImage(blr[i], 9, 14, 42, 42) }),
            MultiLoopImage(100, Array(6) { i -> image("ship/s${i + 1}.png") }),
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
