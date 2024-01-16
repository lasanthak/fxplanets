package org.lasantha.fxplanets.controller

import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import org.lasantha.fxplanets.model.ControlPath
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.EntityCategory
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.model.PresentationType
import org.lasantha.fxplanets.service.EntityService
import org.lasantha.fxplanets.view.ImageLib
import org.lasantha.fxplanets.view.MusicLib

@Suppress("UNUSED_PARAMETER")
class GameController(
    private val keyState: KeyState, private val imageLib: ImageLib,
    private val musicLib: MusicLib, private val entityService: EntityService
) {
    private lateinit var gc: GraphicsContext

    private val shapes = LinkedHashSet<Shape>()
    private val protectedShapes = LinkedHashSet<Shape>()
    private val collidingShapes = LinkedHashSet<Shape>()
    private val collisionMap = mutableMapOf<Shape, Shape>()

    private var fighter: ControlShape
    private var nextFighterTick = Long.MAX_VALUE
    private var nextUFOTick = 1500L

    init {
        val sunEntity = entityService.sun()
        val earthEntity = entityService.earth()
        createAddShape(sunEntity, entityService.sunPath())
        createAddShape(entityService.planet(), entityService.planetPath(sunEntity))
        createAddShape(earthEntity, entityService.earthPath(sunEntity))
        createAddShape(entityService.moon(), entityService.moonPath(earthEntity))

        val fe = entityService.fighter()
        val cs = ControlShape(fe, entityService.fighterPath(fe), imageLib.getControlImage(fe.presentation))
        addShape(cs)
        fighter = cs
    }

    fun initGC(gc: GraphicsContext) {
        if (this::gc.isInitialized) {
            throw IllegalStateException("GraphicsContext is already initialized")
        }
        this.gc = gc
    }

    fun update(time: Long) {
        updateKeyStates(time)
        for (s in shapes) {
            s.entity.update(s.path.location(time))
        }
    }

    fun clear() {
        for (s in shapes) {
            val e = s.entity
            val p = e.presentation
            gc.clearRect(e.lastX, e.lastY, p.width.toDouble(), p.height.toDouble())
        }
    }

    fun makeNewEntities(time: Long) {
        val game = entityService.game
        val shapesToAdd = mutableListOf<Shape>()

        // Make collisions
        collisionMap.filter { (a, b) -> !a.entity.clipBox(b.entity) }.forEach { (a, _) -> collisionMap.remove(a) }
        for (a in collidingShapes) {
            protectedShapes.firstOrNull { b -> b != a && !collisionMap.contains(a) && a.entity.clip(b.entity) }
                ?.let { b ->
                    collisionMap[a] = b
                    if (a.entity.category.ephemeral) a.entity.active = false
                    if (b.entity.category.ephemeral) b.entity.active = false
                    if (a == fighter || b == fighter) updateWinStatus(time)

                    val e = entityService.explosion(time, a.entity, b.entity)
                    shapesToAdd.add(Shape(e, entityService.explosionPath(time, e, b.entity), imageLib.newImage(e.presentation, time)))

                    if (game.debug) {
                        println("[$time] Collision: ${a.entity.name} -> ${b.entity.name}")
                    }
                    if (game.mainAudioEnabled && game.soundEnabled) {
                        musicLib.explosion.play()
                    }
                }
        }

        // Generate UFOs
        if (time > nextUFOTick) {
            nextUFOTick = time + 5000 + game.rand.nextLong(5000)
            val e = entityService.ufo(time)
            shapesToAdd.add(Shape(e, entityService.ufoPath(time), imageLib.getImage(e.presentation)))
        }

        // New fighter
        if (nextFighterTick <= time) {
            nextFighterTick = Long.MAX_VALUE
            val fe = entityService.fighter()
            val cs = ControlShape(fe, entityService.fighterPath(fe), imageLib.getControlImage(fe.presentation))
            shapesToAdd.add(cs)
            fighter = cs
        }

        for (s in shapesToAdd) {
            s.entity.update(s.path.location(time))
            addShape(s)
        }
    }

    fun draw(time: Long) {
        shapes.filterNot { it.entity.active && it.image.hasFrame(time) }.forEach { removeShape(it) }
        for (s in shapes) {
            val e = s.entity
            gc.drawImage(s.image.frame(time), e.x, e.y)
        }
    }

    private fun createAddShape(entity: Entity, path: Path): Shape {
        return addShape(Shape(entity, path, imageLib.getImage(entity.presentation)))
    }

    private fun addShape(s: Shape): Shape {
        val e = s.entity
        shapes.add(s)
        if (!e.category.ephemeral || e.presentation.type == PresentationType.FIGHTER) {
            protectedShapes.add(s)
        }
        if (e.category == EntityCategory.UFO || e.category == EntityCategory.USER) {
            collidingShapes.add(s)
        }
        return s
    }

    private fun removeShape(s: Shape): Shape {
        shapes.remove(s)
        protectedShapes.remove(s)
        collidingShapes.remove(s)
        collisionMap.remove(s)
        return s
    }

    private fun updateWinStatus(time: Long): Boolean {
        val game = entityService.game
        if (game.lost()) {
            println("(ツ) You lost!")
            nextFighterTick = Long.MAX_VALUE
            return false
        }
        println("(Θ︹Θ) Lives: remaining=${game.maxLives - game.live}, lost=${game.live}")
        nextFighterTick = time + 3000
        game.live += 1
        return true
    }

    fun handlePause(time: Long) {
        val game = entityService.game
        musicLib.pauseMusic(game)
    }

    fun handleResume(time: Long) {
        val game = entityService.game
        musicLib.playMusic(game)
    }

    fun updateKeyStates(time: Long) {
        if (keyState.left) {
            fighter.controlPath.setDeltaStopTime(time, ControlPath.Direction.LEFT)
            fighter.controlImage.setDeltaStopTime(time, ControlPath.Direction.LEFT)
        }
        if (keyState.right) {
            fighter.controlPath.setDeltaStopTime(time, ControlPath.Direction.RIGHT)
            fighter.controlImage.setDeltaStopTime(time, ControlPath.Direction.RIGHT)
        }
        if (keyState.up) {
            fighter.controlPath.setDeltaStopTime(time, ControlPath.Direction.UP)
            fighter.controlImage.setDeltaStopTime(time, ControlPath.Direction.UP)
        }
        if (keyState.down) {
            fighter.controlPath.setDeltaStopTime(time, ControlPath.Direction.DOWN)
            fighter.controlImage.setDeltaStopTime(time, ControlPath.Direction.DOWN)
        }
    }
}
