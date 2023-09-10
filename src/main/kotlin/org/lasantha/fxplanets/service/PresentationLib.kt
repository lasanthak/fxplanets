package org.lasantha.fxplanets.service

import org.lasantha.fxplanets.model.Presentation
import org.lasantha.fxplanets.model.PresentationType

class PresentationLib {
    val sun: Presentation = Presentation("sun", PresentationType.STAR, 80, 80)
    val planet: Presentation = Presentation("planet", PresentationType.PLANET, 32, 32)
    val earth: Presentation = Presentation("earth", PresentationType.PLANET, 40, 40)
    val moon: Presentation = Presentation("moon", PresentationType.MOON, 24, 24)

    val explosion1: Presentation = Presentation("explosion1", PresentationType.EXPLOSION, 256, 256)
    val explosion2: Presentation = Presentation("explosion2", PresentationType.EXPLOSION, 192, 192)

    val rock1: Presentation = Presentation("rock1", PresentationType.ASTEROID, 34, 34)
    val rock2: Presentation = Presentation("rock2", PresentationType.ASTEROID, 34, 34)
    val rock3: Presentation = Presentation("rock3", PresentationType.ASTEROID, 32, 32)
    val rock4: Presentation = Presentation("rock4", PresentationType.ASTEROID, 32, 32)
    val rock5: Presentation = Presentation("rock5", PresentationType.ASTEROID, 36, 36)
    val rock6: Presentation = Presentation("rock6", PresentationType.ASTEROID, 36, 36)
    val blob1: Presentation = Presentation("blob1", PresentationType.SPACEBLOB, 42, 42)
    val blob2: Presentation = Presentation("blob2", PresentationType.SPACEBLOB, 38, 38)
    val ship1: Presentation = Presentation("ship1", PresentationType.ALIENSHIP, 39, 28)

    val fighter: Presentation = Presentation("fighter", PresentationType.FIGHTER, 64, 64)
    fun ufos() = listOf(rock1, rock2, rock3, rock4, rock5, rock6, blob1, blob2, ship1)

    fun explosions() = listOf(explosion1, explosion2)
}
