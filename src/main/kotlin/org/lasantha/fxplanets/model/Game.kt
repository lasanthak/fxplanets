package org.lasantha.fxplanets.model

import kotlin.random.Random


class Game(val width: Double, val height: Double) {
    var rand: Random = Random(234)
    //var rand: Random = Random(13) // 62439, 234, Random.Default

    val maxLives = 5
    var live = 0
}
