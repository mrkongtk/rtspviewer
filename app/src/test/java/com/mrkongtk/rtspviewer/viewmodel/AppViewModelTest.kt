package com.mrkongtk.rtspviewer.viewmodel

import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    // 1. Mock the Repository
    @Mock
    private lateinit var rtspRepository: RTSPItemRepository

    @Mock
    private lateinit var fileRepository: FileRepository

    private lateinit var viewModel: AppViewModel

    // Test Dispatcher for Coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Fake data flows from repository
    private val repoItemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())
    private val repoPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // 2. Set the Main dispatcher to our test dispatcher
        Dispatchers.setMain(testDispatcher)

        // 3. Setup default mock behavior
        // The ViewModel observes these flows immediately upon init via the 'combine' operator
        whenever(rtspRepository.items).thenReturn(repoItemsFlow)
        whenever(rtspRepository.cachedPreviews).thenReturn(repoPreviewsFlow)

        // Prevent NPEs if init block tries to resolve paths for items immediately
        whenever(rtspRepository.previewPathFor(any())).thenReturn(mock(File::class.java))

        // Initialize ViewModel
        viewModel = AppViewModel(rtspRepository, fileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls loadData`() = runTest(testDispatcher) {
        // Allow init block to execute
        testScheduler.advanceUntilIdle()

        verify(rtspRepository, times(1)).loadData()
    }

    @Test
    fun `uiState combines repo items, previews, and selection correctly`() =
        runTest(testDispatcher) {
        // Arrange
        val testItem = RTSPItem(1, "Test", "uri", emptyList(), 0)
            val mockBitmap = mock(Bitmap::class.java)

            repoItemsFlow.value = listOf(testItem)
            repoPreviewsFlow.value = mapOf(1L to mockBitmap)

        // Act
        viewModel.select(testItem)
        testScheduler.advanceUntilIdle() // Process Flow combination

        // Assert
        val state = viewModel.uiState.first()
        assertEquals(1, state.items.size)
        assertEquals(testItem, state.selectedItem)
            assertEquals(mockBitmap, state.cachedPreviews[1L])
    }

    @Test
    fun `addItem triggers repository add and reload on success`() = runTest(testDispatcher) {
        // Arrange
        val newItem = RTSPItem(0, "New", "uri", emptyList(), 0)
        whenever(rtspRepository.addItem(newItem)).thenReturn(1L) // Return 1 (success)

        // Act
        viewModel.addItem(newItem)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(rtspRepository).addItem(newItem)
        // loadData is called once in init, and should be called again after add
        verify(rtspRepository, times(2)).loadData()
    }

    @Test
    fun `reorderItems triggers reload only on success`() = runTest(testDispatcher) {
        // Arrange
        val items = listOf(RTSPItem(1, "A", "u", emptyList(), 0))
        whenever(rtspRepository.reorderItems(items)).thenReturn(1) // Success

        // Act
        viewModel.reorderItems(items)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(rtspRepository).reorderItems(items)
        verify(rtspRepository, times(2)).loadData() // Init + Reorder
    }

    @Test
    fun `deleteItem triggers reload on success`() = runTest(testDispatcher) {
        val item = RTSPItem(1, "A", "u", emptyList(), 0)
        whenever(rtspRepository.deleteItem(item)).thenReturn(1)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        verify(rtspRepository).deleteItem(item)
        verify(rtspRepository, times(2)).loadData()
    }

    @Test
    fun `select updates uiState selectedItem`() = runTest(testDispatcher) {
        val item = RTSPItem(1, "A", "u", emptyList(), 0)
        // Ensure the item exists in the repo flow so the combine logic validates it
        repoItemsFlow.value = listOf(item)

        viewModel.select(item)
        testScheduler.advanceUntilIdle()

        val currentState = viewModel.uiState.first()
        assertEquals(item, currentState.selectedItem)
    }

    @Test
    fun `uiState invalidates selectedItem if it is removed from database`() =
        runTest(testDispatcher) {
            // 1. Setup: Item exists and is selected
            val item = RTSPItem(1, "A", "u", emptyList(), 0)
            repoItemsFlow.value = listOf(item)
            viewModel.select(item)
            testScheduler.advanceUntilIdle()

            assertEquals(item, viewModel.uiState.first().selectedItem)

            // 2. Action: Database updates (Item 1 is deleted/gone)
            repoItemsFlow.value = emptyList()
            testScheduler.advanceUntilIdle()

            // 3. Assert: Selection should be cleared automatically by the 'combine' block
            val newState = viewModel.uiState.first()
            assertNull(
                "Selected item should be null if not found in item list",
                newState.selectedItem
            )
        }

    @Test
    fun `savePreview writes to file and updates cache on success`() = runTest(testDispatcher) {
        // Arrange
        val item = RTSPItem(1, "Cam 1", "uri", emptyList(), 0)
        val mockBitmap = mock(Bitmap::class.java)
        val mockFile = mock(File::class.java)

        whenever(rtspRepository.previewPathFor(item)).thenReturn(mockFile)
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(true)

        // Act
        viewModel.savePreview(item, mockBitmap)
        testScheduler.advanceUntilIdle()

        // Assert
        // 1. Get Path
        verify(rtspRepository).previewPathFor(item)
        // 2. Write File
        verify(fileRepository).writeJPEG(mockFile, mockBitmap)
        // 3. Update Cache (only if write was true)
        verify(rtspRepository).cachePreviewFor(item, mockBitmap)
    }

    @Test
    fun `savePreview does not update cache on file write failure`() = runTest(testDispatcher) {
        // Arrange
        val item = RTSPItem(1, "Cam 1", "uri", emptyList(), 0)
        val mockBitmap = mock(Bitmap::class.java)
        val mockFile = mock(File::class.java)

        whenever(rtspRepository.previewPathFor(item)).thenReturn(mockFile)
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(false) // Failure

        // Act
        viewModel.savePreview(item, mockBitmap)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(fileRepository).writeJPEG(mockFile, mockBitmap)
        // Cache should NOT be updated
        verify(rtspRepository, times(0)).cachePreviewFor(any(), any())
    }

    @Test
    fun `onCleared clears repository cache`() {
        // Act
        // Typically onCleared is protected. We can trigger it by calling the method via reflection
        // or effectively relying on the fact that if we were to invoke the viewmodel's lifecycle end, it runs.
        // However, since we can't easily call protected onCleared directly without a subclass helper in tests,
        // we can assume the user might expose a visible method or we use reflection.

        // Using reflection to invoke protected onCleared() for testing purposes
        val method = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)

        // Assert
        verify(rtspRepository).removeCachedPreviews()
    }
}
