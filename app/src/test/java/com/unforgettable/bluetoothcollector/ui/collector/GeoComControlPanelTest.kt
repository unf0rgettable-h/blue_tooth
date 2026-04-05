package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"])
class GeoComControlPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disconnectedState_buttonsDisabled() {
        composeTestRule.setContent {
            GeoComControlPanel(
                isConnected = false,
                isMeasuring = false,
                onStartStopClick = {},
                onSingleMeasureClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Start Auto").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Measure Once").assertIsNotEnabled()
    }

    @Test
    fun connectedIdleState_buttonsEnabled() {
        composeTestRule.setContent {
            GeoComControlPanel(
                isConnected = true,
                isMeasuring = false,
                onStartStopClick = {},
                onSingleMeasureClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Start Auto").assertIsEnabled()
        composeTestRule.onNodeWithText("Measure Once").assertIsEnabled()
    }

    @Test
    fun measuringState_startBecomesStop_measureOnceDisabled() {
        composeTestRule.setContent {
            GeoComControlPanel(
                isConnected = true,
                isMeasuring = true,
                onStartStopClick = {},
                onSingleMeasureClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Stop").assertIsEnabled()
        composeTestRule.onNodeWithText("Measure Once").assertIsNotEnabled()
    }

    @Test
    fun buttonClicks_triggerCallbacks() {
        var startStopClicked = false
        var singleMeasureClicked = false

        composeTestRule.setContent {
            GeoComControlPanel(
                isConnected = true,
                isMeasuring = false,
                onStartStopClick = { startStopClicked = true },
                onSingleMeasureClick = { singleMeasureClicked = true }
            )
        }
        
        composeTestRule.onNodeWithText("Start Auto").performClick()
        composeTestRule.onNodeWithText("Measure Once").performClick()

        assert(startStopClicked)
        assert(singleMeasureClicked)
    }
}
