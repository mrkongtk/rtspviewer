package com.mrkongtk.rtspviewer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RTSPVideoPlayerPlaybackStateTest {

    @Test
    fun `fromValue returns correct state for valid integers`() {
        assertEquals(RTSPVideoPlayerPlaybackState.Idle, RTSPVideoPlayerPlaybackState.fromValue(1))
        assertEquals(
            RTSPVideoPlayerPlaybackState.Buffering,
            RTSPVideoPlayerPlaybackState.fromValue(2)
        )
        assertEquals(RTSPVideoPlayerPlaybackState.Ready, RTSPVideoPlayerPlaybackState.fromValue(3))
        assertEquals(RTSPVideoPlayerPlaybackState.Ended, RTSPVideoPlayerPlaybackState.fromValue(4))
        assertEquals(
            RTSPVideoPlayerPlaybackState.Playing,
            RTSPVideoPlayerPlaybackState.fromValue(10)
        )
    }

    @Test
    fun `fromValue returns null for invalid integers`() {
        assertNull(RTSPVideoPlayerPlaybackState.fromValue(0))
        assertNull(RTSPVideoPlayerPlaybackState.fromValue(99))
        assertNull(RTSPVideoPlayerPlaybackState.fromValue(-1))
    }
}
