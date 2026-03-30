package com.unforgettable.bluetoothcollector.data.bluetooth

enum class BluetoothConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

data class BluetoothPermissionState(
    val canDiscover: Boolean,
    val canConnect: Boolean,
    val bluetoothEnabled: Boolean,
)

data class BluetoothDeviceSnapshot(
    val address: String,
    val isBonded: Boolean,
)

data class BluetoothActionDecision(
    val allowed: Boolean,
    val reason: String? = null,
)

object BluetoothSessionPolicy {

    fun canStartDiscovery(connectionState: BluetoothConnectionState): BluetoothActionDecision {
        return if (connectionState == BluetoothConnectionState.DISCONNECTED) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "discovery_requires_disconnected_state")
        }
    }

    fun canClearCurrentSession(connectionState: BluetoothConnectionState): BluetoothActionDecision {
        return if (connectionState == BluetoothConnectionState.DISCONNECTED) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "clear_requires_disconnected_state")
        }
    }

    fun canConnect(device: BluetoothDeviceSnapshot): BluetoothActionDecision {
        return if (device.isBonded) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "connect_requires_bonded_device")
        }
    }

    fun canReconnectSameSession(
        currentSessionDeviceAddress: String?,
        targetDevice: BluetoothDeviceSnapshot,
    ): BluetoothActionDecision {
        return if (currentSessionDeviceAddress != null &&
            currentSessionDeviceAddress == targetDevice.address &&
            targetDevice.isBonded
        ) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "same_session_reconnect_requires_same_bonded_device")
        }
    }

    fun canRePairSelectedDevice(
        currentSessionDeviceAddress: String?,
        targetDeviceAddress: String,
    ): BluetoothActionDecision {
        return if (currentSessionDeviceAddress != null && currentSessionDeviceAddress == targetDeviceAddress) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "repair_requires_same_session_device")
        }
    }

    fun discoveryAvailability(permissionState: BluetoothPermissionState): BluetoothActionDecision {
        if (!permissionState.bluetoothEnabled) {
            return BluetoothActionDecision(allowed = false, reason = "bluetooth_disabled")
        }
        return if (permissionState.canDiscover) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "missing_discovery_permission")
        }
    }

    fun bondedConnectAvailability(permissionState: BluetoothPermissionState): BluetoothActionDecision {
        if (!permissionState.bluetoothEnabled) {
            return BluetoothActionDecision(allowed = false, reason = "bluetooth_disabled")
        }
        return if (permissionState.canConnect) {
            BluetoothActionDecision(allowed = true)
        } else {
            BluetoothActionDecision(allowed = false, reason = "missing_connect_permission")
        }
    }
}
