package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothDiscoveryManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionChecker: BluetoothPermissionChecker,
) {
    private val mutableIsDiscovering = MutableStateFlow(currentAdapterDiscoveryState(bluetoothAdapter))
    private val mutableDiscoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDeviceItem>>(emptyList())

    val isDiscovering: StateFlow<Boolean> = mutableIsDiscovering
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDeviceItem>> = mutableDiscoveredDevices

    private val discoveredMap = linkedMapOf<String, DiscoveredBluetoothDeviceItem>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        val permissionState = permissionChecker.currentState()
                        discoveredMap[device.address] = DiscoveredBluetoothDeviceItem(
                            name = if (permissionState.canConnect) safeDeviceName(device) else null,
                            address = device.address,
                            bondState = if (permissionState.canConnect) {
                                safeBondState(device)
                            } else {
                                BluetoothPermissionChecker.LEGACY_DISCOVERY_BOND_STATE_UNKNOWN
                            },
                        )
                        mutableDiscoveredDevices.value = discoveredMap.values.toList()
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    mutableIsDiscovering.value = false
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    mutableIsDiscovering.value = true
                }
            }
        }
    }

    fun register() {
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
    }

    fun unregister() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(connectionState: BluetoothConnectionState): Boolean {
        val policyDecision = BluetoothSessionPolicy.canStartDiscovery(connectionState)
        if (!policyDecision.allowed) {
            mutableIsDiscovering.value = false
            return false
        }
        val permissionDecision = BluetoothSessionPolicy.discoveryAvailability(permissionChecker.currentState())
        if (!permissionDecision.allowed) {
            mutableIsDiscovering.value = false
            return false
        }
        if (currentAdapterDiscoveryState(bluetoothAdapter)) {
            mutableIsDiscovering.value = true
            return false
        }
        if (mutableIsDiscovering.value && !cancelDiscovery()) {
            return false
        }
        if (!mutableIsDiscovering.value) {
            discoveredMap.clear()
            mutableDiscoveredDevices.value = emptyList()
        }
        val started = bluetoothAdapter?.startDiscovery() == true
        mutableIsDiscovering.value = started
        return started
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery(): Boolean {
        val adapter = bluetoothAdapter
        val isDiscovering = runCatching { adapter?.isDiscovering == true }.getOrDefault(false)
        if (!isDiscovering) {
            mutableIsDiscovering.value = false
            return true
        }
        if (!permissionChecker.currentState().canDiscover) {
            return false
        }
        val cancelled = runCatching {
            adapter?.cancelDiscovery() == true
        }.getOrDefault(false)
        if (cancelled) {
            mutableIsDiscovering.value = false
        }
        return cancelled
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return runCatching { device.name }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun safeBondState(device: BluetoothDevice): Int {
        return runCatching { device.bondState }
            .getOrDefault(BluetoothPermissionChecker.LEGACY_DISCOVERY_BOND_STATE_UNKNOWN)
    }

    fun handleAdapterStateChanged(state: Int): Boolean {
        return if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
            discoveredMap.clear()
            mutableDiscoveredDevices.value = emptyList()
            mutableIsDiscovering.value = false
            true
        } else {
            false
        }
    }

    companion object {
        @SuppressLint("MissingPermission")
        private fun currentAdapterDiscoveryState(adapter: BluetoothAdapter?): Boolean {
            return runCatching { adapter?.isDiscovering == true }.getOrDefault(false)
        }
    }
}
