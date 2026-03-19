package com.mrkongtk.rtspviewer.shared.ui.player

data class RTSPVideoPlayerState(
    val playback: RTSPVideoPlayerPlaybackState,
    val error: Throwable?
)
