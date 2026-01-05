package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RTSPItemRepositoryImplTest {

    // Mocks
    private val context: Context = mock()
    private val database: AppDatabase = mock()
    private val dao: RTSPItemDao = mock()

    // Subject under test
    private lateinit var repository: RTSPItemRepositoryImpl

    @Before
    fun setup() {
        // Wire up the Database mock to return the DAO mock
        whenever(database.rtspItemDao()).thenReturn(dao)

        repository = RTSPItemRepositoryImpl(context, database)
    }

    // =========================================================================
    // Database Delegation Tests
    // =========================================================================

    @Test
    fun `loadData fetches items from DAO and updates state flow`() = runTest {
        // Arrange
        val mockData = listOf(
            createItem(1, "Cam 1"),
            createItem(2, "Cam 2")
        )
        whenever(dao.getItems(any(), any())).thenReturn(mockData)

        // Act
        repository.loadData()

        // Assert
        verify(dao).getItems(0, Long.MAX_VALUE)
        assertEquals(mockData, repository.items.value)
    }

    @Test
    fun `addItem delegates to DAO and returns ID`() = runTest {
        // Arrange
        val item = createItem(0, "New Cam")
        whenever(dao.insert(item)).thenReturn(99L)

        // Act
        val resultId = repository.addItem(item)

        // Assert
        verify(dao).insert(item)
        assertEquals(99L, resultId)
    }

    @Test
    fun `updateItem delegates to DAO`() = runTest {
        // Arrange
        val item = createItem(1, "Updated Cam")
        whenever(dao.update(item)).thenReturn(1)

        // Act
        val rowsAffected = repository.updateItem(item)

        // Assert
        verify(dao).update(item)
        assertEquals(1, rowsAffected)
    }

    @Test
    fun `deleteItem delegates to DAO`() = runTest {
        // Arrange
        val item = createItem(1, "Delete Me")
        whenever(dao.delete(item)).thenReturn(1)

        // Act
        val rowsAffected = repository.deleteItem(item)

        // Assert
        verify(dao).delete(item)
        assertEquals(1, rowsAffected)
    }

    @Test
    fun `reorderItems maps to Update Entity and delegates to DAO`() = runTest {
        // Arrange
        val inputList = listOf(
            createItem(10, "A", order = 1),
            createItem(20, "B", order = 2)
        )

        // We must tell the mock to return an Int (e.g., 2 rows updated).
        // Otherwise, it returns null, causing the NPE when unboxing.
        whenever(dao.updateOrders(any())).thenReturn(2)
        // ---------------------

        // Act
        repository.reorderItems(inputList)

        // Assert
        val captor = argumentCaptor<List<RTSPItemOrderUpdate>>()
        verify(dao).updateOrders(captor.capture())

        val capturedUpdates = captor.firstValue
        assertEquals(2, capturedUpdates.size)

        assertEquals(10L, capturedUpdates[0].id)
        assertEquals(1, capturedUpdates[0].order)

        assertEquals(20L, capturedUpdates[1].id)
        assertEquals(2, capturedUpdates[1].order)
    }

    // =========================================================================
    // File System Tests
    // =========================================================================

    @Test
    fun `previewPathFor generates correct file path using context cache dir`() {
        // Arrange
        val mockCacheDir = File("/data/user/0/com.app/cache")
        whenever(context.cacheDir).thenReturn(mockCacheDir)
        val item = createItem(123, "Cam")

        // Act
        val resultFile = repository.previewPathFor(item)

        // Assert
        // Should be: /data/user/0/com.app/cache/preview_123.jpg
        assertEquals(File(mockCacheDir, "preview_123.jpg").absolutePath, resultFile.absolutePath)
    }

    // =========================================================================
    // Bitmap Cache & Memory Management Tests
    // =========================================================================

    @Test
    fun `cachePreviewFor updates state flow`() {
        // Arrange
        val item = createItem(1, "Cam")
        val bitmap: Bitmap = mock()

        // Act
        repository.cachePreviewFor(item, bitmap)

        // Assert
        assertEquals(bitmap, repository.cachedPreviews.value[1L])
    }

    @Test
    fun `cachePreviewFor recycles OLD bitmap when replacing with NEW one`() {
        // Arrange
        val item = createItem(1, "Cam")
        val oldBitmap: Bitmap = mock()
        val newBitmap: Bitmap = mock()

        // Pre-fill cache
        repository.cachePreviewFor(item, oldBitmap)

        // Act - Replace with new bitmap
        repository.cachePreviewFor(item, newBitmap)

        // Assert
        // 1. Map should hold new bitmap
        assertEquals(newBitmap, repository.cachedPreviews.value[1L])
        // 2. Old bitmap should be recycled to prevent leaks
        verify(oldBitmap).recycle()
        // 3. New bitmap should NOT be recycled
        verify(newBitmap, times(0)).recycle()
    }

    @Test
    fun `cachePreviewFor does NOT recycle if bitmap object is identical`() {
        // Arrange
        val item = createItem(1, "Cam")
        val bitmap: Bitmap = mock()

        // Pre-fill cache
        repository.cachePreviewFor(item, bitmap)

        // Act - Set the exact same instance again
        repository.cachePreviewFor(item, bitmap)

        // Assert
        // Should not call recycle() because it's the same object
        verify(bitmap, times(0)).recycle()
    }

    @Test
    fun `removeCachedPreviews clears map and recycles all bitmaps`() {
        // Arrange
        val item1 = createItem(1, "Cam1")
        val item2 = createItem(2, "Cam2")
        val bitmap1: Bitmap = mock()
        val bitmap2: Bitmap = mock()

        // Pre-fill cache
        repository.cachePreviewFor(item1, bitmap1)
        repository.cachePreviewFor(item2, bitmap2)

        // Act
        repository.removeCachedPreviews()

        // Assert
        // 1. Map should be empty
        assertTrue(repository.cachedPreviews.value.isEmpty())
        // 2. Both bitmaps should be recycled
        verify(bitmap1).recycle()
        verify(bitmap2).recycle()
    }

    @Test
    fun `removeCachedPreviews skips recycle if already recycled`() {
        // Arrange
        val item = createItem(1, "Cam")
        val bitmap: Bitmap = mock()
        whenever(bitmap.isRecycled).thenReturn(true)

        repository.cachePreviewFor(item, bitmap)

        // Act
        repository.removeCachedPreviews()

        // Assert
        // Should not call recycle() again if isRecycled is true
        verify(bitmap, times(0)).recycle()
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private fun createItem(id: Long, name: String, order: Int = 0): RTSPItem {
        return RTSPItem(
            id = id,
            name = name,
            uri = "rtsp://test",
            tags = emptyList(),
            order = order,
            forceTcp = false
        )
    }
}
