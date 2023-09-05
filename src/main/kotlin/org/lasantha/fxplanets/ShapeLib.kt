package org.lasantha.fxplanets

import org.lasantha.fxplanets.service.ImageService
import kotlin.math.cos
import kotlin.math.sin

class ShapeLib {
    private val centerX = AppConf.width / 2.0
    private val centerY = AppConf.height / 2.0

    val imageLib = ImageService()

    private fun sunLocator(): FXLocator {
        val point = (centerX - imageLib.sun.halfW()) to (centerY - imageLib.sun.halfH())
        return FXLocator(fLocation = { _ -> point })
    }

    val sunShape = FXShape("Sun", imageLib.sun, sunLocator())

    private fun earthLocator(): FXLocator {
        val r = 320.0
        val phase = AppConf.rand.nextDouble() * 7.2
        return FXLocator(fLocation = { time ->
            val t = time * 0.00073
            (centerX + r * cos(t + phase) * 1.5) to (centerY + r * sin(t + phase))
        })
    }

    private val earthLoc = earthLocator()
    val earthShape = FXShape("Earth", imageLib.earth, earthLoc)

    private fun moonLocator(): FXLocator {
        val r = 45.0
        val phase = AppConf.rand.nextDouble() * 7.2
        return FXLocator(fLocation = { time ->
            val t = time * 0.00377
            (earthLoc.getX() + r * cos(t + phase)) to (earthLoc.getY() + r * sin(t + phase) * 1.5)
        })
    }

    val moonShape = FXShape("Moon", imageLib.moon, moonLocator())

    private fun planetLocator(): FXLocator {
        val r = 145.0
        val phase = AppConf.rand.nextDouble() * 0.0072
        return FXLocator(fLocation = { time ->
            val t = time * -0.0011
            (centerX + r * cos(t + phase)) to (centerY + r * sin(t + phase) * 1.9)
        })
    }

    val planetShape = FXShape("Planet", imageLib.planet, planetLocator())

    private fun asteroidLocator(startTime: Long): FXLocator {
        fun locFun(maxLen: Double, forward: Boolean, add: Boolean): (time: Long) -> Double {
            val v = 0.05 + 0.1 * AppConf.rand.nextDouble()
            val c = if (add) (maxLen * 0.8 * AppConf.rand.nextDouble()) else 0.0
            return if (forward) {
                { time -> c + v * (time - startTime) }
            } else {
                { time -> maxLen - c - (v * (time - startTime)) }
            }
        }

        val addC = AppConf.rand.nextBoolean()

        val fx = locFun(AppConf.width, AppConf.rand.nextBoolean(), addC)
        val fy = locFun(AppConf.height, AppConf.rand.nextBoolean(), !addC)

        return FXLocator(destroyIfOutOfBounds = true, fLocation = { time -> fx(time) to fy(time) })
    }

    fun randomAsteroid(startTime: Long): FXShape {
        val image = imageLib.rocks[AppConf.rand.nextInt(imageLib.rocks.size)]
        return FXShape("UFO-$startTime", image, asteroidLocator(startTime))
    }

    fun randomExplosion(a: FXShape, b: FXShape, startTime: Long): FXShape {
        val img = if (AppConf.rand.nextBoolean()) imageLib.explosion1(startTime)
        else imageLib.explosion2(startTime)

        val gapW = b.image.halfW() - img.halfW()
        val gapH = b.image.halfH() - img.halfH()
        return FXShape(
            "Explosion<${a.name}, ${b.name}>", img,
            FXLocator(fLocation = { _ -> (b.locator.getX() + gapW) to (b.locator.getY() + gapH) })
        )
    }

    fun fighterShape(): FXShape {
        val point = (centerX - imageLib.fighter.halfW()) to (AppConf.height - 100.0)
        val locator = FXLocator(fLocation = { _ -> point })
        return FXShape("Fighter", imageLib.fighter, locator)
    }
}
