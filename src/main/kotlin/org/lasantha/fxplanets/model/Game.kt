package org.lasantha.fxplanets.model

import kotlin.random.Random


class Game(val width: Double, val height: Double) {
    var rand: Random = Random(62439) // 62439, 234, Random.Default

    //60 fps -> frame duration 17 ms
    //50 fps -> frame duration 20 ms
    //40 fps -> frame duration 25 ms
    //33 fps -> frame duration 30 ms
    //25 fps -> frame duration 40 ms
    var fps = 60

    val maxLives = 5
    var live = 1

    var mainAudioEnabled = false
    var soundEnabled = true
    var musicEnabled = true

    var debug = true
}
