package org.lasantha.fxplanets.model

import kotlin.math.roundToLong
import kotlin.random.Random


class Game(val width: Double, val height: Double) {
    var rand: Random = Random(62439) // 62439, 234, Random.Default

    //60 fps = duration 17 ms
    //50 fps = duration 20 ms
    //40 fps = duration 25 ms
    //33 fps = duration 30 ms
    //25 fps = duration 40 ms
    val fps = 33
    val tick = (1000.0 / fps.toDouble()).roundToLong()

    val maxLives = 5
    var live = 1

    var mainAudioEnabled = false
    var soundEnabled = true
    var musicEnabled = true

    var debug = true

    fun lost(): Boolean = (live >= maxLives)
}
