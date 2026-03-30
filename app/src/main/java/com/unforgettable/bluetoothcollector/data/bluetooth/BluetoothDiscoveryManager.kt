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
    private val mutableIsDiscovering = MutableStateFlow(false)
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
                            name = if (permissionState.canConnect) device.name else null,
                            address = device.address,
                            bondState = if (permissionState.canConnect) {
                                device.bondState
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
            }
        }
    }

    fun register() {
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
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
        cancelDiscovery()
        discoveredMap.clear()
        mutableDiscoveredDevices.value = emptyList()
        val started = bluetoothAdapter?.startDiscovery() == true
        mutableIsDiscovering.value = started
        return started
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        mutableIsDiscovering.value = false
    }
}
