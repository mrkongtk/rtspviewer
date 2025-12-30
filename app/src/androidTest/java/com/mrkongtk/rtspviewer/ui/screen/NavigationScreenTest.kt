package com.mrkongtk.rtspviewer.ui.screen

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Instrumented UI Test for the Navigation Screen.
 *
 * This test verifies the integration between the Navigation component, the AppViewModel,
 * and the UI screens (List Screen and Add Item Screen).
 *
 * It uses Mockito to mock the Repository layer to avoid touching the real database
 * and to allow deterministic control over the data state (Empty vs Populated).
 */
@RunWith(AndroidJUnit4::class)
class NavigationScreenTest {

    /**
     * The ComposeTestRule allows us to set content and interact with Compose nodes
     * (clicking, typing, checking visibility).
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mocking the Repository dependency to isolate the UI/ViewModel logic.
    private val repository: RTSPItemRepository = mock()

    // The ViewModel under test.
    private lateinit var viewModel: AppViewModel

    // TestNavHostController allows us to assert current route and backstack state.
    private lateinit var navController: TestNavHostController

    // A MutableStateFlow used to simulate real-time database updates from the Repository.
    private val itemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * Initial setup runs before every @Test.
     * configures the mock behavior and initializes the ViewModel.
     */
    @Before
    fun setup() {
        // 1. Define Mock Behavior
        // When the ViewModel collects 'repository.items', return our local MutableStateFlow.
        whenever(repository.items).thenReturn(itemsFlow)

        // Mock CRUD operations to return valid row IDs (indicating success) without
        // actually hitting a database.
        runBlocking {
            whenever(repository.loadData()).thenAnswer { } // No-op
            whenever(repository.addItem(any())).thenReturn(1L)
            whenever(repository.updateItem(any())).thenReturn(1)
        }

        // 2. Initialize ViewModel with the mocked repository.
        // Since this is an instrumented test, it runs on the emulator/device main thread.
        viewModel = AppViewModel(repository)
    }

    /**
     * Helper function to set the Compose content.
     * It initializes the Navigation Controller and renders the main NavigationScreen.
     */
    private fun launchScreen() {
        composeTestRule.setContent {
            // Create a TestNavController to verify navigation events
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            // Render the screen, injecting our test ViewModel and NavController
            NavigationScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }

    /**
     * Scenario: The database is empty.
     * Expected: The UI should display the "No Streaming" placeholder text.
     */
    @Test
    fun emptyState_showsNoStreamingMessage() {
        // GIVEN: Repository flow emits an empty list
        itemsFlow.value = emptyList()

        // WHEN: Screen launches
        launchScreen()

        // THEN: The empty state text component is visible on screen
        composeTestRule
            .onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()
    }

    /**
     * Scenario: The database has data.
     * Expected: The UI should display the list item with the correct name.
     */
    @Test
    fun populatedList_showsItems() {
        // GIVEN: Repository flow emits a list containing one item
        val item1 = RTSPItem(1, "Cam One", "rtsp://1", emptyList(), 0)
        itemsFlow.value = listOf(item1)

        // WHEN: Screen launches
        launchScreen()

        // THEN: A node with the text "Cam One" exists and is displayed
        composeTestRule
            .onNodeWithText("Cam One")
            .assertIsDisplayed()
    }

    /**
     * Scenario: User clicks the Floating Action Button (FAB).
     * Expected: The app navigates to the 'AddRTSPItem' screen.
     */
    @Test
    fun clickAddButton_navigatesToAddScreen() {
        // GIVEN: Screen is launched with an empty list
        itemsFlow.value = emptyList()
        launchScreen()

        // WHEN: User clicks the Add (FAB) button
        composeTestRule
            .onNodeWithTag("AddButton")
            .performClick()

        // THEN: Verify the Navigation Controller's current route matches the Add Screen
        composeTestRule.waitForIdle()
        assertEquals(
            AppScreen.AddRTSPItem.name,
            navController.currentBackStackEntry?.destination?.route
        )

        // AND: The root component of the Add Screen is visible
        composeTestRule
            .onNodeWithTag("AddStreamItemScreenRoot")
            .assertIsDisplayed()
    }

    /**
     * Scenario: Full flow of adding a new item.
     * 1. Click Add.
     * 2. Type Name and URI.
     * 3. Click Save.
     * Expected: Repository.addItem is called with correct data, and App navigates back Home.
     */
    @Test
    fun addItemFlow_verifiesRepositoryCall() {
        // GIVEN: Screen is launched
        itemsFlow.value = emptyList()
        launchScreen()

        // Navigate to add screen
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // WHEN: User enters valid data into the form
        onRTSPField("NameTextField").performTextInput("New Cam")
        onRTSPField("UriTextField").performTextInput("rtsp://192.168.1.50")

        // AND: User clicks the Save button
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // THEN: Verify the Mock Repository received the 'addItem' call with the text input by the user
        runBlocking {
            verify(repository).addItem(org.mockito.kotlin.check { item ->
                assertEquals("New Cam", item.name)
                assertEquals("rtsp://192.168.1.50", item.uri)
            })
        }

        // AND: Verify the app navigated back to the Start screen
        composeTestRule.waitForIdle()
        assertEquals(
            AppScreen.Start.name,
            navController.currentBackStackEntry?.destination?.route
        )
    }

    /**
     * Helper to find a specific text field within the Add Item form.
     * Since the TextField is often nested inside a wrapper (like OutlinedTextField),
     * this looks for a node with the generic tag "RTSPOutlinedTextField" that also
     * has an ancestor with the specific [tag] (e.g., "NameTextField").
     */
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}
