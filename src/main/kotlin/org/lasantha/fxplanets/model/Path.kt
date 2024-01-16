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

sealed interface ControlPath : Path {
    enum class Direction { LEFT, RIGHT, UP, DOWN }

    fun setDeltaStopTime(time: Long, direction: Direction)
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
 * @param v - velocity per sec
 */
class LRUDControlPath(startX: Double, startY: Double, v: Double, val timeDelta: Long, entity: Entity) : ControlPath {
    private var x = startX
    private var y = startY
    private var deltaStopTimeL = -1L
    private var deltaStopTimeR = -1L
    private var deltaStopTimeU = -1L
    private var deltaStopTimeD = -1L

    private val spaceDelta = v / entity.game.fps.toDouble()
    private val minX = 5.0
    private val maxX = entity.game.width - entity.presentation.width - 5.0
    private val minY = startY - 200.0
    private val maxY = entity.game.height - entity.presentation.height - 5.0

    override fun location(time: Long): Pair<Double, Double> {
        if (time < deltaStopTimeL) {
            val newX = x - spaceDelta
            if (newX > minX && newX < maxX) {
                x = newX
            }
        }
        if (time < deltaStopTimeR) {
            val newX = x + spaceDelta
            if (newX > minX && newX < maxX) {
                x = newX
            }
        }
        if (time < deltaStopTimeU) {
            val newY = y - spaceDelta
            if (newY > minY && newY < maxY) {
                y = newY
            }
        }
        if (time < deltaStopTimeD) {
            val newY = y + spaceDelta
            if (newY > minY && newY < maxY) {
                y = newY
            }
        }

        return x to y
    }

    override fun setDeltaStopTime(time: Long, direction: ControlPath.Direction) {
        val t = time + timeDelta
        when (direction) {
            ControlPath.Direction.LEFT -> deltaStopTimeL = t
            ControlPath.Direction.RIGHT -> deltaStopTimeR = t
            ControlPath.Direction.UP -> deltaStopTimeU = t
            ControlPath.Direction.DOWN -> deltaStopTimeD = t
        }
    }
}
