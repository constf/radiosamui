package com.blinked.radiosamui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player.Listener
import androidx.media3.exoplayer.ExoPlayer

const val MUSIC_URL: String = "https://stream.mixadance.fm/radiosamui"

class RadioViewModel(private val inApplication: Application): AndroidViewModel(inApplication) {

    private val exoPlayer: ExoPlayer = getPlayerInstance(inApplication, MUSIC_URL).also { it.addListener(SamuiErrorListener()) }

    private var _errorCode: MutableLiveData<String> = MutableLiveData(null)
    val errorCode: LiveData<String> get() = _errorCode

    private var _metaData: MutableLiveData<String> = MutableLiveData(_musicData)
    val metaData: LiveData<String> get() = _metaData

    fun isPlayingNow(): Boolean {
        return exoPlayer.isPlaying
    }

    fun saveMusicData() {
        _musicData = metaData.value
    }


    fun Play() {
        if (exoPlayer.isPlaying) return

        exoPlayer.play()
    }

    fun Pause() {
        if(!exoPlayer.isPlaying) return
        exoPlayer.pause()
    }

    fun ReleasePlayer() {
        exoPlayer.release()
    }


    inner class SamuiErrorListener: Listener{
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            _errorCode.value = PlaybackException.getErrorCodeName(error.errorCode)
            Pause()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            _metaData.value = with(mediaMetadata) {
                val str = if (title.isNullOrEmpty()) "     " else title.toString()
                str + "  " + str + "  " + str + "  " + str + "  " + str + "  " + str + "  " + str + "  "
            }
        }
    }

    companion object {

        private var instance: ExoPlayer? = null

        var _musicData: String? = "          "

        fun getPlayerInstance(context: Context, url: String): ExoPlayer {
            return instance ?: synchronized(this){
                instance ?: buildPlayer(context, url).also{ instance = it}
            }
        }

        private fun buildPlayer(context: Context, url: String): ExoPlayer {
            return ExoPlayer.Builder(context).build().also { player ->
                val uri = Uri.parse(url)

                try {
                    val mediaItem: MediaItem = MediaItem.fromUri(uri)

                    player.setMediaItem(mediaItem)
                    player.playWhenReady = false
                    player.prepare()

                    true
                } catch (e: Exception) {
                    Log.d("Player initialize error", e.message.toString())
                    false
                }
            }
        }
    }
}