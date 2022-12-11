package com.blinked.radiosamui

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import kotlin.random.Random

const val MSG_REGISTER_CLIENT: Int = 1
const val MSG_UNREGISTER_CLIENT: Int = 2

const val MSG_REPORT_STATE: Int = 10
const val MSG_REPORT_METADATA: Int = 11
const val MSG_ANSWER_STATE: Int = 1010
const val MSG_STATE_IS_PLAYING: Int = 1101
const val MSG_STATE_IS_PAUSED: Int = 1102
const val MSG_ANSWER_METADATA: Int = 1011

const val MSG_DO_PLAY: Int = 20
const val MSG_DO_PAUSE: Int = 21
const val MSG_DO_STOP: Int = 22
const val MSG_DO_CLEAR_MEDIA: Int = 23
const val MSG_DO_RELEASE_PLAYER: Int = 24



const val MSG_SET_SAMUI_RADIO: Int = 101
const val MSG_SET_DANCE_RADIO: Int = 102
const val MSG_SET_JAZZ_RADIO: Int = 103
const val MSG_SET_RELAX_RADIO: Int = 104
const val MSG_SET_FITNESS_RADIO: Int = 105



const val SAMUI_MUSIC_URL: String = "https://stream.mixadance.fm/radiosamui"
const val MIXADANCE_URL: String = "https://stream.mixadance.fm/mixadance"
const val RELAX_URL: String = "https://stream.mixadance.fm/relax#.m4a"
const val FITNESS_URL: String = "https://stream.mixadance.fm/fitness"
const val JAZZ_URL: String = "https://stream.mixadance.fm/mixadancejazz"


class MixadancePlaybackService: Service() {
    private lateinit var mThread : HandlerThread
    lateinit private var serviceHandler: MixadanceServiceWorkHandler
    lateinit private var mMessenger: Messenger
    private lateinit var mNM: NotificationManager

    override fun onCreate() {
        serviceHandler = MixadanceServiceWorkHandler(this@MixadancePlaybackService)

        mMessenger = Messenger(serviceHandler)
        serviceHandler.preparePlayer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Radio Samui notifications"
            val descText = "Radio Samui music service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descText
            }

            mNM = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNM.createNotificationChannel(channel)
        }

        showNotificationForeground()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showNotificationForeground() {
        val text: String = "Radio Samui playback service"
        val contentIntent: PendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).also {
            it.setSmallIcon(R.drawable.radio_logo)
            it.setTicker(text)
            it.setWhen(System.currentTimeMillis())
            it.setContentTitle("Radio Samui music, background playback")
            //it.setContentIntent(contentIntent)
            it.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }.build()

        startForeground(Random.nextInt(100_000), notification)
    }

    companion private object {
        const val CHANNEL_ID = "remote_radio_samui_channel"
    }
}


// will run in separate background thread
class MixadanceServiceWorkHandler(serviceContext: Context) : Handler() {
    private val mClients: ArrayList<Messenger> = arrayListOf()
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(serviceContext).build()
        .also { player -> player.addListener(MixadanceDataAndErrorObject()) }


    private var mErrorCode: String = ""

    private var mMetaData: String = ""
        get() = field
        set(value) {
            field = value
            sendMetaDataToAllClients()
        }

    private var mIsPlaying: Boolean = false
        set(value) {
            field = value
            sendStateToAllClients()
        }


    // Receive incoming messages
    override fun handleMessage(msg: Message) {
        when(msg.what){
            MSG_REGISTER_CLIENT -> {
                mClients.add(msg.replyTo)
            }
            MSG_UNREGISTER_CLIENT -> {
                mClients.remove(msg.replyTo)
            }
            MSG_REPORT_STATE -> {
                sendStateToAllClients()
            }
            MSG_REPORT_METADATA -> {
                sendMetaDataToAllClients()
            }
            MSG_DO_PLAY -> {
                exoPlayer.play()
            }
            MSG_DO_PAUSE -> {
                exoPlayer.pause()
            }
            MSG_DO_STOP -> {
                exoPlayer.stop()
            }
            MSG_DO_RELEASE_PLAYER -> {
                exoPlayer.release()
            }
            MSG_DO_CLEAR_MEDIA -> {
                exoPlayer.clearMediaItems()
            }
            MSG_SET_SAMUI_RADIO -> {
                setRadio(SAMUI_MUSIC_URL)
            }
            MSG_SET_DANCE_RADIO -> {
                setRadio(MIXADANCE_URL)
            }
            MSG_SET_FITNESS_RADIO -> {
                setRadio(FITNESS_URL)
            }
            MSG_SET_JAZZ_RADIO -> {
                setRadio(JAZZ_URL)
            }
            MSG_SET_RELAX_RADIO -> {
                setRadio(RELAX_URL)
            }
        }

    }

    private fun setRadio(url: String){
        val mediaItem: MediaItem = MediaItem.fromUri(url)
        try {
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.playWhenReady = false
            exoPlayer.prepare()
        } catch (e: Exception) {
            Log.d("Player set Radio error, ", e.message.toString())
        }
    }

    // send media metadata to all clients of the service
    private fun sendMetaDataToAllClients() {
        val message: Message = Message.obtain(null, MSG_ANSWER_METADATA, 0, 0).also {
            it.obj = mMetaData
        }
        mClients.forEach{ client ->
            client.send(message)
        }
    }

    //send state to all clients of the service
    private fun sendStateToAllClients() {
        val currentState = if ( exoPlayer.isPlaying ) MSG_STATE_IS_PLAYING else MSG_STATE_IS_PAUSED
        val message: Message = Message.obtain(null, MSG_ANSWER_STATE, currentState, 0)
        mClients.forEach{ client ->
            client.send(message)
        }
    }


    //
    // <<<<<<<<<<<<<<<<<   Player methods >>>>>>>>>>>>>>>>>>
    //
    fun preparePlayer() {
        try {
            exoPlayer.playWhenReady = false
            exoPlayer.setWakeMode(C.WAKE_MODE_LOCAL or C.WAKE_MODE_NETWORK)
            exoPlayer.prepare()
        } catch (e: Exception) {
            Log.d("Player initialize error, ", e.message.toString())
        }
    }

    // Object that listens to errors and media events from the Exo Player
    inner class MixadanceDataAndErrorObject : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            mErrorCode = PlaybackException.getErrorCodeName(error.errorCode)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            mMetaData = with(mediaMetadata) {
                val resultString: String =
                    if (!title.isNullOrEmpty()){ title.toString() }
                    else if (!station.isNullOrEmpty()) { station.toString() }
                    else { "" }

                resultString
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mIsPlaying = isPlaying
        }
    }



    // PLayer instance creator
    private companion object {
        private var instance: ExoPlayer? = null

        var _musicData: String? = "          "

        // Creates an instance of Exo Player
        fun getPlayerInstance(context: Context): ExoPlayer {
            return instance ?: buildPlayer(context).also{ instance = it}
        }

        private fun buildPlayer(context: Context): ExoPlayer {
            return ExoPlayer.Builder(context).build()
        }
    }
}