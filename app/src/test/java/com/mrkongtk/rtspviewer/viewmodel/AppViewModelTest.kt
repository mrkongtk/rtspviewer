package com.mrkongtk.rtspviewer.viewmodel

import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
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
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    // 1. Mock the Repository
    @Mock
    private lateinit var repository: RTSPItemRepository

    private lateinit var viewModel: AppViewModel

    // Test Dispatcher for Coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Fake data flow from repository
    private val repoItemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // 2. Set the Main dispatcher to our test dispatcher
        Dispatchers.setMain(testDispatcher)

        // 3. Setup default mock behavior
        // The ViewModel observes this flow immediately upon init
        whenever(repository.items).thenReturn(repoItemsFlow)

        // Initialize ViewModel
        viewModel = AppViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls loadData`() = runTest(testDispatcher) {
        // Allow init block to execute
        testScheduler.advanceUntilIdle()

        verify(repository, times(1)).loadData()
    }

    @Test
    fun `uiState combines repo items and selection correctly`() = runTest(testDispatcher) {
        // Arrange
        val testItem = RTSPItem(1, "Test", "uri", emptyList(), 0)
        repoItemsFlow.value = listOf(testItem)

        // Act
        viewModel.select(testItem)
        testScheduler.advanceUntilIdle() // Process Flow combination

        // Assert
        val state = viewModel.uiState.first()
        assertEquals(1, state.items.size)
        assertEquals(testItem, state.selectedItem)
    }

    @Test
    fun `addItem triggers repository add and reload on success`() = runTest(testDispatcher) {
        // Arrange
        val newItem = RTSPItem(0, "New", "uri", emptyList(), 0)
        whenever(repository.addItem(newItem)).thenReturn(1L) // Return 1 (success)

        // Act
        viewModel.addItem(newItem)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(repository).addItem(newItem)
        // loadData is called once in init, and should be called again after add
        verify(repository, times(2)).loadData()
    }

    @Test
    fun `reorderItems triggers reload only on success`() = runTest(testDispatcher) {
        // Arrange
        val items = listOf(RTSPItem(1, "A", "u", emptyList(), 0))
        whenever(repository.reorderItems(items)).thenReturn(1) // Success

        // Act
        viewModel.reorderItems(items)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(repository).reorderItems(items)
        verify(repository, times(2)).loadData() // Init + Reorder
    }

    @Test
    fun `deleteItem triggers reload on success`() = runTest(testDispatcher) {
        val item = RTSPItem(1, "A", "u", emptyList(), 0)
        whenever(repository.deleteItem(item)).thenReturn(1)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        verify(repository).deleteItem(item)
        verify(repository, times(2)).loadData()
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
}
