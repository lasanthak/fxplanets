package org.lasantha.fxplanets.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

sealed interface Path {
    /**
     * @param time - time in ms
     */
    fun location(time: Long): Pair<Double, Double>

}

/**
 * @param startX - x coordinate of the starting point
 * @param startY - y coordinate of the starting point
 */
class StationaryPath(private val startX: Double, private val startY: Double) : Path {
    private val point = startX to startY
    override fun location(time: Long): Pair<Double, Double> = point
}

/**
 * @param startTime - start time in ms
 * @param entity - entity this path is associated with
 * @param parent - the parent entity to follow
 */
class PiggyBackPath(private val startTime: Long, private val entity: Entity, private val parent: Entity) : Path {
    private val gapW = (parent.presentation.width - entity.presentation.width) / 2.0
    private val gapH = (parent.presentation.height - entity.presentation.height) / 2.0
    override fun location(time: Long): Pair<Double, Double> = (parent.x + gapW) to (parent.y + gapH)
}

/**
 * @param a - a parameter of ellipsis
 * @param b - b parameter of ellipsis
 * @param p - phase of the angle in radians
 * @param aV - angular velocity (omega) in radians per ms
 * @param parent - the parent entity around which the entity is orbiting
 */
class EllipticalPath(
    private val a: Double, private val b: Double, private val p: Double, private val aV: Double, private val parent: Entity
) : Path {
    companion object {
        const val TWO_PI = PI * 2.0
    }

    override fun location(time: Long): Pair<Double, Double> {
        val theta = (time * aV) % TWO_PI
        return (parent.x + a * cos(theta + p)) to (parent.y + b * sin(theta + p))
    }
}


/**
 * @param vX - velocity along the x-axis (pixels per ms)
 * @param vY - velocity along the y-axis (pixels per ms)
 * @param startX - x coordinate of the starting point
 * @param startY - y coordinate of the starting point
 * @param startTime - start time in ms
 */
class LinearPath(
    private val vX: Double, private val vY: Double,
    private val startX: Double, private val startY: Double, private val startTime: Long
) : Path {
    override fun location(time: Long): Pair<Double, Double> {
        val t = time - startTime
        return (startX + vX * t) to (startY + vY * t)
    }
}

/**
 * @param startX - x coordinate of the starting point
 * @param startY - y coordinate of the starting point
 */
class ControlPath(private val startX: Double, private val startY: Double, private val entity: Entity) : Path {
    private var x = startX
    private var y = startY

    private val maxX = entity.game.width - entity.presentation.width - 5.0
    private val minX = 5.0

    override fun location(time: Long): Pair<Double, Double> = x to y

    fun addX(delta: Double) {
        val newX = x  + delta
        if (newX > minX && newX < maxX) {
            x += delta
        }
        println("newX = $newX")
    }
}
