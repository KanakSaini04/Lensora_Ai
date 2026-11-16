package com.lensora.ai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lensora.ai.ui.navigation.AppNavigation
import com.lensora.ai.ui.theme.Lensora_AiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBottomNavigationTabSwitching() {
        composeTestRule.setContent {
            Lensora_AiTheme {
                AppNavigation()
            }
        }

        // Tap Studio tab
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()

        // Tap Gallery tab
        composeTestRule.onNodeWithText("Gallery").performClick()
        composeTestRule.waitForIdle()

        // Tap Vibes tab
        composeTestRule.onNodeWithText("Vibes").performClick()
        composeTestRule.waitForIdle()

        // Tap Settings tab
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Tap Camera tab
        composeTestRule.onNodeWithText("Camera").performClick()
        composeTestRule.waitForIdle()
    }
}
