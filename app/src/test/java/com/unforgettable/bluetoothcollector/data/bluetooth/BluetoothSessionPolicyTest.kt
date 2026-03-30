package com.unforgettable.bluetoothcollector.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothSessionPolicyTest {

    @Test
    fun `discovery allowed only while disconnected`() {
        assertTrue(BluetoothSessionPolicy.canStartDiscovery(BluetoothConnectionState.DISCONNECTED).allowed)
        assertFalse(BluetoothSessionPolicy.canStartDiscovery(BluetoothConnectionState.CONNECTED).allowed)
    }

    @Test
    fun `connect requires bonded device`() {
        val decision = BluetoothSessionPolicy.canConnect(
            BluetoothDeviceSnapshot(address = "00:11:22:33:44:55", isBonded = false),
        )
        assertFalse(decision.allowed)
        assertEquals("connect_requires_bonded_device", decision.reason)
    }

    @Test
    fun `same session reconnect requires same bonded device`() {
        val allowed = BluetoothSessionPolicy.canReconnectSameSession(
            currentSessionDeviceAddress = "00:11:22:33:44:55",
            targetDevice = BluetoothDeviceSnapshot(address = "00:11:22:33:44:55", isBonded = true),
        )
        assertTrue(allowed.allowed)
    }

    @Test
    fun `selected device can re-enter pairing without session clear`() {
        val decision = BluetoothSessionPolicy.canRePairSelectedDevice(
            currentSessionDeviceAddress = "00:11:22:33:44:55",
            targetDeviceAddress = "00:11:22:33:44:55",
        )
        assertTrue(decision.allowed)
    }

    @Test
    fun `clear current session refused while connected`() {
        val decision = BluetoothSessionPolicy.canClearCurrentSession(BluetoothConnectionState.CONNECTED)
        assertFalse(decision.allowed)
        assertEquals("clear_requires_disconnected_state", decision.reason)
    }

    @Test
    fun `discovery unavailable while connected even when permission exists`() {
        val decision = BluetoothSessionPolicy.canStartDiscovery(BluetoothConnectionState.CONNECTED)
        assertFalse(decision.allowed)
    }

    @Test
    fun `discovery can be blocked while connect remains available`() {
        val permissionState = BluetoothPermissionState(
            canDiscover = false,
            canConnect = true,
            bluetoothEnabled = true,
        )
        assertFalse(BluetoothSessionPolicy.discoveryAvailability(permissionState).allowed)
        assertTrue(BluetoothSessionPolicy.bondedConnectAvailability(permissionState).allowed)
    }

    @Test
    fun `connect can be blocked while discovery remains available`() {
        val permissionState = BluetoothPermissionState(
            canDiscover = true,
            canConnect = false,
            bluetoothEnabled = true,
        )
        assertTrue(BluetoothSessionPolicy.discoveryAvailability(permissionState).allowed)
        assertFalse(BluetoothSessionPolicy.bondedConnectAvailability(permissionState).allowed)
    }
}
