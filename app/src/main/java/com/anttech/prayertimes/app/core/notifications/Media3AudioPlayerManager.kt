package com.anttech.prayertimes.core.notifications

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.anttech.prayertimes.features.settings.domain.AudioPlayerManager
import com.anttech.prayertimes.features.settings.domain.SoundOption

class Media3AudioPlayerManager(
    private val context: Context
) : AudioPlayerManager {

    private var exoPlayer: ExoPlayer? = null
    private var onFinished: (() -> Unit)? = null

    override fun play(soundOption: SoundOption, volume: Float, onFinished: () -> Unit) {
        stop()

        if (soundOption is SoundOption.Silent || soundOption is SoundOption.VibrateOnly) {
            onFinished()
            return
        }

        val mediaItem = when (soundOption) {
            is SoundOption.BuiltIn -> {
                val resId = context.resources.getIdentifier(
                    soundOption.resName,
                    "raw",
                    context.packageName
                )
                if (resId == 0) {
                    onFinished()
                    return
                }
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$resId"))
            }
            is SoundOption.CustomFile -> MediaItem.fromUri(Uri.parse(soundOption.uriString))
            is SoundOption.Silent, SoundOption.VibrateOnly -> {
                onFinished()
                return
            }
        }

        val player = ExoPlayer.Builder(context).build()
        exoPlayer = player
        this.onFinished = onFinished
        player.volume = perceptualVolume(volume)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finish(player)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                finish(player)
            }
        })
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = perceptualVolume(volume)
    }

    companion object {
        /**
         * Linear slider (0..1) -> perceptual loudness for ExoPlayer.
         * Human hearing is roughly logarithmic; a power curve (^2) makes the
         * slider feel linear to the ear.
         */
        fun perceptualVolume(linear: Float): Float =
            linear.coerceIn(0f, 1f).let { it * it }
    }

    override fun stop() {
        onFinished = null
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    /**
     * Çalma kendiliğinden bitince durumu sıfırlar ve dinleyiciyi bilgilendirir.
     * [player] artık güncel oynatıcı değilse (yeni ses başlamışsa) hiçbir şey yapmaz.
     */
    private fun finish(player: ExoPlayer) {
        if (exoPlayer !== player) return
        val callback = onFinished
        stop()
        callback?.invoke()
    }
}
