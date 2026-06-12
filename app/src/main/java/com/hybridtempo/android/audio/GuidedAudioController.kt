package com.hybridtempo.android.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class GuidedAudioController(
    private val context: Context,
    private val trackName: String,
) {
    private var player: ExoPlayer? = null

    fun setPlaying(playing: Boolean) {
        val currentPlayer = player ?: buildPlayer() ?: return

        if (playing) {
            currentPlayer.play()
        } else {
            currentPlayer.pause()
        }
    }

    fun release() {
        player?.release()
        player = null
    }

    private fun buildPlayer(): ExoPlayer? {
        val rawResourceId = context.resources.getIdentifier(trackName, "raw", context.packageName)
        if (rawResourceId == 0) {
            release()
            return null
        }

        return ExoPlayer.Builder(context).build().also { exoPlayer ->
            val uri = Uri.parse("android.resource://${context.packageName}/$rawResourceId")
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.volume = 1f
            exoPlayer.prepare()
            player = exoPlayer
        }
    }
}
