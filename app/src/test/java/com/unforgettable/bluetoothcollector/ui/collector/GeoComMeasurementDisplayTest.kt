package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"])
class GeoComMeasurementDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displayFormatsAnglesAndDistance() {
        val record = MeasurementRecord(
            id = "1", sequence = 1, receivedAt = "now", instrumentBrand = "Leica", instrumentModel = "TS60",
            bluetoothDeviceName = "TS60_123", bluetoothDeviceAddress = "00:11:22:33:44:55",
            rawPayload = "raw", parsedCode = "code", parsedValue = "val",
            hzAngleRad = Math.PI, vAngleRad = Math.PI / 2.0, slopeDistanceM = 12.345
        )

        composeTestRule.setContent {
            GeoComMeasurementDisplay(measurement = record)
        }

        // Default is DEG
        composeTestRule.onNodeWithText("Hz: 180.0000°").assertIsDisplayed()
        composeTestRule.onNodeWithText("V: 90.0000°").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dist: 12.345m").assertIsDisplayed()

        // Click to switch to RAD
        composeTestRule.onNodeWithText("Deg(°)").performClick()
        composeTestRule.onNodeWithText("Hz: 3.1416 rad").assertIsDisplayed()
        composeTestRule.onNodeWithText("V: 1.5708 rad").assertIsDisplayed()

        // Click to switch to GON
        composeTestRule.onNodeWithText("Rad").performClick()
        composeTestRule.onNodeWithText("Hz: 200.0000 gon").assertIsDisplayed()
        composeTestRule.onNodeWithText("V: 100.0000 gon").assertIsDisplayed()
        
        // Click to switch back to DEG
        composeTestRule.onNodeWithText("Gon").performClick()
        composeTestRule.onNodeWithText("Hz: 180.0000°").assertIsDisplayed()
    }
}
