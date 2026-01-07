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
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Mock
    private lateinit var rtspRepository: RTSPItemRepository

    @Mock
    private lateinit var fileRepository: FileRepository

    private lateinit var viewModel: AppViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val repoItemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())
    private val repoPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup base flow returns
        whenever(rtspRepository.items).thenReturn(repoItemsFlow)
        whenever(rtspRepository.cachedPreviews).thenReturn(repoPreviewsFlow)
        whenever(rtspRepository.previewPathFor(any())).thenReturn(mock(File::class.java))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper to initialize the ViewModel after mocks are configured.
     */
    private fun createViewModel() {
        viewModel = AppViewModel(rtspRepository, fileRepository)
    }

    @Test
    fun `init calls loadData and hydrates preview cache`() = runTest(testDispatcher) {
        // Arrange
        val item = RTSPItem(1, "Cam", "uri", emptyList(), 0)
        val mockBitmap = mock(Bitmap::class.java)
        val mockFile = mock(File::class.java)

        // 1. Stub loadData to actually populate the flow so the init loop finds the item
        whenever(rtspRepository.loadData()).thenAnswer {
            repoItemsFlow.value = listOf(item)
            Unit
        }

        whenever(rtspRepository.previewPathFor(item)).thenReturn(mockFile)
        whenever(fileRepository.readJPEG(mockFile)).thenReturn(mockBitmap)

        // 2. Act: Initialize now so the init block sees the stubbed loadData behavior
        createViewModel()
        testScheduler.advanceUntilIdle()

        // 3. Assert
        verify(rtspRepository, atLeastOnce()).loadData()
        verify(fileRepository).readJPEG(mockFile)
        verify(rtspRepository).cachePreviewFor(item, mockBitmap)
    }

    @Test
    fun `uiState correctly extracts and sorts tags from items`() = runTest(testDispatcher) {
        // Arrange
        val item1 = RTSPItem(1, "A", "u", listOf("Z", "B"), 0)
        val item2 = RTSPItem(2, "B", "u", listOf("A"), 1)
        repoItemsFlow.value = listOf(item1, item2)

        createViewModel()
        testScheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        assertEquals(listOf("A", "B", "Z"), state.tags)
    }

    @Test
    fun `addItem triggers repository add and reload on success`() = runTest(testDispatcher) {
        val newItem = RTSPItem(0, "New", "uri", emptyList(), 0)
        whenever(rtspRepository.addItem(newItem)).thenReturn(1L)

        createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.addItem(newItem)
        testScheduler.advanceUntilIdle()

        verify(rtspRepository).addItem(newItem)
        // Called once in init, once after addItem
        verify(rtspRepository, times(2)).loadData()
    }

    @Test
    fun `reorderItems triggers reload only if list is not empty`() = runTest(testDispatcher) {
        createViewModel()

        // Case: Populated list
        val items = listOf(RTSPItem(1, "A", "u", emptyList(), 0))
        whenever(rtspRepository.reorderItems(items)).thenReturn(1)

        viewModel.reorderItems(items)
        testScheduler.advanceUntilIdle()

        verify(rtspRepository).reorderItems(items)
        verify(rtspRepository, times(2)).loadData()
    }

    @Test
    fun `select tag updates uiState and handles invalid tags`() = runTest(testDispatcher) {
        val item = RTSPItem(1, "A", "u", listOf("Home"), 0)
        repoItemsFlow.value = listOf(item)

        createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.select("Home")
        testScheduler.advanceUntilIdle()
        assertEquals("Home", viewModel.uiState.value.selectedTag)

        viewModel.select("Invalid")
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedTag)
    }

    @Test
    fun `uiState invalidates selectedItem if it is removed from database`() =
        runTest(testDispatcher) {
            val item = RTSPItem(1, "A", "u", emptyList(), 0)
            repoItemsFlow.value = listOf(item)

            createViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.select(item)
            testScheduler.advanceUntilIdle()
            assertEquals(item, viewModel.uiState.value.selectedItem)

            // Remove item
            repoItemsFlow.value = emptyList()
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedItem)
        }

    @Test
    fun `savePreview writes to file and updates cache on success`() = runTest(testDispatcher) {
        createViewModel()
        val item = RTSPItem(1, "Cam 1", "uri", emptyList(), 0)
        val mockBitmap = mock(Bitmap::class.java)
        val mockFile = mock(File::class.java)

        whenever(rtspRepository.previewPathFor(item)).thenReturn(mockFile)
        whenever(fileRepository.writeJPEG(mockFile, mockBitmap)).thenReturn(true)

        viewModel.savePreview(item, mockBitmap)
        testScheduler.advanceUntilIdle()

        verify(rtspRepository).cachePreviewFor(item, mockBitmap)
    }

    @Test
    fun `onCleared clears repository cache`() {
        createViewModel()
        val method = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)

        verify(rtspRepository).removeCachedPreviews()
    }
}
