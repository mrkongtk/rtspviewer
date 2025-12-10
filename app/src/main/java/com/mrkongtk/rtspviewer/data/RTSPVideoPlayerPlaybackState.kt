package com.mrkongtk.rtspviewer.data

/**
 * Represents the current playback lifecycle state of the RTSP video player.
 *
 * @property value The integer representation associated with the state.
 */
enum class RTSPVideoPlayerPlaybackState(val value: Int) {
    /** The player is idle, stopped, or has not been initialized yet. */
    Idle(1),

    /** The player is currently buffering data from the stream. */
    Buffering(2),

    /** The player has buffered enough data and is ready to play. */
    Ready(3),

    /** The stream has finished or playback has completed. */
    Ended(4),

    /** The video is currently playing. */
    Playing(10);

    companion object {
        /**
         * Finds the [RTSPVideoPlayerPlaybackState] corresponding to the given integer value.
         *
         * @param value The integer value to look up.
         * @return The matching state, or null if no state matches the value.
         */
        fun fromValue(value: Int): RTSPVideoPlayerPlaybackState? {
            return RTSPVideoPlayerPlaybackState.entries.firstOrNull { it.value == value }
        }
    }
}
