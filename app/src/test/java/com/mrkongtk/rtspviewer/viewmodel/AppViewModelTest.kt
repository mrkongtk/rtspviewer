package com.mrkongtk.rtspviewer.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private lateinit var rtspItemRepository: RTSPItemRepository
    private lateinit var fileRepository: FileRepository

    // Flows for Mocks
    private val itemsFlow = MutableSharedFlow<List<RTSPItem>>()
    private val cachedPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    private lateinit var viewModel: AppViewModel

    private val mockItem1 = RTSPItem(1, "Cam 1", "rtsp://1", listOf("Home"), 1)
    private val mockItem2 = RTSPItem(2, "Cam 2", "rtsp://2", listOf("Work", "Home"), 2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        rtspItemRepository = mock {
            on { items } doReturn itemsFlow
            on { cachedPreviews } doReturn cachedPreviewsFlow
        }
        fileRepository = mock()

        // Instantiate the ViewModel
        viewModel = AppViewModel(rtspItemRepository, fileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty AppUiState`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(emptyList<RTSPItem>(), initialState.items)
            assertNull(initialState.selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when repository emits items, uiState updates with items and unique sorted tags`() =
        runTest {
            viewModel.uiState.test {
                // Skip initial state
                skipItems(1)

                // Emit data from repository
                itemsFlow.emit(listOf(mockItem2, mockItem1))

                val state = awaitItem()
                assertEquals(2, state.items.size)
                // Verify tags are extracted, unique, and sorted: ["Home", "Work"]
                assertEquals(listOf("Home", "Work"), state.tags)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `select item updates selectedItem in uiState`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.select(mockItem1)

            val state = awaitItem()
            assertEquals(mockItem1, state.selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `select tag updates selectedTag in uiState if tag is valid`() = runTest {
        // First, provide items so the ViewModel knows which tags are valid
        itemsFlow.emit(listOf(mockItem1))
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state after emitting items
            skipItems(1)

            viewModel.select("Home")
            assertEquals("Home", awaitItem().selectedTag)

            // Select invalid tag
            viewModel.select("Invalid")
            assertNull(awaitItem().selectedTag)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addItem delegates call to repository`() = runTest {
        viewModel.addItem(mockItem1)
        advanceUntilIdle()
        verify(rtspItemRepository).addItem(mockItem1)
    }

    @Test
    fun `deleteItem delegates call to repository`() = runTest {
        viewModel.deleteItem(mockItem1)
        advanceUntilIdle()
        verify(rtspItemRepository).deleteItem(mockItem1)
    }

    @Test
    fun `savePreview writes to file and then updates cache on success`() = runTest {
        val mockBitmap = mock<Bitmap>()
        val mockFile = File("path/to/preview.jpg")

        whenever(rtspItemRepository.previewPathFor(mockItem1)).thenReturn(mockFile)
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(true)

        viewModel.savePreview(mockItem1, mockBitmap)
        advanceUntilIdle()

        // Verify file was written
        verify(fileRepository).writeJPEG(mockFile, mockBitmap)
        // Verify memory cache was updated
        verify(rtspItemRepository).cachePreviewFor(mockItem1, mockBitmap)
    }

    @Test
    fun `savePreview does not update cache if file write fails`() = runTest {
        val mockBitmap = mock<Bitmap>()
        val mockFile = File("path/to/preview.jpg")

        whenever(rtspItemRepository.previewPathFor(mockItem1)).thenReturn(mockFile)
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(false)

        viewModel.savePreview(mockItem1, mockBitmap)
        advanceUntilIdle()

        verify(fileRepository).writeJPEG(mockFile, mockBitmap)
        verify(rtspItemRepository, never()).cachePreviewFor(any(), any())
    }

    @Test
    fun `when items update, selectedItem is cleared if it no longer exists in list`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            // 1. Emit list and select item 1
            itemsFlow.emit(listOf(mockItem1, mockItem2))
            viewModel.select(mockItem1)
            skipItems(2) // Skip emit and selection update

            // 2. Emit new list without item 1
            itemsFlow.emit(listOf(mockItem2))

            val state = awaitItem()
            assertNull(state.selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCleared triggers repository preview cleanup`() = runTest {
        // Accessing the internal onCleared is tricky, usually we test it via a custom method
        // or by checking lifecycle. Since we want to ensure it cleans up:
        val privateMethod = ViewModel::class.java.getDeclaredMethod("onCleared")
        privateMethod.isAccessible = true
        privateMethod.invoke(viewModel)

        verify(rtspItemRepository).removeCachedPreviews()
    }
}
