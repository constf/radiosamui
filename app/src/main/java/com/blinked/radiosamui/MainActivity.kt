package com.blinked.radiosamui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaDataSource
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaPeriodId
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MetadataRetriever
import com.blinked.radiosamui.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar



class MainActivity : AppCompatActivity() {
    private lateinit var _binding : ActivityMainBinding
    private lateinit var _layout: View

    private var playerService: Messenger? = null
    private var boundToService: Boolean = false

    private var incomingMessenger: Messenger? = null

    private val viewModel: RadioViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(_binding.root)
        _layout = _binding.mainLayout
        supportActionBar?.hide()

        val startService: Intent = Intent(this, MixadancePlaybackService::class.java)
        bindService(startService, serviceConnection, Context.BIND_AUTO_CREATE)

        showActionSymbolToDo()

        viewModel.requestState()

        viewModel.setRadio(MSG_SET_SAMUI_RADIO)

        _binding.textMusicData.isSelected = true

        _binding.playButtonId.setOnClickListener { buttonView ->
            if (viewModel.isPlayingNow()){
                viewModel.Pause()
            } else {
                viewModel.Play()
            }
            showActionSymbolToDo()
        }

        viewModel.metaData.observe(this){ musicData ->
            if (musicData.isNullOrEmpty()) return@observe

            _binding.textMusicData.text =
                musicData + "    " + musicData + "   " + musicData + "    " + musicData + "   " + musicData + "    " + musicData + "   "+ musicData + "    " + musicData + "   " + musicData + "    " + musicData + "   "
        }

        _binding.textMusicData.setOnClickListener { view ->
            view.isSelected = ! view.isSelected
        }

        viewModel.isPlaying.observe(this) { isPlaying ->
            showActionSymbolToDo()
        }
    }

    private fun showActionSymbolToDo() {
        if (viewModel.isPlayingNow()){
            _binding.playButtonId.setImageResource(R.drawable.pause_button)
        } else {
            _binding.playButtonId.setImageResource(R.drawable.play_button)
        }
    }

    override fun onDestroy() {
        viewModel.stopAndRelease()
        unbindService(serviceConnection)
        super.onDestroy()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = Messenger(service)
            viewModel.setServiceMessenger(playerService)
            boundToService = true

            if (incomingMessenger == null) {
                incomingMessenger = Messenger(mixadanceClient)
            }

            val msg: Message = Message.obtain(null, MSG_REGISTER_CLIENT, 0, 0).also { message ->
                message.replyTo = incomingMessenger
            }
            playerService?.send(msg)

            viewModel.setRadio(MSG_SET_SAMUI_RADIO)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            incomingMessenger = null
            playerService = null
            viewModel.setServiceMessenger(null)
            boundToService = false
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            incomingMessenger = null
            playerService = null
            viewModel.setServiceMessenger(null)
            boundToService = false
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
        }
    }

    private val mixadanceClient = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_ANSWER_METADATA -> {
                    viewModel.receiveMetaData(msg.obj as String ?: "")
                }
                MSG_ANSWER_STATE -> {
                    viewModel.receiveState( msg.arg1 == MSG_STATE_IS_PLAYING )
                }
                else -> {
                    super.handleMessage(msg)
                }
            }
        }
    }
}

fun View.showSnackBar(view: View, message: String, length: Int, actionMessage: CharSequence?, action: (View)->Unit) {
    val snackBar = Snackbar.make(view, message, length)
    if (actionMessage != null){
        snackBar.setAction(actionMessage){action(this)}.show()
    } else {
        snackBar.show()
    }
}