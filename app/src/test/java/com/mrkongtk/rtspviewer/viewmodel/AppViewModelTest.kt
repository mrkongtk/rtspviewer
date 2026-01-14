package com.mrkongtk.rtspviewer.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    // Backing flows for Mocks (Room-like behavior)
    private val itemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())
    private val cachedPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    private lateinit var viewModel: AppViewModel

    private val mockItem1 = RTSPItem(1, "Living Room", "rtsp://1", listOf("Indoor"), 1)
    private val mockItem2 = RTSPItem(2, "Garage", "rtsp://2", listOf("Outdoor", "Indoor"), 2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        rtspItemRepository = mock {
            on { items } doReturn itemsFlow
            on { cachedPreviews } doReturn cachedPreviewsFlow
        }
        fileRepository = mock()

        viewModel = AppViewModel(rtspItemRepository, fileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state matches default AppUiState`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(emptyList<RTSPItem>(), initialState.items)
            assertNull(initialState.selectedItem)
            assertNull(initialState.selectedTag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState aggregates and sorts unique tags from items`() = runTest {
        viewModel.uiState.test {
            // Skip initial empty state
            skipItems(1)

            // Emit items with overlapping and unsorted tags
            itemsFlow.value = listOf(mockItem2, mockItem1)

            val state = awaitItem()
            // Should be unique and alphabetical: ["Indoor", "Outdoor"]
            assertEquals(listOf("Indoor", "Outdoor"), state.tags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting an item updates uiState`() = runTest {
        itemsFlow.value = listOf(mockItem1)

        viewModel.uiState.test {
            skipItems(1) // Initial data state

            viewModel.select(mockItem1)
            assertEquals(mockItem1, awaitItem().selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting an invalid tag resets selectedTag to null`() = runTest {
        itemsFlow.value = listOf(mockItem1) // Contains "Indoor"

        viewModel.uiState.test {
            skipItems(1)

            // Valid selection
            viewModel.select("Indoor")
            assertEquals("Indoor", awaitItem().selectedTag)

            // Invalid selection
            viewModel.select("Garden")
            assertNull(awaitItem().selectedTag)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reorderItems delegates update to repository`() = runTest {
        val newList = listOf(mockItem2.copy(order = 0), mockItem1.copy(order = 1))
        viewModel.reorderItems(newList)
        advanceUntilIdle()
        verify(rtspItemRepository).reorderItems(newList)
    }

    @Test
    fun `editItem delegates update to repository`() = runTest {
        val updatedItem = mockItem1.copy(name = "New Name")
        viewModel.editItem(updatedItem)
        advanceUntilIdle()
        verify(rtspItemRepository).updateItem(updatedItem)
    }

    @Test
    fun `savePreview updates cache only after successful file write`() = runTest {
        val mockBitmap = mock<Bitmap>()
        val mockFile = File("dummy/path.jpg")

        whenever(rtspItemRepository.previewPathFor(mockItem1)).thenReturn(mockFile)

        // Scenario A: Success
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(true)
        viewModel.savePreview(mockItem1, mockBitmap)
        advanceUntilIdle()
        verify(rtspItemRepository).cachePreviewFor(mockItem1, mockBitmap)

        // Scenario B: Failure
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(false)
        viewModel.savePreview(mockItem1, mockBitmap)
        advanceUntilIdle()
        // verify cache was not called again for the second attempt
        verify(rtspItemRepository, never()).cachePreviewFor(mockItem2, mockBitmap)
    }

    @Test
    fun `selectedItem is cleared if removed from the underlying data`() = runTest {
        viewModel.uiState.test {
            // 1. Initial Load
            itemsFlow.value = listOf(mockItem1, mockItem2)
            skipItems(1)

            // 2. Select Item 1
            viewModel.select(mockItem1)
            assertEquals(mockItem1, awaitItem().selectedItem)

            // 3. Update data, removing Item 1
            itemsFlow.value = listOf(mockItem2)

            // 4. State should reconcile and clear selectedItem
            val finalState = awaitItem()
            assertNull(finalState.selectedItem)
            assertEquals(1, finalState.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCleared triggers bitmap cache recycling`() = runTest {
        // We use reflection to trigger the protected onCleared method
        val onClearedMethod = ViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)

        verify(rtspItemRepository).removeCachedPreviews()
    }

    @Test
    fun `addItem delegates insertion to repository`() = runTest {
        viewModel.addItem(mockItem1)
        advanceUntilIdle()
        verify(rtspItemRepository).addItem(mockItem1)
    }
}
