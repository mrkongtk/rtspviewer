package com.mrkongtk.rtspviewer.data

data class RTSPVideoPlayerState(
    val playback: RTSPVideoPlayerPlaybackState,
    val error: Throwable?
)
