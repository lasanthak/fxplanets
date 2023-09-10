package org.lasantha.fxplanets.model

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

enum class EntityCategory(val ephemeral: Boolean = true) {
    PLANETARY(false),
    UFO, USER, MISC;
}


class Entity(
    val name: String, val category: EntityCategory, val presentation: Presentation,
    val game: Game, private val inactiveIfOutOfBounds: Boolean = false
) {

    var active = true

    var x = 0.0
        private set

    var y = 0.0
        private set

    var lastX = Double.NaN
        private set

    var lastY = Double.NaN
        private set

    fun update(location: Pair<Double, Double>) {
        lastX = x
        lastY = y
        x = location.first
        y = location.second

        if (inactiveIfOutOfBounds && (x < 0 || y < 0 || x > game.width || y > game.height)) {
            active = false
        }
    }

    fun clip(target: Entity): Boolean = clipBox(target) && clipCircle(target)

    fun clipBox(other: Entity): Boolean {
        val xClips = if (x < other.x) {
            other.x - x < presentation.width
        } else {
            x - other.x < other.presentation.width
        }

        if (xClips) {
            return if (y < other.y) {
                other.y - y < presentation.height
            } else {
                y - other.y < other.presentation.height
            }
        }

        return false
    }

    private fun clipCircle(other: Entity): Boolean {
        val len = max(presentation.width, presentation.height)
        val otherLen = max(other.presentation.width, other.presentation.height)
        return ((x - other.x).pow(2) + (y - other.y).pow(2)) < ((len + otherLen) / 2.0).pow(2)
    }

    override fun toString(): String {
        return "${javaClass.simpleName}{name=$name, category=${category.name}, x=${x.roundToInt()}, y=${y.roundToInt()}, " +
                "presentation=$presentation}"
    }
}
