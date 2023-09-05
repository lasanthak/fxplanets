package org.lasantha.fxplanets.service

import org.lasantha.fxplanets.AppConf
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.EntityCategory
import org.lasantha.fxplanets.model.EntityType
import org.lasantha.fxplanets.model.Game

class GameService(private val service: PresentationService) {

    private val entities = LinkedHashSet<Entity>()
    private val longLivedEntities = LinkedHashSet<Entity>()
    private val shortLivedEntities = LinkedHashSet<Entity>()
    private val collidingEntities = LinkedHashSet<Entity>()

    private val ufoPresentations = listOf(
        service.rock1, service.rock2, service.rock3,
        service.rock4, service.rock5, service.rock6,
        service.blob1, service.blob2, service.ship1
    ).shuffled(AppConf.rand)
    private val explosionPresentations = listOf(service.explosion1, service.explosion2).shuffled(AppConf.rand)

    val game = Game(AppConf.width, AppConf.height)

    val sun = addEntity(Entity("Sun", EntityType.STAR, service.sun, game))
    val earth = addEntity(Entity("Earth", EntityType.PLANET, service.earth, game))
    val moon = addEntity(Entity("Moon", EntityType.MOON, service.moon, game))
    val planet = addEntity(Entity("Planet", EntityType.PLANET, service.planet, game))

    fun randomUFO(time: Long): Entity {
        val presentation = ufoPresentations[AppConf.rand.nextInt(ufoPresentations.size)]
        val entity = Entity("UFO-$time", EntityType.ASTEROID, presentation, game)
        return addEntity(entity)
    }

    fun randomExplosion(time: Long, a: Entity, b: Entity): Entity {
        val presentation = explosionPresentations[AppConf.rand.nextInt(explosionPresentations.size)]
        val entity = Entity("Explosion-$time<${a.name}, ${b.name}>", EntityType.EXPLOSION, presentation, game)
        return addEntity(entity)
    }

    fun fighter(): Entity {
        val entity = Entity("Fighter-${game.live}", EntityType.FIGHTER, service.fighter, game)
        return addEntity(entity)
    }

    private fun addEntity(entity: Entity): Entity {
        entities.add(entity)
        if (entity.type.ephemeral()) {
            shortLivedEntities.add(entity)
        } else {
            longLivedEntities.add(entity)
        }
        when (entity.type.category) {
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

    fun entities(): Set<Entity> = entities
    fun longLivedEntities(): Set<Entity> = longLivedEntities
    fun shortLivedEntities(): Set<Entity> = shortLivedEntities
    fun collidingEntities(): Set<Entity> = collidingEntities
}
