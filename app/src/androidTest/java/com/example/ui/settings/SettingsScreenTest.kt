package com.example.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun togglingQuicSwitchUpdatesUI() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            SettingsScreen(navController = navController)
        }

        composeTestRule.onNodeWithTag("quic_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quic_switch").performClick()
    }
}
