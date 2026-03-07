package com.mrkongtk.rtspviewer.shared.viewmodel

import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.AppScreen
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppBarViewModelTest {

    private lateinit var viewModel: AppBarViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        // Multiplatform Coroutines Test API to handle viewModelScope
        Dispatchers.setMain(testDispatcher)
        viewModel = AppBarViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_Start_screen_with_no_back_navigation() = runTest {
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
    fun update_screen_updates_title_and_navigation_state() = runTest {

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
    fun update_item_provides_formatting_arguments_for_the_title() = runTest {
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
            assertEquals("Front Door", title.args.first())
            assertEquals(1, title.args.size)
        }
    }

    @Test
    fun update_item_to_null_clears_formatting_arguments() = runTest {
        val mockItem = RTSPItem(1, "Camera", "rtsp://", emptyList(), 0)

        viewModel.update(mockItem)
        viewModel.update(null)

        viewModel.title.test {
            val title = awaitItem()
            assertTrue(title.args.isEmpty())
        }
    }

    @Test
    fun full_state_update_maintains_consistency() = runTest {
        val mockItem = RTSPItem(1, "Yard", "rtsp://", emptyList(), 0)

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