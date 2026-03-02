package com.mrkongtk.rtspviewer.shared.data.database

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListStringConvertersTest {

    private lateinit var converter: ListStringConverters

    @BeforeTest
    fun setup() {
        converter = ListStringConverters()
    }

    @Test
    fun fromListShouldConvertListToJSONString() {
        val list = listOf("camera1", "office", "rtsp://link")
        val result = converter.fromList(list)
        // Note: Check if your converter produces spaces or not in commonMain
        assertEquals("[\"camera1\",\"office\",\"rtsp://link\"]", result)
    }

    @Test
    fun fromStringShouldConvertJSONStringBackToList() {
        val json = "[\"item1\",\"item2\"]"
        val result = converter.fromString(json)
        assertEquals(2, result.size)
        assertEquals("item1", result[0])
        assertEquals("item2", result[1])
    }

    @Test
    fun fromStringWithEmptyStringShouldReturnEmptyList() {
        val result = converter.fromString("   ")
        assertEquals(0, result.size)
    }

    @Test
    fun conversionShouldHandleSpecialCharacters() {
        val original = listOf("comma,here", "quote\"here", "{json:style}")
        val encoded = converter.fromList(original)
        val decoded = converter.fromString(encoded)

        assertEquals(original, decoded)
    }
}
