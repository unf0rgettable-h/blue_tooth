package com.unforgettable.bluetoothcollector.ui.collector

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import com.unforgettable.bluetoothcollector.ui.theme.CollectorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Screen-scope rendering contract tests. They validate the Compose surface, not the Route wiring.
 */
class CollectorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_nearby_and_paired_device_sections() {
        composeRule.setContent {
            CollectorTheme {
                CollectorScreen(
                    uiState = sampleUiState(),
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
                    onExportRequested = {},
                    onExportFormatSelected = {},
                    onDismissExportDialog = {},
                )
            }
        }

        composeRule.onNodeWithTag(CollectorScreenTags.NearbySection).assertIsDisplayed()
        composeRule.onNodeWithTag(CollectorScreenTags.PairedSection).assertIsDisplayed()
    }

    @Test
    fun restored_preview_is_visible_while_connection_stays_disconnected() {
        composeRule.setContent {
            CollectorTheme {
                CollectorScreen(
                    uiState = sampleUiState(),
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
                    onExportRequested = {},
                    onExportFormatSelected = {},
                    onDismissExportDialog = {},
                )
            }
        }

        composeRule.onNodeWithText("连接 未连接").assertIsDisplayed()
        composeRule.onNodeWithText("01123.456").assertIsDisplayed()
    }

    private fun sampleUiState(): CollectorUiState {
        return CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS02",
            nearbyDevices = listOf(
                DiscoveredBluetoothDeviceItem(
                    name = "Nearby Leica",
                    address = "66:77:88:99:AA:BB",
                    bondState = 0,
                ),
            ),
            pairedDevices = listOf(
                BondedBluetoothDeviceItem(
                    name = "Paired Leica",
                    address = "00:11:22:33:44:55",
                ),
            ),
            selectedTargetDeviceAddress = "00:11:22:33:44:55",
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
