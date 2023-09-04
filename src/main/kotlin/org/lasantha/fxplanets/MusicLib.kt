package org.lasantha.fxplanets

import javafx.scene.media.AudioClip
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

class MusicLib {

    private val bgMusicPlayer = createBgMusicPlayer("music/bg_music_return.mp3")

    val explosion = createAudioClip("music/explosion.mp3")

    fun playMusic() {
        if (AppConf.mainAudioEnabled && AppConf.musicEnabled) {
            bgMusicPlayer.play()
        }
    }

    fun pauseMusic() {
        if (AppConf.mainAudioEnabled && AppConf.musicEnabled) {
            bgMusicPlayer.pause()
        }
    }

    private fun createBgMusicPlayer(media: String): MediaPlayer {
        val url = javaClass.getResource(media)?.toExternalForm()
        val sound = Media(url ?: throw IllegalArgumentException("Invalid music file"))
        val player = MediaPlayer(sound)
        player.cycleCount = Int.MAX_VALUE // repeat indefinitely
        return player
    }

    private fun createAudioClip(media: String): AudioClip {
        val url = javaClass.getResource(media)?.toExternalForm()
        return AudioClip(url ?: throw IllegalArgumentException("Invalid audio file"))
    }
}
