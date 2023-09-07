package org.lasantha.fxplanets.service

import org.lasantha.fxplanets.model.EllipticalPath
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.EntityCategory
import org.lasantha.fxplanets.model.Game
import org.lasantha.fxplanets.model.LinearPath
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.model.PiggyBackPath
import org.lasantha.fxplanets.model.StationaryPath

class GameService(private val prService: PresentationService) {

    val game = Game(width = 1600.0, height = 1200.0)
    private val centerX = game.width / 2.0
    private val centerY = game.height / 2.0

    private val entities = LinkedHashSet<Entity>()
    private val longLivedEntities = LinkedHashSet<Entity>()
    private val shortLivedEntities = LinkedHashSet<Entity>()
    private val collidingEntities = LinkedHashSet<Entity>()

    private val pathMap = HashMap<Entity, Path>()

    private val ufos = prService.ufos().shuffled(game.rand)
    private val explosions = prService.explosions().shuffled(game.rand)

    private val sun: Entity
    private val earth: Entity
    private val moon: Entity
    private val planet: Entity

    var fighter: Entity
        private set

    init {
        fighter = fighter()

        sun = addEntity(Entity("Sun", EntityCategory.PLANETARY, prService.sun, game))
        pathMap[sun] = StationaryPath(startX = centerX - prService.sun.width / 2.0, startY = centerY - prService.sun.height / 2.0)

        earth = addEntity(Entity("Earth", EntityCategory.PLANETARY, prService.earth, game))
        pathMap[earth] = EllipticalPath(a = 480.0, b = 320.0, p = game.rand.nextDouble() * 7.2, aV = 0.00073, parent = sun)

        moon = addEntity(Entity("Moon", EntityCategory.PLANETARY, prService.moon, game))
        pathMap[moon] = EllipticalPath(a = 45.0, b = 67.5, p = game.rand.nextDouble() * 7.2, aV = 0.00377, parent = earth)

        planet = addEntity(Entity("Planet", EntityCategory.PLANETARY, prService.planet, game))
        pathMap[planet] = EllipticalPath(a = 145.0, b = 275.5, p = game.rand.nextDouble() * 0.0072, aV = -0.0011, parent = sun)
    }


    fun randomUFO(time: Long): Entity {
        val p = ufos[game.rand.nextInt(ufos.size)]
        val e = Entity("UFO-$time", EntityCategory.UFO, p, game)
        pathMap[e] = ufoPath(time)
        return addEntity(e)
    }

    private fun ufoPath(time: Long): Path {
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

    fun randomExplosion(time: Long, a: Entity, b: Entity): Entity {
        val p = explosions[game.rand.nextInt(explosions.size)]
        val e = Entity("Explosion-$time<${a.name}, ${b.name}>", EntityCategory.MISC, p, game)
        pathMap[e] = PiggyBackPath(time, p, b)
        return addEntity(e)
    }

    fun fighter(): Entity {
        val p = prService.fighter
        val e = Entity("Fighter-${game.live}", EntityCategory.USER, p, game)
        pathMap[e] = StationaryPath(startX = centerX - p.width / 2.0, startY = game.height - 100.0)
        return addEntity(e)
    }

    private fun addEntity(entity: Entity): Entity {
        entities.add(entity)
        if (entity.category.ephemeral) {
            shortLivedEntities.add(entity)
        } else {
            longLivedEntities.add(entity)
        }
        when (entity.category) {
            EntityCategory.UFO, EntityCategory.USER, EntityCategory.MISC -> collidingEntities.add(entity)
            EntityCategory.PLANETARY -> {}
        }
        return entity
    }

    fun removeEntity(entity: Entity): Entity {
        entities.remove(entity)
        longLivedEntities.remove(entity)
        shortLivedEntities.remove(entity)
        collidingEntities.remove(entity)
        return entity
    }

    fun path(entity: Entity): Path = pathMap[entity] ?: throw IllegalArgumentException("No path for entity: $entity")

    fun entities(): Set<Entity> = entities
    fun longLivedEntities(): Set<Entity> = longLivedEntities
    fun shortLivedEntities(): Set<Entity> = shortLivedEntities
    fun collidingEntities(): Set<Entity> = collidingEntities
}
