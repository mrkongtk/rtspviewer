package com.mrkongtk.rtspviewer.viewmode

import androidx.lifecycle.ViewModel
import androidx.media3.common.PlaybackException
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class RTSPVideoPlayerViewModel @Inject constructor(
) : ViewModel() {
    private val _state = MutableStateFlow(
        RTSPVideoPlayerState(
            playback = RTSPVideoPlayerPlaybackState.Idle,
            error = null
        )
    )

    val state: StateFlow<RTSPVideoPlayerState> = _state

    fun updatePlaybackState(playback: RTSPVideoPlayerPlaybackState) {
        _state.update {
            it.copy(playback = playback)
        }
    }

    fun updatePlaybackError(error: PlaybackException?) {
        _state.update {
            it.copy(error = error)
        }
    }
}