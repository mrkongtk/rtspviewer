package com.mrkongtk.rtspviewer.shared.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * ViewModel responsible for managing the state, configuration, and lifecycle of an RTSP Video Player.
 *
 * This class acts as a bridge between the UI and an [RTSPVideoPlayer] implementation.
 * It handles player initialization, stream configuration updates, and exposes reactive state flows
 * for the UI to observe and react to playback changes.
 *
 * @property videoPlayer The platform-specific implementation of the RTSP player.
 * @property initialUri The initial RTSP URI to load, if any.
 * @property initialForceTcp Whether to force the use of TCP for the initial stream.
 * @property onImageAvailable Optional callback for processing captured video frames as [ImageBitmap].
 */
open class RTSPVideoPlayerViewModel(
    val videoPlayer: RTSPVideoPlayer,
    private val initialUri: String?,
    private val initialForceTcp: Boolean,
    private val onImageAvailable: ((ImageBitmap) -> Unit)?,
) : ViewModel() {

    /**
     * A [StateFlow] representing the combined UI state of the player, including playback state and errors.
     *
     * The UI should observe this flow to:
     * - Show/hide loading indicators (based on [RTSPVideoPlayerPlaybackState.Buffering]).
     * - Display error messages (based on [RTSPVideoPlayerState.error]).
     * - Update playback controls.
     */
    val state: StateFlow<RTSPVideoPlayerState> =
        combine(videoPlayer.currentState, videoPlayer.error) { playback, error ->
            RTSPVideoPlayerState(playback, error)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RTSPVideoPlayerState(RTSPVideoPlayerPlaybackState.Idle, null)
        )


    /**
     * Internal state for the current stream configuration.
     */
    private val _data = MutableStateFlow<Data?>(null)
    internal val data: StateFlow<Data?> = _data

    /**
     * A [StateFlow] exposing the current video aspect ratio (width / height).
     *
     * The UI uses this value to correctly size the video surface and prevent image stretching.
     */
    val videoAspectRatio: StateFlow<Float> = videoPlayer.videoAspectRatio

    init {
        // Start playback if an initial URI is provided.
        updateData(initialUri, initialForceTcp)
    }

    /**
     * Releases player resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        videoPlayer.release()
    }

    /**
     * Updates the stream configuration and prepares the player with new settings.
     *
     * @param uri The new RTSP URI. If null, the existing URI is kept.
     * @param forceTcp Whether to use TCP transport. If null, the existing preference is kept.
     */
    fun updateData(uri: String? = null, forceTcp: Boolean? = null) {
        _data.update { oldData ->
            val newUri = uri ?: oldData?.uri
            val newForceTcp = forceTcp ?: oldData?.forceTcp ?: false
            val newData = newUri?.let {
                Data(uri = it, forceTcp = newForceTcp)
            }

            newData?.let { data ->
                videoPlayer.prepare(data.uri, data.forceTcp)
            }
            newData
        }
    }

    /**
     * Resumes or starts video playback.
     */
    fun playVideo() {
        videoPlayer.play()
    }

    /**
     * Stops video playback.
     */
    fun stopVideo() {
        videoPlayer.stop()
    }

    /**
     * Handles a captured video frame and passes it to the [onImageAvailable] callback.
     *
     * @param bitmap The [ImageBitmap] representing the current frame.
     */
    fun imageAvailable(bitmap: ImageBitmap) {
        onImageAvailable?.invoke(bitmap)
    }

    /**
     * Internal data class for storing RTSP stream configuration.
     *
     * @property uri The RTSP stream URI.
     * @property forceTcp Boolean indicating if TCP transport is required.
     */
    internal data class Data(val uri: String, val forceTcp: Boolean)

}
