package com.mrkongtk.rtspviewer.shared.data.database.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RTSPItemTest {

    @Test
    fun test_custom_equals_returns_true_for_identical_data() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)

        assertEquals(item1, item2, "Objects with identical data should be equal")
        assertEquals(item1.hashCode(), item2.hashCode(), "HashCodes should match")
    }

    @Test
    fun test_custom_equals_returns_false_for_different_forceTcp() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, false)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0, true)

        assertNotEquals(item1, item2, "Objects with different forceTcp should not be equal")
    }

    @Test
    fun test_custom_equals_returns_false_for_different_tags() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1"), 0)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1", "tag2"), 0)

        assertNotEquals(item1, item2, "Objects with different tags should not be equal")
    }

    @Test
    fun test_custom_equals_returns_true_for_different_tags_order() {
        val item1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag2", "tag1"), 0)
        val item2 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("tag1", "tag2"), 0)

        assertEquals(item1, item2, "Objects with different tags order should be equal")
    }

    @Test
    fun test_stringify_logic_handles_empty_tags() {
        // We verify this indirectly via equals to ensure the joinToString doesn't crash or behave oddly
        val item1 = RTSPItem(1, "Cam", "uri", emptyList(), 0)
        val item2 = RTSPItem(1, "Cam", "uri", emptyList(), 0)

        assertEquals(item1, item2)
    }

    @Test
    fun test_copy_creates_equal_object() {
        val item1 = RTSPItem(1, "Cam", "uri", listOf("A"), 1)
        val item2 = item1.copy()

        assertEquals(item1, item2)
    }
}
