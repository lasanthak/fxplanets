package org.lasantha.fxplanets.model

import kotlin.math.PI
import kotlin.math.cos
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
 * @param presentation - presentation of the entity this path is associated with
 * @param parent - the parent entity around which the entity is orbiting
 */
class PiggyBackPath(private val startTime: Long, private val presentation: Presentation, private val parent: Entity) : Path {
    private val gapW = (presentation.width - parent.presentation.width) / 2.0
    private val gapH = (presentation.height - parent.presentation.height) / 2.0
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
