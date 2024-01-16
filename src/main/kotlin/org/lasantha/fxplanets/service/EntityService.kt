package org.lasantha.fxplanets.service

import org.lasantha.fxplanets.model.EllipticalPath
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.EntityCategory
import org.lasantha.fxplanets.model.Game
import org.lasantha.fxplanets.model.LRUDControlPath
import org.lasantha.fxplanets.model.LinearPath
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.model.PiggyBackPath
import org.lasantha.fxplanets.model.PresentationType
import org.lasantha.fxplanets.model.StationaryPath

class EntityService(val game: Game, val prLib: PresentationLib) {
    private val centerX = game.width / 2.0
    private val centerY = game.height / 2.0

    private val ufoPresentations = prLib.ufos().shuffled(game.rand)
    private val explosionPresentations = prLib.explosions().shuffled(game.rand)

    fun sun() = Entity("Sun", EntityCategory.PLANETARY, prLib.sun, game)

    fun sunPath() =
        StationaryPath(startX = centerX - prLib.sun.width / 2.0, startY = centerY - prLib.sun.height / 2.0)

    fun earth() = Entity("Earth", EntityCategory.PLANETARY, prLib.earth, game)

    fun earthPath(sun: Entity) =
        EllipticalPath(a = 500.0, b = 350.0, p = game.rand.nextDouble() * 7.2, aV = 0.00073, parent = sun)

    fun moon() = Entity("Moon", EntityCategory.PLANETARY, prLib.moon, game)

    fun moonPath(earth: Entity) =
        EllipticalPath(a = 45.0, b = 67.5, p = game.rand.nextDouble() * 7.2, aV = 0.00377, parent = earth)

    fun planet() = Entity("Planet", EntityCategory.PLANETARY, prLib.planet, game)

    fun planetPath(sun: Entity) =
        EllipticalPath(a = 150.0, b = 250.0, p = game.rand.nextDouble() * 0.0072, aV = -0.0011, parent = sun)

    fun ufo(time: Long): Entity {
        val p = ufoPresentations[game.rand.nextInt(ufoPresentations.size)]
        val prefix = when (p.type) {
            PresentationType.ASTEROID -> "Asteroid"
            PresentationType.ALIENSHIP -> "AlienShip"
            PresentationType.SPACEBLOB -> "SpaceBlob"
            else -> "UFO"
        }
        return Entity("$prefix-$time", EntityCategory.UFO, p, game, inactiveIfOutOfBounds = true)
    }

    fun ufoPath(time: Long): Path {
        val multiplier = game.rand.nextDouble()
        var (startX, startY) = if (game.rand.nextBoolean()) {
            (game.width * 0.8 * multiplier) to 0.0
        } else {
            0.0 to (game.height * 0.8 * multiplier)
        }

        var vX = 0.05 + 0.1 * game.rand.nextDouble()
        var vY = 0.05 + 0.1 * game.rand.nextDouble()
        if (game.rand.nextBoolean()) { // x backward
            startX = game.width - startX
            vX = -vX
        }
        if (game.rand.nextBoolean()) { // y backward
            startY = game.height - startY
            vY = -vY
        }
        return LinearPath(vX = vX, vY = vY, startX = startX, startY = startY, startTime = time)
    }

    fun explosion(time: Long, a: Entity, b: Entity): Entity {
        val p = explosionPresentations[game.rand.nextInt(explosionPresentations.size)]
        return Entity("Explosion-$time<${a.name}, ${b.name}>", EntityCategory.MISC, p, game)
    }

    fun explosionPath(time: Long, entity: Entity, followEntity: Entity) =
        PiggyBackPath(time, entity, followEntity)

    fun fighter() =
        Entity("Fighter-${game.live}", EntityCategory.USER, prLib.fighter, game, inactiveIfOutOfBounds = true)

    fun fighterPath(entity: Entity) =
        LRUDControlPath(startX = centerX - entity.presentation.width / 2.0,
            startY = game.height - 200.0, v = 400.0,
            timeDelta = 20, entity = entity)

}
