package com.mrkongtk.rtspviewer.shared.ui.player

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
    Playing(5),

    /** The player is released and no longer available. */
    Released(100);
}
