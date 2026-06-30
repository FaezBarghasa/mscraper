package com.example.ui.playlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PlaylistMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingMenuOpensDropdown() {
        composeTestRule.setContent {
            PlaylistMenu(
                playlistName = "Test",
                tracks = emptyList(),
                onImportSuccess = { _, _ -> },
                accentColor = Color.Cyan
            )
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        // verify options are visible
        // composeTestRule.onNodeWithText("Export to JSON").assertExists()
    }
}
