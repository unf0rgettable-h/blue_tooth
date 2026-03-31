package com.unforgettable.bluetoothcollector.ui.collector

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import com.unforgettable.bluetoothcollector.ui.theme.CollectorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Screen interaction contract tests. They verify callback paths on CollectorScreen, not end-to-end Route integration.
 */
class CollectorWorkflowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screen_export_button_opens_dialog_and_routes_selected_format_callback() {
        var selectedFormat: ExportFormat? = null

        composeRule.setContent {
            var state by remember { mutableStateOf(sampleUiState()) }
            CollectorTheme {
                CollectorScreen(
                    uiState = state,
                    onInstrumentBrandSelected = {},
                    onInstrumentModelSelected = {},
                    onTargetDeviceSelected = {},
                    onDiscoveryRequested = {},
                    onStopDiscoveryRequested = {},
                    onPairDeviceRequested = {},
                    onConnectRequested = {},
                    onDisconnectRequested = {},
                    onStartReceivingRequested = {},
                    onStopReceivingRequested = {},
                    onClearRequested = {},
                    onExportRequested = {
                        state = state.copy(isExportDialogVisible = true)
                    },
                    onExportFormatSelected = {
                        selectedFormat = it
                        state = state.copy(isExportDialogVisible = false)
                    },
                    onDismissExportDialog = {
                        state = state.copy(isExportDialogVisible = false)
                    },
                )
            }
        }

        composeRule.onNodeWithText("导出并分享").performClick()
        composeRule.onNodeWithTag(CollectorScreenTags.ExportDialog).assertIsDisplayed()
        composeRule.onNodeWithTag(CollectorScreenTags.ExportCsv).assertIsDisplayed()
        composeRule.onNodeWithTag(CollectorScreenTags.ExportTxt).assertIsDisplayed()

        composeRule.onNodeWithTag(CollectorScreenTags.ExportCsv).performClick()

        assertEquals(ExportFormat.CSV, selectedFormat)
    }

    private fun sampleUiState(): CollectorUiState {
        return CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS02",
            connectionState = BluetoothConnectionState.DISCONNECTED,
            currentSession = Session(
                sessionId = "session-1",
                startedAt = "2026-03-31T10:00:00+08:00",
                updatedAt = "2026-03-31T10:00:00+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS02",
                bluetoothDeviceName = "Paired Leica",
                bluetoothDeviceAddress = "00:11:22:33:44:55",
                delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
                isCurrent = true,
            ),
            previewRecords = listOf(
                MeasurementRecord(
                    id = "record-1",
                    sequence = 1,
                    receivedAt = "2026-03-31T10:00:01+08:00",
                    instrumentBrand = "leica",
                    instrumentModel = "TS02",
                    bluetoothDeviceName = "Paired Leica",
                    bluetoothDeviceAddress = "00:11:22:33:44:55",
                    rawPayload = "01123.456",
                    parsedCode = "01",
                    parsedValue = "123.456",
                ),
            ),
            receivedCount = 1,
        )
    }
}
