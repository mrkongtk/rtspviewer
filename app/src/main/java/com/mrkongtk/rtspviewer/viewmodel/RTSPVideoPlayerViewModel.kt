package com.mrkongtk.rtspviewer.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel responsible for managing the state, configuration, and lifecycle of the RTSP Video Player.
 *
 * This class acts as the bridge between the UI (Compose/XML) and the Media3 ExoPlayer instance.
 * It handles the initialization of the player, processes RTSP streams, and exposes reactive
 * state flows for UI updates.
 *
 * **Dependency Injection:**
 * This ViewModel uses Hilt's [AssistedInject] to combine compile-time dependencies (Context)
 * with runtime arguments (URI and TCP preference) provided by the navigation or fragment arguments.
 *
 * @property context The application context used to build the ExoPlayer instance.
 * @property initialUri The initial RTSP URI provided when the screen is created.
 * @property initialForceTcp The initial user preference for forcing TCP transport.
 */
@HiltViewModel(assistedFactory = RTSPVideoPlayerViewModel.Factory::class)
class RTSPVideoPlayerViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    @Assisted private val initialUri: String?,
    @Assisted private val initialForceTcp: Boolean
) : ViewModel() {

    /**
     * The Media3 ExoPlayer instance.
     * Use nullable to safely handle initialization failures (e.g., missing codecs or context issues).
     */
    val exoPlayer: ExoPlayer?

    // Backing property for the UI state.
    private val _state = MutableStateFlow(
        RTSPVideoPlayerState(
            playback = RTSPVideoPlayerPlaybackState.Idle,
            error = null
        )
    )

    /**
     * Public immutable StateFlow representing the current UI state of the player.
     *
     * The UI observes this flow to react to changes, such as:
     * - Displaying a loading spinner ([RTSPVideoPlayerPlaybackState.Buffering])
     * - Showing the Play/Pause button ([RTSPVideoPlayerPlaybackState.Ready])
     * - Displaying error snackbars or dialogs ([RTSPVideoPlayerState.error])
     */
    val state: StateFlow<RTSPVideoPlayerState> = _state

    /**
     * Internal holder for the current stream configuration (URI and TCP mode).
     */
    private val _data = MutableStateFlow<Data?>(null)
    internal val data: StateFlow<Data?> = _data

    // Backing property for the video aspect ratio. Defaults to standard 16:9 (1.77).
    private val _videoAspectRatio = MutableStateFlow(16f / 9f)

    /**
     * StateFlow exposing the current video aspect ratio (width / height).
     *
     * The UI uses this to resize the playback surface (SurfaceView/TextureView) to prevent
     * stretching or black bars (letterboxing) when the stream resolution changes.
     */
    val videoAspectRatio: StateFlow<Float> = _videoAspectRatio

    init {
        // Initialize ExoPlayer safely
        exoPlayer = try {
            ExoPlayer.Builder(context).build()
        } catch (e: Throwable) {
            // If ExoPlayer initialization fails, update the state with the error and set exoPlayer to null.
            updateError(e)
            null
        }

        // Attach listeners to react to player events
        val listener = object : Player.Listener {
            /**
             * Called when the player encounters a fatal error (e.g., connection lost, malformed RTSP).
             * We propagate this to the UI to show an error message.
             */
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                // Update the error state in the ViewModel.
                updateError(error)
            }

            /**
             * Called when the internal buffer/ready state changes.
             * We map the raw ExoPlayer integer constants to our domain-specific
             * [RTSPVideoPlayerPlaybackState] enum for easier UI consumption.
             */
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                // Convert the ExoPlayer playback state to our custom enum.
                RTSPVideoPlayerPlaybackState.fromValue(playbackState)?.let {
                    updatePlaybackState(it)
                } ?: run {
                    // Log an error if an unknown playback state is encountered.
                    Log.e("RTSPVideoPlayer", "Unknown ExoPlayer state: $playbackState")
                }
            }

            /**
             * Syncs the "Is Playing" boolean with the ViewModel state.
             *
             * Note: `STATE_READY` in ExoPlayer does not imply the video is moving; it just means
             * it has buffered enough to start. We use this method to distinguish between
             * "Ready but Paused" and "Ready and Playing".
             */
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                // Update the playback state based on whether the player is currently playing.
                if (isPlaying) {
                    updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
                } else {
                    updatePlaybackState(RTSPVideoPlayerPlaybackState.Ready)
                }
            }

            /**
             * Called when the video resolution is determined from the stream metadata.
             * Updates the aspect ratio state to trigger a UI layout pass.
             */
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                // Update the video aspect ratio based on the new video dimensions.
                updateVideoAspectRatio(
                    videoSize.width.toFloat(),
                    videoSize.height.toFloat()
                )
            }
        }
        // Add the listener to the ExoPlayer instance.
        exoPlayer?.addListener(listener)

        // If a URI was passed via Assisted Injection, start loading immediately.
        initialUri?.let { uri ->
            // Set the initial data (URI and forceTcp preference) and prepare the player.
            updateData(uri, initialForceTcp)
        }
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     * Stops the player and releases codecs to free up system memory.
     */
    override fun onCleared() {
        super.onCleared()
        // Stop and release the ExoPlayer when the ViewModel is no longer needed.
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
    }

    /**
     * Updates the stream configuration and prepares the player.
     *
     * This function handles partial updates (e.g., changing only the TCP preference while keeping the URI).
     *
     * @param uri The new RTSP URI (optional).
     * @param forceTcp The new TCP preference (optional).
     */
    fun updateData(uri: String? = null, forceTcp: Boolean? = null) {
        _data.update { oldData ->
            // Determine the new data based on provided parameters, merging with old data if necessary.
            val newData = uri?.let { newUri ->
                forceTcp?.let { newForceTcp ->
                    // If both URI and forceTcp are provided, create new Data object.
                    Data(uri = newUri, forceTcp = newForceTcp)
                } ?: oldData?.copy(uri = newUri) ?: Data(
                    uri = newUri,
                    forceTcp = false
                ) // If only URI is provided, update URI, default forceTcp to false if no oldData.
            } ?: run {
                forceTcp?.let {
                    // If only forceTcp is provided, update forceTcp in oldData.
                    oldData?.copy(forceTcp = it)
                } ?: oldData // If neither is provided, keep the old data.
            }

            // If we have valid data, configure and prepare the MediaSource
            newData?.let { data ->
                exoPlayer?.let { player ->
                    val mediaSource = RtspMediaSource.Factory()
                        // Critical for RTSP: Forces RTP over TCP (interleaved) if configured.
                        // This is required for viewing streams over the internet/WAN or through firewalls
                        // where UDP packets are often dropped, causing artifacts or connection failures.
                        .setForceUseRtpTcp(data.forceTcp)
                        .createMediaSource(MediaItem.fromUri(data.uri))

                    // Set the media source and prepare the player.
                    player.setMediaSource(mediaSource)
                    player.playWhenReady = true // Start playback automatically when ready.
                    player.prepare()
                }
            }
            newData
        }
    }

    /**
     * Updates the current playback state enum in the UI state flow.
     */
    internal fun updatePlaybackState(playback: RTSPVideoPlayerPlaybackState) {
        // Update the playback state within the _state flow.
        _state.update {
            it.copy(playback = playback)
        }
    }

    /**
     * Updates the error state in the UI state flow.
     * @param error The exception thrown, or null to clear the error.
     */
    internal fun updateError(error: Throwable?) {
        // Update the error field in the _state flow.
        _state.update {
            it.copy(error = error)
        }
    }

    /**
     * Calculates and updates the video aspect ratio safely.
     *
     * Prevents layout crashes by checking for Infinite or NaN results (e.g., if height is 0).
     * Defaults to 16:9 if dimensions are invalid.
     */
    internal fun updateVideoAspectRatio(width: Float, height: Float) {
        _videoAspectRatio.update {
            // Calculate the aspect ratio.
            val result = width / height
            // Update the aspect ratio only if it's a finite positive number, otherwise default to 16:9.
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
    internal data class Data(val uri: String, val forceTcp: Boolean)

    /**
     * AssistedFactory interface required by Hilt.
     *
     * This allows the creation of [RTSPVideoPlayerViewModel] with runtime arguments
     * that are not known at compile time (the URI and TCP setting).
     */
    @AssistedFactory
    interface Factory {
        fun create(initialUri: String?, initialForceTcp: Boolean): RTSPVideoPlayerViewModel
    }
}
