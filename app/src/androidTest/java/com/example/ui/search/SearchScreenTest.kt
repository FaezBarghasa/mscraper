package com.example.ui.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typingInSearchBarUpdatesState() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            SearchScreen(
                navController = navController
            )
        }

        val searchText = "Cyberpunk"
        composeTestRule.onNodeWithText("Search tracks...")
            .performTextInput(searchText)

        composeTestRule.onNodeWithText(searchText).assertExists()
    }
}
