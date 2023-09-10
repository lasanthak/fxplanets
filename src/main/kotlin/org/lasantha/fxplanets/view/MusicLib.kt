package org.lasantha.fxplanets.view

import javafx.scene.media.AudioClip
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import org.lasantha.fxplanets.model.Game

class MusicLib {

    private val bgMusicPlayer = createBgMusicPlayer("../music/bg_music_return.mp3")

    val explosion = createAudioClip("../music/explosion.mp3")

    fun playMusic(game: Game) {
        if (game.mainAudioEnabled && game.musicEnabled) {
            bgMusicPlayer.play()
        }
    }

    fun pauseMusic(game: Game) {
        if (game.mainAudioEnabled && game.musicEnabled) {
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
