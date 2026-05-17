package com.mrkongtk.rtspviewer.shared.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // We use manual Fakes instead of Mockito
    private lateinit var rtspItemRepository: FakeRTSPItemRepository
    private lateinit var fileRepository: FakeFileRepository
    private lateinit var viewModel: AppViewModel

    private val mockItem1 = RTSPItem(1, "Living Room", "rtsp://1", listOf("Indoor"), 1)
    private val mockItem2 = RTSPItem(2, "Garage", "rtsp://2", listOf("Outdoor", "Indoor"), 2)

    @BeforeTest
    fun setup() {
        // Set Main dispatcher for ViewModelScope
        Dispatchers.setMain(testDispatcher)

        rtspItemRepository = FakeRTSPItemRepository()
        fileRepository = FakeFileRepository()

        viewModel = AppViewModel(fileRepository, rtspItemRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_matches_default_AppUiState() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(0, initialState.items.size)
            assertNull(initialState.selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_aggregates_and_sorts_unique_tags() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Initial state

            // Update the fake flow directly
            rtspItemRepository.items.value = listOf(mockItem2, mockItem1)

            val state = awaitItem()
            assertEquals(listOf("Indoor", "Outdoor"), state.tags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selecting_an_item_updates_uiState() = runTest {
        rtspItemRepository.items.value = listOf(mockItem1)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.select(mockItem1)
            assertEquals(mockItem1, awaitItem().selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reorderItems_delegates_update_to_repository() = runTest {
        val newList = listOf(mockItem2, mockItem1)
        viewModel.reorderItems(newList)
        advanceUntilIdle()
        assertTrue(rtspItemRepository.reorderCalled)
    }

    @Test
    fun selectedItem_is_cleared_if_removed_from_underlying_data() = runTest {
        viewModel.uiState.test {
            rtspItemRepository.items.value = listOf(mockItem1, mockItem2)
            skipItems(1)

            viewModel.select(mockItem1)
            assertEquals(mockItem1, awaitItem().selectedItem)

            // Simulate deletion in repository
            rtspItemRepository.items.value = listOf(mockItem2)

            val finalState = awaitItem()
            assertNull(finalState.selectedItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    class FakeFileRepository : FileRepository {
        var readCount = 0
        var lastReadPath: Path? = null
        var bitmapToReturn: ImageBitmap? = null

        override suspend fun readJPEG(file: Path): ImageBitmap? {
            readCount++
            lastReadPath = file
            return bitmapToReturn
        }

        override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap) = true
        override fun getCacheDir(): Path = okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }

    class FakeRTSPItemRepository : RTSPItemRepository {

        private var continueId: Long = Random.nextLong(from = 100000, until = 999999)
        override val items = MutableStateFlow<List<RTSPItem>>(emptyList())
        override val cachedPreviews = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())
        override suspend fun addItem(item: RTSPItem): Long {
            continueId += 1
            val result = item.copy(id = continueId)
            items.update { it + listOf(result) }
            return continueId
        }

        var reorderCalled = false
        var removeCachedCalled = false

        override suspend fun reorderItems(items: List<RTSPItem>): Int {
            reorderCalled = true
            return 1
        }

        override suspend fun updateItem(item: RTSPItem): Int {
            return 1
        }

        override suspend fun deleteItem(item: RTSPItem): Int {
            var deleted = 0
            items.update { original ->
                val removed = original.filter { it.id != item.id }
                deleted = original.count() - removed.count()
                removed
            }
            return deleted
        }

        override fun previewPathFor(item: RTSPItem): Path {
            TODO("Not yet implemented")
        }

        override fun cachePreviewFor(
            item: RTSPItem,
            bitmap: ImageBitmap
        ) {

        }

        override fun removeCachedPreviews() {
            removeCachedCalled = true
        }
    }
}

