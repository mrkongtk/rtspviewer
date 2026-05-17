package com.mrkongtk.rtspviewer.shared.player

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of [RTSPVideoPlayer] that uses Jetpack Media3 [ExoPlayer] 
 * to handle RTSP streaming.
 *
 * This class manages the lifecycle of an [ExoPlayer] instance and maps its internal 
 * states to the common [RTSPVideoPlayerPlaybackState].
 *
 * @param context The Android context used to build the [ExoPlayer] instance.
 */
class ExoVideoPlayer(context: Context) : RTSPVideoPlayer {

    // Attach listeners to react to player events
    private val listener = object : Player.Listener {
        /**
         * Called when the player encounters a fatal error (e.g., connection lost, malformed RTSP).
         * We propagate this to the UI to show an error message.
         */
        override fun onPlayerErrorChanged(error: PlaybackException?) {
            super.onPlayerErrorChanged(error)
            _error.value = error
        }

        /**
         * Called when the internal buffer/ready state changes.
         * We map the raw ExoPlayer integer constants to our domain-specific
         * [RTSPVideoPlayerPlaybackState] enum for easier UI consumption.
         *
         * @param playbackState The integer constant from [Player].
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
                return
            }
            val state = when (playbackState) {
                Player.STATE_IDLE -> RTSPVideoPlayerPlaybackState.Idle
                Player.STATE_READY -> RTSPVideoPlayerPlaybackState.Ready
                Player.STATE_BUFFERING -> RTSPVideoPlayerPlaybackState.Buffering
                Player.STATE_ENDED -> RTSPVideoPlayerPlaybackState.Ended
                else -> null
            }
            state?.let {
                _currentState.value = it
            } ?: run {
                Log.e("RTSPVideoPlayer", "Unknown ExoPlayer state: $playbackState")
            }
        }

        /**
         * Syncs the "Is Playing" boolean with the state.
         *
         * Note: `STATE_READY` in ExoPlayer does not imply the video is moving; it just means
         * it has buffered enough to start. We use this method to distinguish between
         * "Ready but Paused" and "Ready and Playing".
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
                return
            }
            if (isPlaying) {
                _currentState.value = RTSPVideoPlayerPlaybackState.Playing
            } else if (_currentState.value == RTSPVideoPlayerPlaybackState.Playing) {
                _currentState.value = RTSPVideoPlayerPlaybackState.Ready
            }
        }

        /**
         * Called when the video resolution is determined from the stream metadata.
         * Updates the aspect ratio state to trigger a UI layout pass.
         */
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            val width = videoSize.width.toFloat()
            val height = videoSize.height.toFloat()
            val result = width / height
            _aspectRatio.value = if (result.isFinite() && result > 0) {
                result
            } else {
                16f / 9f
            }
        }
    }

    private val _exoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(listener)
    }

    private val _currentState = MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
    override val currentState = _currentState.asStateFlow()

    private val _aspectRatio = MutableStateFlow(16f / 9f)
    override val videoAspectRatio = _aspectRatio.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    override val error = _error.asStateFlow()

    /**
     * Prepares the [ExoPlayer] with an [RtspMediaSource].
     *
     * @param uri The RTSP stream URI.
     * @param forceTcp Whether to force RTP over TCP (Interleaved).
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    override fun prepare(uri: String, forceTcp: Boolean) {
        if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
            return
        }
        try {
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(forceTcp)
                .createMediaSource(MediaItem.fromUri(uri))

            _exoPlayer.setMediaSource(mediaSource)
            _exoPlayer.prepare()
            _exoPlayer.playWhenReady = true
        } catch (error: IllegalStateException) {
            _error.value = error
        }
    }

    /**
     * Resumes playback or prepares the player if it's in an idle state.
     */
    override fun play() {
        if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
            return
        }
        _exoPlayer.playWhenReady = true
        when (_currentState.value) {
            RTSPVideoPlayerPlaybackState.Ready -> _exoPlayer.play()
            RTSPVideoPlayerPlaybackState.Buffering,
            RTSPVideoPlayerPlaybackState.Playing -> { /* Already active, do nothing */
            }

            else -> _exoPlayer.prepare()
        }
    }

    /**
     * Stops the [ExoPlayer] and disables playWhenReady.
     */
    override fun stop() {
        if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
            return
        }
        _exoPlayer.playWhenReady = false
        _exoPlayer.stop()
    }

    /**
     * Releases the [ExoPlayer] resources.
     */
    override fun release() {
        if (_currentState.value == RTSPVideoPlayerPlaybackState.Released) {
            return
        }
        _currentState.value = RTSPVideoPlayerPlaybackState.Released
        _exoPlayer.playWhenReady = false
        _exoPlayer.stop()
        _exoPlayer.release()
    }

    /**
     * Returns the underlying [ExoPlayer] instance.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlayer(): T? {
        return _exoPlayer as? T
    }

}
