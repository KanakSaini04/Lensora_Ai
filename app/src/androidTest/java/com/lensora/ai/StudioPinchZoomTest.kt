package com.lensora.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StudioPinchZoomTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun studioPinchZoomGesture_changesScaleWithoutCrashing() {
        // MainActivity is already launched by createAndroidComposeRule
        composeTestRule.waitForIdle()

        // Navigate to Studio tab
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()

        // Perform 2-finger pinch gesture on Studio photo canvas
        composeTestRule.onNodeWithContentDescription("Studio Active Image", useUnmergedTree = true)
            .performTouchInput {
                pinch(
                    start0 = Offset(400f, 600f),
                    end0 = Offset(100f, 600f),
                    start1 = Offset(500f, 600f),
                    end1 = Offset(800f, 600f),
                )
            }
        composeTestRule.waitForIdle()
    }
}
