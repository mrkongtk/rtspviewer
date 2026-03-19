package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer

@Composable
actual fun RTSPVideoPlayerDisplay(
    modifier: Modifier,
    player: RTSPVideoPlayer,
    onImageAvailable: (ImageBitmap) -> Unit,
) {

}