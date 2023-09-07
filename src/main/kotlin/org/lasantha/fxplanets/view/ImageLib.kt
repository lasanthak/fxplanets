package org.lasantha.fxplanets.view

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import org.lasantha.fxplanets.model.Presentation
import org.lasantha.fxplanets.service.PresentationService
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageLib(private val service: PresentationService) {
    private val imageMap: Map<Presentation, ImageWrapper>
    private val exp1Frames: Array<Image>
    private val exp2Frames: Array<Image>

    init {
        val map = mutableMapOf<Presentation, ImageWrapper>()
        map[service.sun] =
            StaticImage(WritableImage(image("sun.png").pixelReader, 4, 4, service.sun.width, service.sun.height))
        map[service.planet] =
            StaticImage(WritableImage(image("planet1.png").pixelReader, 0, 0, service.planet.width, service.planet.height))
        map[service.moon] = run {
            val r = image("moon.png").pixelReader
            val w = service.moon.width
            val h = service.moon.height
            DynamicImage(duration = 160, frames = Array(19) { WritableImage(r, (it % 4) * w, (it / 4) * h, w, h) })
        }
        map[service.earth] = run {
            val r = image("earth.png").pixelReader
            val w = service.earth.width
            val h = service.earth.height
            DynamicImage(duration = 50, frames = Array(256) { WritableImage(r, (it % 16) * w, (it / 16) * h, w, h) })
        }

        val r12 = image("rock1.png").pixelReader
        map[service.rock1] =
            DynamicImage(100, Array(16) { WritableImage(r12, (it * 64) + 15, 15, service.rock1.width, service.rock1.height) })
        map[service.rock2] =
            DynamicImage(100, Array(16) { WritableImage(r12, (15 - it) * 64 + 15, 15, service.rock2.width, service.rock2.height) })

        val r34 = image("rock2.png").pixelReader
        map[service.rock3] =
            DynamicImage(100, Array(16) { WritableImage(r34, it * 32, 0, service.rock3.width, service.rock3.height) })
        map[service.rock4] =
            DynamicImage(100, Array(16) { WritableImage(r34, (15 - it) * 32, 0, service.rock4.width, service.rock4.height) })

        val r56 = Array(48) { image("rock3/$it.png").pixelReader }
        map[service.rock5] =
            DynamicImage(70, Array(48) { WritableImage(r56[it], 2, 2, service.rock5.width, service.rock5.height) })
        map[service.rock6] =
            DynamicImage(70, Array(48) { WritableImage(r56[47 - it], 2, 2, service.rock6.width, service.rock6.height) })

        map[service.blob1] = run {
            val r = Array(60) { image("blob1/$it.png").pixelReader }
            DynamicImage(50, Array(60) { WritableImage(r[it], 9, 14, service.blob1.width, service.blob1.height) })
        }

        map[service.blob2] = run {
            val r = Array(60) { image("blob2/$it.png").pixelReader }
            DynamicImage(50, Array(60) { WritableImage(r[it], 1, 1, service.blob2.width, service.blob2.height) })
        }

        map[service.ship1] = run {
            val r = Array(6) { image("ship/s${it + 1}.png").pixelReader }
            DynamicImage(100, Array(6) { WritableImage(r[it], 0, 5, service.ship1.width, service.ship1.height) })
        }

        map[service.fighter] = run {
            val r = image("fighter.png").pixelReader
            val w = service.fighter.width
            val h = service.fighter.height
            LRLoopImage(duration = 100, frames = Array(7) { WritableImage(r, it * w, 0, w, h) })
        }

        imageMap = map.toMap()

        exp1Frames = Array(48) {
            WritableImage(image("explosion1.png").pixelReader, it * service.explosion1.width, 0, service.explosion1.width, service.explosion1.height)
        }
        exp2Frames = Array(64) {
            WritableImage(image("explosion2.png").pixelReader, it * service.explosion2.width, 0, service.explosion2.width, service.explosion2.height)
        }
    }

    fun image(presentation: Presentation): ImageWrapper =
        imageMap[presentation] ?: throw IllegalArgumentException("Presentation not implemented for id=${presentation.id}")

    fun image(presentation: Presentation, startTime: Long): ImageWrapper = when (presentation) {
        service.explosion1 -> SingleLoopImage(startTime = startTime, duration = 50, frames = exp1Frames)
        service.explosion2 -> SingleLoopImage(startTime = startTime, duration = 50, frames = exp2Frames)
        else -> throw IllegalArgumentException("Presentation not implemented for id=${presentation.id}")
    }

    fun bgImage(): Image = image("space.png")
    fun icon(): Image = image("galaxy.png")

    private fun image(name: String): Image =
        Image(ImageLib::class.java.getResourceAsStream("img/$name"), 0.0, 0.0, true, true)
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
