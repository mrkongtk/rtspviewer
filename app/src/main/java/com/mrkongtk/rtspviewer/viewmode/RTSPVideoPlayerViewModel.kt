package com.mrkongtk.rtspviewer.viewmode

import androidx.lifecycle.ViewModel
import androidx.media3.common.PlaybackException
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * ViewModel responsible for managing the state and configuration of the RTSP Video Player.
 *
 * This ViewModel uses Hilt's Assisted Injection to accept dynamic runtime arguments
 * (URI and TCP preference) while still participating in the Hilt dependency graph.
 *
 * @property initialUri The initial RTSP URI provided when the screen/component is created.
 * @property initialForceTcp The initial preference for forcing TCP transport.
 */
@HiltViewModel(assistedFactory = RTSPVideoPlayerViewModel.Factory::class)
class RTSPVideoPlayerViewModel @AssistedInject constructor(
    @Assisted initialUri: String?,
    @Assisted initialForceTcp: Boolean
) : ViewModel() {

    // Internal mutable state flow for the player's UI state (buffering, idle, errors, etc.)
    private val _state = MutableStateFlow(
        RTSPVideoPlayerState(
            playback = RTSPVideoPlayerPlaybackState.Idle,
            error = null
        )
    )

    /**
     * Public immutable StateFlow representing the current UI state of the player.
     * The UI should collect this flow to update the interface (e.g., show loading spinners or error messages).
     */
    val state: StateFlow<RTSPVideoPlayerState> = _state

    // Internal flow to track the current RTSP URI
    private val _uri = MutableStateFlow(initialUri)

    // Internal flow to track the "Force TCP" preference
    private val _forceTcp = MutableStateFlow(initialForceTcp)

    /**
     * A combined flow of configuration data.
     *
     * This flow emits a new [Data] object whenever the URI or the ForceTCP setting changes.
     * It allows the player implementation to reactively restart or reconfigure the stream
     * when these parameters change.
     *
     * Note: This will only emit if the URI is not null.
     */
    val data = combine(_uri, _forceTcp) { uri, forceTcp ->
        uri?.let {
            Data(uri = uri, forceTcp = forceTcp)
        }
    }

    /**
     * Updates the current playback state (e.g., from Idle to Buffering to Ready).
     *
     * @param playback The new playback state from the media player.
     */
    fun updatePlaybackState(playback: RTSPVideoPlayerPlaybackState) {
        _state.update {
            it.copy(playback = playback)
        }
    }

    /**
     * Updates the state with a playback exception.
     *
     * @param error The exception thrown by ExoPlayer/Media3, or null to clear the error.
     */
    fun updatePlaybackError(error: PlaybackException?) {
        _state.update {
            it.copy(error = error)
        }
    }

    /**
     * Data class holding the required configuration to start a stream.
     */
    data class Data(val uri: String, val forceTcp: Boolean)

    /**
     * AssistedFactory interface required by Hilt.
     * This allows the creation of the [RTSPVideoPlayerViewModel] with runtime arguments
     * that are not known at compile time (the URI and Boolean flag).
     */
    @AssistedFactory
    interface Factory {
        fun create(initialUri: String?, initialForceTcp: Boolean): RTSPVideoPlayerViewModel
    }
}
