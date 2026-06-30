package com.example.ui.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerControlsAreDisplayed() {
        composeTestRule.setContent {
            NowPlayingScreen(onBack = {})
        }

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next").assertIsDisplayed()
    }
}
