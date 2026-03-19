package com.mrkongtk.rtspviewer.shared.player

import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the contract for an RTSP video player implementation.
 * It provides stream control and exposes the player's state and video properties via [StateFlow].
 */
interface RTSPVideoPlayer {
    /**
     * A [StateFlow] representing the current [RTSPVideoPlayerPlaybackState] of the player.
     */
    val currentState: StateFlow<RTSPVideoPlayerPlaybackState>

    /**
     * A [StateFlow] representing the aspect ratio (width / height) of the video being played.
     * Defaults to 0.0f or a predefined value until the video dimensions are known.
     */
    val videoAspectRatio: StateFlow<Float>

    /**
     * A [StateFlow] containing the last error that occurred during playback, or null if no error exists.
     */
    val error: StateFlow<Throwable?>

    /**
     * Prepares the player for playback of the specified RTSP URI.
     *
     * @param uri The RTSP stream URI to be played.
     * @param forceTcp Whether to force the use of TCP for the RTSP connection.
     */
    fun prepare(uri: String, forceTcp: Boolean)

    /**
     * Starts or resumes video playback.
     */
    fun play()

    /**
     * Stops video playback.
     */
    fun stop()

    /**
     * Releases the player and any resources it holds. Once released, the player instance
     * should not be used again.
     */
    fun release()

    /**
     * Provides access to the underlying platform-specific player instance.
     *
     * @param T The expected type of the underlying player (e.g., ExoPlayer on Android).
     * @return The platform-specific player instance, or null if it cannot be cast to [T].
     */
    fun <T> getPlayer(): T?
}
