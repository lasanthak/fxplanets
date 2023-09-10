package org.lasantha.fxplanets.controller

import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import org.lasantha.fxplanets.model.*
import org.lasantha.fxplanets.service.EntityService
import org.lasantha.fxplanets.view.ImageLib
import org.lasantha.fxplanets.view.MusicLib

class GameController(private val imageLib: ImageLib, private val musicLib: MusicLib, val entityService: EntityService) {
    private lateinit var gc: GraphicsContext

    private val shapes = LinkedHashSet<Shape>()
    private val longLivedShapes = LinkedHashSet<Shape>()
    private val shortLivedShapes = LinkedHashSet<Shape>()
    private val collidingShapes = LinkedHashSet<Shape>()
    private val collisionMap = mutableMapOf<Shape, Shape>()

    private var nextFighterTick = Long.MAX_VALUE
    private var nextUFOTick = 1500L

    private lateinit var flightControlPath: ControlPath

    init {
        val sunEntity = entityService.sun()
        val earthEntity = entityService.earth()
        createAddShape(sunEntity, entityService.sunPath())
        createAddShape(entityService.planet(), entityService.planetPath(sunEntity))
        createAddShape(earthEntity, entityService.earthPath(sunEntity))
        createAddShape(entityService.moon(), entityService.moonPath(earthEntity))

        val fighterEntity = entityService.fighter()
        flightControlPath = entityService.fighterPath(fighterEntity)
        createAddShape(fighterEntity, flightControlPath)
    }

    fun initGC(gc: GraphicsContext) {
        if (this::gc.isInitialized) {
            throw IllegalStateException("GraphicsContext is already initialized")
        }
        this.gc = gc
    }

    fun update(time: Long) {
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
            longLivedShapes.firstOrNull { b -> b != a && !collisionMap.contains(a) && a.entity.clip(b.entity) }
                ?.let { b ->
                    collisionMap[a] = b
                    if (a.entity.category.ephemeral) {
                        a.entity.active = false
                    }
                    if (a.entity.presentation.type == PresentationType.FIGHTER && handleFighterKilled(time)) {
                        nextFighterTick = time + 3000
                    }
                    val entity = entityService.explosion(time, a.entity, b.entity)
                    val path = entityService.explosionPath(time, entity, b.entity)
                    val image = imageLib.newImage(entity.presentation, time)
                    shapesToAdd.add(Shape(entity, path, image))
                    if (game.debug) {
                        println("Collision: ${a.entity.name} -> ${b.entity.name}" + time)
                    }
                    if (game.mainAudioEnabled && game.soundEnabled) {
                        musicLib.explosion.play()
                    }
                }
        }

        // Generate UFOs
        if (time > nextUFOTick) {
            nextUFOTick = time + 5000 + game.rand.nextLong(5000)
            val entity = entityService.ufo(time)
            val path = entityService.ufoPath(time)
            val image = imageLib.getImage(entity.presentation)
            shapesToAdd.add(Shape(entity, path, image))
        }

        // New fighter
        if (nextFighterTick <= time) {
            nextFighterTick = Long.MAX_VALUE
            val fighterEntity = entityService.fighter()
            flightControlPath = entityService.fighterPath(fighterEntity)
            val fighterImage = imageLib.getImage(fighterEntity.presentation)
            shapesToAdd.add(Shape(fighterEntity, flightControlPath, fighterImage))
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
        val image = imageLib.getImage(entity.presentation)
        return addShape(Shape(entity, path, image))
    }

    private fun addShape(s: Shape): Shape {
        shapes.add(s)
        when (s.entity.category.ephemeral) {
            true -> shortLivedShapes.add(s)
            false -> longLivedShapes.add(s)
        }
        when (s.entity.category) {
            EntityCategory.UFO,
            EntityCategory.USER -> collidingShapes.add(s)

            else -> {}
        }
        println("Created: ${s.entity}")
        return s
    }

    private fun removeShape(s: Shape): Shape {
        shapes.remove(s)
        longLivedShapes.remove(s)
        shortLivedShapes.remove(s)
        collidingShapes.remove(s)
        collisionMap.remove(s)
        println("Removed: ${s.entity}")
        return s
    }

    fun handleFighterKilled(time: Long): Boolean {
        val game = entityService.game
        if (game.live >= game.maxLives) {
            println("You lost!")
            return false
        }
        println("Lives lost: ${game.live}")

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

    fun handleKeyPress(time: Long, keyCode: KeyCode) {
        when (keyCode) {
            KeyCode.LEFT -> flightControlPath.addX(-20.0)
            KeyCode.RIGHT -> flightControlPath.addX(20.0)
            else -> {}
        }
    }
}
