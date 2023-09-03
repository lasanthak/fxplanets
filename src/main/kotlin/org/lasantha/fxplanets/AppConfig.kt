package org.lasantha.fxplanets

import kotlin.random.Random

class AppConfig {
    companion object {
        //60 fps -> frame duration 17 ms
        //50 fps -> frame duration 20 ms
        //40 fps -> frame duration 25 ms
        //33 fps -> frame duration 30 ms
        //25 fps -> frame duration 40 ms
        var fps = 60

        var fxRandom: Random = Random.Default

        var mainAudioEnabled = false
        var soundEnabled = true
        var musicEnabled = true

        var width = 1600.0
        var height = 1200.0

        var debug = true
    }
}
