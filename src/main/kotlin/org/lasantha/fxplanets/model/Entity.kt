package org.lasantha.fxplanets.model

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

enum class EntityCategory { PLANETARY, UFO, USER, MISC }
enum class EntityType(val category: EntityCategory) {
    STAR(EntityCategory.PLANETARY), PLANET(EntityCategory.PLANETARY), MOON(EntityCategory.PLANETARY),
    ASTEROID(EntityCategory.UFO), ALIENSHIP(EntityCategory.UFO), SPACEBLOB(EntityCategory.UFO),
    EXPLOSION(EntityCategory.MISC),
    FIGHTER(EntityCategory.USER);

    fun ephemeral(): Boolean = when (category) {
        EntityCategory.UFO, EntityCategory.USER, EntityCategory.MISC -> true
        EntityCategory.PLANETARY -> false
    }

}

class Entity(
    val name: String, val type: EntityType,
    val presentation: Presentation,
    private val game: Game, private val inactiveIfOutOfBounds: Boolean = false
) {

    private var active = true
        get
        set

    private var x = 0.0
        get

    private var y = 0.0
        get

    private var lastX = Double.NaN
        get

    private var lastY = Double.NaN
        get

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
        return "${javaClass.simpleName}{name=$name, type=${type.name}, x=${x.roundToInt()}, y=${y.roundToInt()}, " +
                "presentation=$presentation}"
    }
}
