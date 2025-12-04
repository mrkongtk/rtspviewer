package com.mrkongtk.rtspviewer.data

import androidx.media3.common.PlaybackException

data class RTSPVideoPlayerState(
    val playback: RTSPVideoPlayerPlaybackState,
    val error: PlaybackException?
)
