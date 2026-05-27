package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.import_.TransferConfidence
import com.unforgettable.bluetoothcollector.data.import_.TransferRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorUiStateTest {

    @Test
    fun ts09_primary_import_action_requires_connected_client_transport() {
        val disconnected = CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS09",
            connectionState = BluetoothConnectionState.DISCONNECTED,
        )
        val connected = disconnected.copy(
            connectionState = BluetoothConnectionState.CONNECTED,
        )

        assertFalse(disconnected.canStartPrimaryImportAction())
        assertTrue(connected.canStartPrimaryImportAction())
    }

    @Test
    fun ts60_primary_import_action_uses_receiver_state_instead_of_connection_state() {
        val ready = CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS60",
            connectionState = BluetoothConnectionState.DISCONNECTED,
            receiverState = ReceiverState.Idle,
        )
        val listening = ready.copy(receiverState = ReceiverState.Listening)

        assertTrue(ready.usesReceiverImportMode())
        assertTrue(ready.canStartPrimaryImportAction())
        assertFalse(listening.canStartPrimaryImportAction())
        assertEquals(
            listOf(
                TransferRoute.GEOCOM_WLAN,
                TransferRoute.CABLE_RS232,
                TransferRoute.USB_CABLE,
            ),
            ready.currentImportProfile().capability.recommendedRoutes.map { it.route },
        )
        assertEquals(
            TransferConfidence.EXPERIMENTAL_DIAGNOSTIC,
            ready.currentImportProfile().capability.experimentalRoutes.single().confidence,
        )
    }

    @Test
    fun ts60_state_exposes_captivate_transfer_guidance_for_ui_without_ts60_string_branching() {
        val state = CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS60",
        )

        assertTrue(state.usesCaptivateProtocol())
        assertEquals(
            listOf("GeoCOM WLAN", "Cable RS232", "USB Cable"),
            state.recommendedTransferRouteLabels(),
        )
        assertEquals("Android蓝牙实验监听", state.experimentalTransferRouteLabel())
        assertEquals("查看TS60连接方案", state.primaryImportActionLabel())
    }

    @Test
    fun ts09_state_keeps_verified_bluetooth_guidance() {
        val state = CollectorUiState(
            selectedBrandId = "leica",
            selectedModelId = "TS09",
        )

        assertFalse(state.usesCaptivateProtocol())
        assertEquals(listOf("经典蓝牙导入"), state.recommendedTransferRouteLabels())
        assertEquals(null, state.experimentalTransferRouteLabel())
        assertEquals("导入存储数据", state.primaryImportActionLabel())
    }
}
