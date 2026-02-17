package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
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
    private val mockFileRepository: FileRepository = mock()

    // Use a SharedFlow to simulate database updates
    private val dbFlow = MutableSharedFlow<List<RTSPItem>>()

    @Before
    fun setup() {
        whenever(mockDatabase.rtspItemDao()).thenReturn(mockDao)
        whenever(mockDao.getAllItemsFlow()).thenReturn(dbFlow)

        // Setup Context for file path testing
        val realCacheDir = File("/tmp/mock_cache")
        whenever(mockContext.cacheDir).thenReturn(realCacheDir)

        repository = RTSPItemRepositoryImpl(mockContext, mockDatabase, mockFileRepository)
    }

    @Test
    fun `addItem sanitizes tags by trimming, removing empty, and removing duplicates`() = runTest {
        val inputItem = RTSPItem(
            id = 0,
            name = "Test Camera",
            uri = "rtsp://link",
            tags = listOf(" outdoor ", "", "HD", "HD "),
            order = 0
        )

        repository.addItem(inputItem)

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
    fun `updateItem also sanitizes tags`() = runTest {
        val inputItem = RTSPItem(1L, "Cam", "rtsp://link", listOf(" tag ", "tag"), 0)

        repository.updateItem(inputItem)

        argumentCaptor<RTSPItem>().apply {
            verify(mockDao).update(capture())
            assertEquals(listOf("tag"), firstValue.tags)
        }
    }

    @Test
    fun `items flow automatically triggers file reading and caching`() = runTest {
        val item = RTSPItem(1L, "Cam", "uri", emptyList(), 0)
        val mockBitmap: Bitmap = mock()

        // Mock that a file exists and returns a bitmap
        whenever(mockFileRepository.readJPEG(any())).thenReturn(mockBitmap)

        // Use Turbine to test the flow
        repository.items.test {
            dbFlow.emit(listOf(item))
            val emittedList = awaitItem()

            assertEquals(1, emittedList.size)
            // Verify that the repository attempted to read the file for the item
            verify(mockFileRepository).readJPEG(any())
            // Verify it was added to the memory cache
            assertEquals(mockBitmap, repository.cachedPreviews.value[1L])
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

        assertTrue(file.name.contains("555"))
        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(file.path.contains("preview_555.jpg"))
    }

    @Test
    fun `cachePreviewFor updates state flow and recycles old bitmap`() {
        val item = RTSPItem(1L, "Cam", "uri", emptyList(), 0)
        val oldBitmap: Bitmap = mock { on { isRecycled } doReturn false }
        val newBitmap: Bitmap = mock { on { isRecycled } doReturn false }

        repository.cachePreviewFor(item, oldBitmap)
        assertEquals(oldBitmap, repository.cachedPreviews.value[1L])

        repository.cachePreviewFor(item, newBitmap)

        assertEquals(newBitmap, repository.cachedPreviews.value[1L])
        verify(oldBitmap).recycle()
        verify(newBitmap, never()).recycle()
    }

    @Test
    fun `removeCachedPreviews recycles all bitmaps and clears map`() {
        val item1 = RTSPItem(1L, "C1", "u", emptyList(), 0)
        val bmp1: Bitmap = mock { on { isRecycled } doReturn false }

        repository.cachePreviewFor(item1, bmp1)
        repository.removeCachedPreviews()

        assertTrue(repository.cachedPreviews.value.isEmpty())
        verify(bmp1).recycle()
    }

    @Test
    fun `deleteItem calls dao delete`() = runTest {
        val item = RTSPItem(1L, "Delete Me", "u", emptyList(), 0)
        repository.deleteItem(item)
        verify(mockDao).delete(item)
    }
}
