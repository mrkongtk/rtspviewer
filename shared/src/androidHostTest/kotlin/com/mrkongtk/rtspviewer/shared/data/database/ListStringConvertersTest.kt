package com.mrkongtk.rtspviewer.shared.data.database

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ListStringConvertersTest {

    private lateinit var converter: ListStringConverters

    @Before
    fun setup() {
        converter = ListStringConverters()
    }

    @Test
    fun `fromList should convert list to JSON string`() {
        val list = listOf("camera1", "office", "rtsp://link")
        val result = converter.fromList(list)
        // JSON format for a list of strings
        assertEquals("[\"camera1\",\"office\",\"rtsp://link\"]", result)
    }

    @Test
    fun `fromString should convert JSON string back to list`() {
        val json = "[\"item1\",\"item2\"]"
        val result = converter.fromString(json)
        assertEquals(2, result.size)
        assertEquals("item1", result[0])
        assertEquals("item2", result[1])
    }

    @Test
    fun `fromString with empty string should return empty list`() {
        val result = converter.fromString("   ")
        assertEquals(0, result.size)
    }

    @Test
    fun `conversion should handle special characters like commas and quotes`() {
        val original = listOf("comma,here", "quote\"here", "{json:style}")
        val encoded = converter.fromList(original)
        val decoded = converter.fromString(encoded)

        assertEquals(original, decoded)
    }
}
