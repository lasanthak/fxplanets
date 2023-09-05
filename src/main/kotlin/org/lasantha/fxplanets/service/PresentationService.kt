package org.lasantha.fxplanets.service

import org.lasantha.fxplanets.model.Presentation

class PresentationService() {
    val sun: Presentation = Presentation("sun", 80, 80)
    val planet: Presentation = Presentation("planet", 32, 32)
    val earth: Presentation = Presentation("earth", 40, 40)
    val moon: Presentation = Presentation("moon", 24, 24)

    val explosion1: Presentation = Presentation("explosion1", 256, 256)
    val explosion2: Presentation = Presentation("explosion2", 192, 192)

    val rock1: Presentation = Presentation("rock1", 34, 34)
    val rock2: Presentation = Presentation("rock2", 34, 34)
    val rock3: Presentation = Presentation("rock3", 32, 32)
    val rock4: Presentation = Presentation("rock4", 32, 32)
    val rock5: Presentation = Presentation("rock5", 36, 36)
    val rock6: Presentation = Presentation("rock6", 36, 36)
    val blob1: Presentation = Presentation("blob1", 42, 42)
    val blob2: Presentation = Presentation("blob2", 38, 38)
    val ship1: Presentation = Presentation("ship1", 39, 28)

    val fighter: Presentation = Presentation("fighter", 64, 64)

}
