package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import org.junit.Assert.assertFalse
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
    }
}
