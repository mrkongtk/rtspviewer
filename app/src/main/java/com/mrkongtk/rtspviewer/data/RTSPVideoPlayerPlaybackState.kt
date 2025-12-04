package com.mrkongtk.rtspviewer.data

enum class RTSPVideoPlayerPlaybackState(val value: Int) {
    Idle(1),
    Buffering(2),
    Ready(3),
    Ended(4),
    ;

    companion object {
        fun fromValue(value: Int): RTSPVideoPlayerPlaybackState? {
            return RTSPVideoPlayerPlaybackState.entries.firstOrNull { it.value == value }
        }
    }
}
