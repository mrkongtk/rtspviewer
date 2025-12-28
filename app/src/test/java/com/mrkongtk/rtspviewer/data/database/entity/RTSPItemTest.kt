package com.mrkongtk.rtspviewer.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RTSPItemTest {

    @Test
    fun `test custom equals returns true for identical data`() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)

        assertEquals("Objects with identical data should be equal", item1, item2)
        assertEquals("HashCodes should match", item1.hashCode(), item2.hashCode())
    }

    @Test
    fun `test custom equals returns false for different forceTcp`() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, true)

        assertNotEquals("Objects with different forceTcp should not be equal", item1, item2)
    }

    @Test
    fun `test custom equals returns false for different tags`() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1", "tag2"), 0)

        assertNotEquals("Objects with different tags should not be equal", item1, item2)
    }

    @Test
    fun `test custom equals returns true for different tags order`() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag2", "tag1"), 0)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1", "tag2"), 0)

        assertEquals("Objects with different tags order should be equal", item1, item2)
    }

    @Test
    fun `test stringify logic handles empty tags`() {
        // We verify this indirectly via equals to ensure the joinToString doesn't crash or behave oddly
        val item1 = RTSPItem(1, "Cam", "uri", emptyList(), 0)
        val item2 = RTSPItem(1, "Cam", "uri", emptyList(), 0)

        assertEquals(item1, item2)
    }

    @Test
    fun `test copy creates equal object`() {
        val item1 = RTSPItem(1, "Cam", "uri", listOf("A"), 1)
        val item2 = item1.copy()

        assertEquals(item1, item2)
    }
}
