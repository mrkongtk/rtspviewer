package com.mrkongtk.rtspviewer.viewmodel

import app.cash.turbine.test
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppBarViewModelTest {

    private lateinit var viewModel: AppBarViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Set the Main dispatcher to a test dispatcher for ViewModelScope
        Dispatchers.setMain(testDispatcher)
        viewModel = AppBarViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Start screen with no back navigation`() = runTest {
        viewModel.canNavigateBack.test {
            assertFalse(awaitItem())
        }

        viewModel.title.test {
            val title = awaitItem()
            assertEquals(AppScreen.Start.title, title.id)
            assertTrue(title.args.isEmpty())
        }
    }

    @Test
    fun `update screen updates title and navigation state`() = runTest {
        // Switch to Add screen with back navigation enabled
        viewModel.update(AppScreen.AddRTSPItem, canNavigateBack = true)

        viewModel.canNavigateBack.test {
            assertTrue(awaitItem())
        }

        viewModel.title.test {
            val title = awaitItem()
            assertEquals(AppScreen.AddRTSPItem.title, title.id)
        }
    }

    @Test
    fun `update item provides formatting arguments for the title`() = runTest {
        val mockItem = RTSPItem(
            id = 1,
            name = "Front Door",
            uri = "rtsp://...",
            tags = emptyList(),
            order = 0
        )

        viewModel.update(mockItem)

        viewModel.title.test {
            val title = awaitItem()
            // Verify that the name "Front Door" is passed as an argument
            assertEquals("Front Door", title.args.first())
            assertEquals(1, title.args.size)
        }
    }

    @Test
    fun `update item to null clears formatting arguments`() = runTest {
        val mockItem = RTSPItem(1, "Camera", "rtsp://", emptyList(), 0)

        viewModel.update(mockItem)
        viewModel.update(null)

        viewModel.title.test {
            val title = awaitItem()
            assertTrue(title.args.isEmpty())
        }
    }

    @Test
    fun `full state update maintains consistency`() = runTest {
        val mockItem = RTSPItem(1, "Yard", "rtsp://", emptyList(), 0)

        // Simulate navigating to the Display screen for a specific camera
        viewModel.update(AppScreen.RTSPDisplay, canNavigateBack = true)
        viewModel.update(mockItem)

        viewModel.title.test {
            val title = awaitItem()
            assertEquals(AppScreen.RTSPDisplay.title, title.id)
            assertEquals("Yard", title.args.first())
        }

        viewModel.canNavigateBack.test {
            assertTrue(awaitItem())
        }
    }
}
