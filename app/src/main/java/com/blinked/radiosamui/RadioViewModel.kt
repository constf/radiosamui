package com.blinked.radiosamui

import android.app.Application
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.*
import com.blinked.radiosamui.MSG_DO_CLEAR_MEDIA
import com.blinked.radiosamui.MSG_DO_PAUSE
import com.blinked.radiosamui.MSG_DO_PLAY
import com.blinked.radiosamui.MSG_DO_STOP


class RadioViewModel(private val inApplication: Application): AndroidViewModel(inApplication) {

    private var playerService: Messenger? = null

    private var _errorCode: MutableLiveData<String> = MutableLiveData(null)
    val errorCode: LiveData<String> get() = _errorCode

    private val _metaData: MutableLiveData<String> = MutableLiveData("")
    val metaData: LiveData<String> get() = _metaData

    private val _isPLaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPLaying

    fun isPlayingNow(): Boolean {
        return isPlaying.value!!
    }

    fun clearMusicData() {
        _metaData.value = "   "
    }

    fun setRadio(radioToSet: Int) {
        val message: Message = Message.obtain(null, radioToSet, 0, 0)
        playerService?.send(message)
    }

    fun Play() {
        val message: Message = Message.obtain(null, MSG_DO_PLAY, 0, 0)
        playerService?.send(message)
    }

    fun Pause() {
        val message: Message = Message.obtain(null, MSG_DO_PAUSE, 0, 0)
        playerService?.send(message)
    }

    fun stopAndRelease() {
        var message: Message = Message.obtain(null, MSG_DO_STOP, 0, 0)
        playerService?.send(message)

        message = Message.obtain(null, MSG_DO_RELEASE_PLAYER, 0, 0)
        playerService?.send(message)

    }

    fun clearMedia() {
        val message: Message = Message.obtain(null, MSG_DO_CLEAR_MEDIA, 0, 0)
        playerService?.send(message)
    }

    fun requestState() {
        val message: Message = Message.obtain(null, MSG_REPORT_STATE, 0, 0)
        playerService?.send(message)
    }


    fun receiveMetaData(str: String) {
        _metaData.value = str
    }

    fun receiveState(state: Boolean) {
        _isPLaying.value = state
    }

    fun setServiceMessenger(service: Messenger?) {
        playerService = service
    }



}
