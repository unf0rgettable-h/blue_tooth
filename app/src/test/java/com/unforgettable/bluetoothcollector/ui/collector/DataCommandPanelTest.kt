package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"])
class DataCommandPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ts60_panel_shows_vendor_routes_and_experimental_receiver_boundary() {
        composeTestRule.setContent {
            DataActionPanel(
                uiState = CollectorUiState(
                    selectedBrandId = "leica",
                    selectedModelId = "TS60",
                ),
                onStartReceivingRequested = {},
                onStopReceivingRequested = {},
                onSingleMeasureRequested = {},
                onStartImportRequested = {},
                onStartReceiverRequested = {},
                onClearRequested = {},
                onExportRequested = {},
                onSaveToLocalRequested = {},
            )
        }

        composeTestRule.onNodeWithText("推荐连接").assertIsDisplayed()
        composeTestRule.onNodeWithText("WLAN FTP项目传输").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cable RS232").assertIsDisplayed()
        composeTestRule.onNodeWithText("USB Cable").assertIsDisplayed()
        composeTestRule.onNodeWithText("实验监听").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android蓝牙实验监听").assertIsDisplayed()
    }

    @Test
    fun ts09_panel_keeps_plain_verified_bluetooth_import_without_ts60_boundary() {
        composeTestRule.setContent {
            DataActionPanel(
                uiState = CollectorUiState(
                    selectedBrandId = "leica",
                    selectedModelId = "TS09",
                ),
                onStartReceivingRequested = {},
                onStopReceivingRequested = {},
                onSingleMeasureRequested = {},
                onStartImportRequested = {},
                onStartReceiverRequested = {},
                onClearRequested = {},
                onExportRequested = {},
                onSaveToLocalRequested = {},
            )
        }

        composeTestRule.onNodeWithText("经典蓝牙导入").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Android蓝牙实验监听").assertCountEquals(0)
    }
}
