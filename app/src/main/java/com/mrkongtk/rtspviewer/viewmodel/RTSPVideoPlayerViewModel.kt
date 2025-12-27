package com.mrkongtk.rtspviewer.viewmodel

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
 * @param initialUri The initial RTSP URI provided when the screen/component is created.
 * @param initialForceTcp The initial preference for forcing TCP transport.
 */
@HiltViewModel(assistedFactory = RTSPVideoPlayerViewModel.Factory::class)
class RTSPVideoPlayerViewModel @AssistedInject constructor(
    @Assisted initialUri: String?,
    @Assisted initialForceTcp: Boolean
) : ViewModel() {

    // Backing property for the UI state.
    private val _state = MutableStateFlow(
        RTSPVideoPlayerState(
            playback = RTSPVideoPlayerPlaybackState.Idle,
            error = null
        )
    )

    /**
     * Public immutable StateFlow representing the current UI state of the player.
     * The UI should collect this flow to update the interface (e.g., show loading spinners,
     * generic error messages, or the play/pause state).
     */
    val state: StateFlow<RTSPVideoPlayerState> = _state

    // Backing property for the current URI.
    private val _uri = MutableStateFlow(initialUri)

    // Backing property for the Force TCP preference.
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

    // Backing property for the video aspect ratio. Defaults to standard 16:9 (1.77).
    private val _videoAspectRatio = MutableStateFlow(16f / 9f)

    /**
     * StateFlow exposing the current video aspect ratio (width / height).
     * The UI can use this to resize the SurfaceView or TextureView to match the content.
     */
    val videoAspectRatio: StateFlow<Float> = _videoAspectRatio

    /**
     * Updates the current playback state (e.g., from Idle to Buffering to Ready).
     * Usually called by the ExoPlayer/Media3 listener callbacks.
     *
     * @param playback The new playback state enum.
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
     * Calculates and updates the video aspect ratio based on the provided dimensions.
     *
     * This method includes validation to ensure the calculated ratio is finite and positive.
     * If the dimensions are invalid (e.g., width or height is 0), it defaults to 16:9.
     *
     * @param width The width of the video track in pixels.
     * @param height The height of the video track in pixels.
     */
    fun updateVideoAspectRatio(width: Float, height: Float) {
        _videoAspectRatio.update {
            val result = width / height
            // Ensure we don't crash or create layout issues with Infinite or NaN values
            if (result.isFinite() && result > 0) {
                result
            } else {
                16f / 9f
            }
        }
    }

    /**
     * Data class holding the required configuration to start a stream.
     *
     * @property uri The RTSP address.
     * @property forceTcp Whether to enforce RTSP over TCP (interleaved) instead of UDP.
     */
    data class Data(val uri: String, val forceTcp: Boolean)

    /**
     * AssistedFactory interface required by Hilt.
     * This allows the creation of the [RTSPVideoPlayerViewModel] with runtime arguments
     * that are not known at compile time.
     */
    @AssistedFactory
    interface Factory {
        /**
         * Creates an instance of the ViewModel.
         *
         * @param initialUri The starting RTSP URI.
         * @param initialForceTcp The starting preference for TCP transport.
         */
        fun create(initialUri: String?, initialForceTcp: Boolean): RTSPVideoPlayerViewModel
    }
}
