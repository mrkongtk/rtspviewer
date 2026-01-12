package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class RTSPItemRepositoryImplTest {

    private lateinit var repository: RTSPItemRepositoryImpl
    private val mockContext: Context = mock()
    private val mockDatabase: AppDatabase = mock()
    private val mockDao: RTSPItemDao = mock()
    private val mockCacheDir: File = mock()

    @Before
    fun setup() {
        // Setup Dao and Database mocks
        whenever(mockDatabase.rtspItemDao()).thenReturn(mockDao)
        whenever(mockDao.getAllItemsFlow()).thenReturn(flowOf(emptyList()))

        // Setup Context for file path testing
        val realCacheDir = File("/tmp/mock_cache")
        whenever(mockContext.cacheDir).thenReturn(realCacheDir)
        whenever(mockCacheDir.path).thenReturn("/mock/cache")

        repository = RTSPItemRepositoryImpl(mockContext, mockDatabase)
    }

    @Test
    fun `addItem sanitizes tags by trimming and removing duplicates`() = runTest {
        val inputItem = RTSPItem(
            id = 0,
            name = "Test Camera",
            uri = "rtsp://link",
            tags = listOf(" outdoor ", "", "HD", "HD "), // messy tags
            order = 0
        )

        repository.addItem(inputItem)

        // Capture the item passed to the DAO
        argumentCaptor<RTSPItem>().apply {
            verify(mockDao).insert(capture())
            val savedItem = firstValue

            // Expected sanitized: ["outdoor", "HD"]
            assertEquals(2, savedItem.tags.size)
            assertTrue(savedItem.tags.contains("outdoor"))
            assertTrue(savedItem.tags.contains("HD"))
        }
    }

    @Test
    fun `reorderItems maps items to order update projections`() = runTest {
        val items = listOf(
            RTSPItem(10L, "A", "uri", emptyList(), 0),
            RTSPItem(20L, "B", "uri", emptyList(), 1)
        )

        repository.reorderItems(items)

        argumentCaptor<List<RTSPItemOrderUpdate>>().apply {
            verify(mockDao).updateOrders(capture())
            val updates = firstValue

            assertEquals(2, updates.size)
            assertEquals(10L, updates[0].id)
            assertEquals(0, updates[0].order)
            assertEquals(20L, updates[1].id)
            assertEquals(1, updates[1].order)
        }
    }

    @Test
    fun `previewPathFor returns correct file format in cache directory`() {
        val item = RTSPItem(555L, "Cam", "uri", emptyList(), 0)

        val file = repository.previewPathFor(item)

        // Check if the filename contains the ID and correct extension
        assertTrue(file.name.contains("555"))
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun `cachePreviewFor updates state flow and recycles old bitmap`() {
        val item = RTSPItem(1L, "Cam", "uri", emptyList(), 0)
        val oldBitmap: Bitmap = mock { on { isRecycled } doReturn false }
        val newBitmap: Bitmap = mock { on { isRecycled } doReturn false }

        // 1. Initial cache
        repository.cachePreviewFor(item, oldBitmap)
        assertEquals(oldBitmap, repository.cachedPreviews.value[1L])

        // 2. Update cache with new bitmap
        repository.cachePreviewFor(item, newBitmap)

        // Verify state update
        assertEquals(newBitmap, repository.cachedPreviews.value[1L])

        // Verify memory management: old bitmap should be recycled
        verify(oldBitmap).recycle()
        verify(newBitmap, never()).recycle()
    }

    @Test
    fun `removeCachedPreviews recycles all bitmaps and clears map`() {
        val item1 = RTSPItem(1L, "C1", "u", emptyList(), 0)
        val item2 = RTSPItem(2L, "C2", "u", emptyList(), 1)
        val bmp1: Bitmap = mock { on { isRecycled } doReturn false }
        val bmp2: Bitmap = mock { on { isRecycled } doReturn false }

        repository.cachePreviewFor(item1, bmp1)
        repository.cachePreviewFor(item2, bmp2)

        repository.removeCachedPreviews()

        // Verify map is empty
        assertTrue(repository.cachedPreviews.value.isEmpty())

        // Verify all recycled
        verify(bmp1).recycle()
        verify(bmp2).recycle()
    }

    @Test
    fun `deleteItem calls dao delete`() = runTest {
        val item = RTSPItem(1L, "Delete Me", "u", emptyList(), 0)

        repository.deleteItem(item)

        verify(mockDao).delete(item)
    }
}
