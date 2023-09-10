package org.lasantha.fxplanets.view

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import org.lasantha.fxplanets.model.Presentation
import org.lasantha.fxplanets.service.PresentationLib
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageLib(private val prService: PresentationLib) {
    private val imageMap: Map<Presentation, ImageWrapper>
    private val exp1Frames: Array<Image>
    private val exp2Frames: Array<Image>

    init {
        val map = mutableMapOf<Presentation, ImageWrapper>()
        map[prService.sun] =
            StaticImage(WritableImage(image("sun.png").pixelReader, 4, 4, prService.sun.width, prService.sun.height))
        map[prService.planet] =
            StaticImage(WritableImage(image("planet1.png").pixelReader, 0, 0, prService.planet.width, prService.planet.height))
        map[prService.moon] = run {
            val r = image("moon.png").pixelReader
            val w = prService.moon.width
            val h = prService.moon.height
            DynamicImage(duration = 160, frames = Array(19) { WritableImage(r, (it % 4) * w, (it / 4) * h, w, h) })
        }
        map[prService.earth] = run {
            val r = image("earth.png").pixelReader
            val w = prService.earth.width
            val h = prService.earth.height
            DynamicImage(duration = 50, frames = Array(256) { WritableImage(r, (it % 16) * w, (it / 16) * h, w, h) })
        }

        val r12 = image("rock1.png").pixelReader
        map[prService.rock1] =
            DynamicImage(100, Array(16) { WritableImage(r12, (it * 64) + 15, 15, prService.rock1.width, prService.rock1.height) })
        map[prService.rock2] =
            DynamicImage(100, Array(16) { WritableImage(r12, (15 - it) * 64 + 15, 15, prService.rock2.width, prService.rock2.height) })

        val r34 = image("rock2.png").pixelReader
        map[prService.rock3] =
            DynamicImage(100, Array(16) { WritableImage(r34, it * 32, 0, prService.rock3.width, prService.rock3.height) })
        map[prService.rock4] =
            DynamicImage(100, Array(16) { WritableImage(r34, (15 - it) * 32, 0, prService.rock4.width, prService.rock4.height) })

        val r56 = Array(48) { image("rock3/$it.png").pixelReader }
        map[prService.rock5] =
            DynamicImage(70, Array(48) { WritableImage(r56[it], 2, 2, prService.rock5.width, prService.rock5.height) })
        map[prService.rock6] =
            DynamicImage(70, Array(48) { WritableImage(r56[47 - it], 2, 2, prService.rock6.width, prService.rock6.height) })

        map[prService.blob1] = run {
            val r = Array(60) { image("blob1/$it.png").pixelReader }
            DynamicImage(50, Array(60) { WritableImage(r[it], 9, 14, prService.blob1.width, prService.blob1.height) })
        }

        map[prService.blob2] = run {
            val r = Array(60) { image("blob2/$it.png").pixelReader }
            DynamicImage(50, Array(60) { WritableImage(r[it], 1, 1, prService.blob2.width, prService.blob2.height) })
        }

        map[prService.ship1] = run {
            val r = Array(6) { image("ship/s${it + 1}.png").pixelReader }
            DynamicImage(100, Array(6) { WritableImage(r[it], 0, 5, prService.ship1.width, prService.ship1.height) })
        }

        map[prService.fighter] = run {
            val r = image("fighter.png").pixelReader
            val w = prService.fighter.width
            val h = prService.fighter.height
            LRLoopImage(duration = 100, frames = Array(7) { WritableImage(r, it * w, 0, w, h) })
        }

        imageMap = map.toMap()

        exp1Frames = Array(48) {
            WritableImage(image("explosion1.png").pixelReader, it * prService.explosion1.width, 0, prService.explosion1.width, prService.explosion1.height)
        }
        exp2Frames = Array(64) {
            WritableImage(image("explosion2.png").pixelReader, it * prService.explosion2.width, 0, prService.explosion2.width, prService.explosion2.height)
        }
    }

    fun getImage(presentation: Presentation): ImageWrapper =
        imageMap[presentation] ?: throw IllegalArgumentException("Presentation not implemented for id=${presentation.id}")

    fun newImage(presentation: Presentation, startTime: Long): ImageWrapper = when (presentation) {
        prService.explosion1 -> SingleLoopImage(startTime = startTime, duration = 50, frames = exp1Frames)
        prService.explosion2 -> SingleLoopImage(startTime = startTime, duration = 50, frames = exp2Frames)
        else -> throw IllegalArgumentException("Presentation not implemented for id=${presentation.id}")
    }

    fun bgImage(): Image = image("space.png")
    fun icon(): Image = image("galaxy.png")

    private fun image(name: String): Image =
        Image(ImageLib::class.java.getResourceAsStream("../img/$name"), 0.0, 0.0, true, true)
}

sealed interface ImageWrapper {
    val width: Double
    val height: Double

    fun halfW(): Double = width / 2.0
    fun halfH(): Double = height / 2.0

    fun hasFrame(time: Long): Boolean = true
    fun frame(time: Long): Image
}

class StaticImage(private val image: Image) : ImageWrapper {
    override val width = image.width
    override val height = image.height

    override fun frame(time: Long) = image
}

open class DynamicImage(private val duration: Long, private val frames: Array<Image>) : ImageWrapper {
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

    val totalDuration = frames.size * duration

    override val width = frames[0].width
    override val height = frames[0].height

    override fun frame(time: Long) = frames[((time % totalDuration) / duration).toInt()]
}

class SingleLoopImage(private val startTime: Long, private val duration: Long, private val frames: Array<Image>) :
    DynamicImage(duration, frames) {

    override fun hasFrame(time: Long) = time < startTime + totalDuration

    override fun frame(time: Long) = frames[((time - startTime).coerceAtLeast(0) / duration).toInt()]
}

class LRLoopImage(private val duration: Long, private val frames: Array<Image>) : DynamicImage(duration, frames) {
    private val minId = 0
    private val midId = frames.size / 2
    private val maxId = frames.size - 1
    override fun frame(time: Long): Image {
        val id = midId + (time / duration).toDouble().roundToInt()
        return frames[min(max(id, minId), maxId)]
    }
}
