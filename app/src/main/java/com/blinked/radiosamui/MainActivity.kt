package com.blinked.radiosamui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaDataSource
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

    private val viewModel: RadioViewModel by viewModels()

    private var internetPermission: Boolean = false

    private var marqeeOn: Boolean = true

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
            if (isGranted){
                internetPermission = true
                _layout.showSnackBar(_binding.playButtonId, "< PLAY MUSIC NOW >", Snackbar.LENGTH_SHORT, null){}
            }else {
                internetPermission = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(_binding.root)
        _layout = _binding.mainLayout
        supportActionBar?.hide()


        showActionSymbolToDo()

        checkRequestPermission(_layout)



        _binding.playButtonId.setOnClickListener { buttonView ->
            if (viewModel.isPlayingNow()){
                viewModel.Pause()
            }else if (internetPermission) {
                viewModel.Play()
            } else {
                _layout.showSnackBar(_binding.playButtonId, "Internet Permission and Connection are required!", Snackbar.LENGTH_LONG, null){}
            }
            showActionSymbolToDo()
        }

        viewModel.metaData.observe(this){ musicData ->
            if (musicData == null) return@observe
            _binding.textMusicData.text = musicData
            _binding.textMusicData.isSelected = marqeeOn
        }

        _binding.textMusicData.setOnClickListener { view ->
            marqeeOn = !marqeeOn
            view.isSelected = marqeeOn
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
        if (!viewModel.isPlayingNow()){
            viewModel.Pause()
            //viewModel.ReleasePlayer()
        }
        viewModel.saveMusicData()
        super.onDestroy()
    }

    private fun checkRequestPermission(view: View){
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            == PackageManager.PERMISSION_GRANTED -> {
                internetPermission = true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET) -> {
                _layout.showSnackBar(view,
                    "Internet permission is required for the App to play stream music from Internet radio!",
                    Snackbar.LENGTH_INDEFINITE, "OK"){
                    requestPermissionLauncher.launch(Manifest.permission.INTERNET)
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.INTERNET)
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