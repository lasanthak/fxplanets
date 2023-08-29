package org.lasantha.fxplanets

import javafx.scene.media.AudioClip
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

class MusicLib {

    val bgMusicPlayer = backgroundMediaPlayer()

    val explosion = audioClip("music/explosion.mp3")

    private fun backgroundMediaPlayer(): MediaPlayer {
        val url = javaClass.getResource("music/bg_music_return.mp3")?.toExternalForm()
        val sound = Media(url ?: throw IllegalArgumentException("Invalid music file"))
        val player = MediaPlayer(sound)
        player.cycleCount = Int.MAX_VALUE // repeat indefinitely
        return player
    }

    private fun audioClip(media: String): AudioClip {
        val url = javaClass.getResource(media)?.toExternalForm()
        return AudioClip(url ?: throw IllegalArgumentException("Invalid audio file"))
    }
}