package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PairingRequestCoordinator(
    private val context: Context,
    private val bondedDeviceManager: BondedDeviceManager,
) {
    private val mutableBondedAddresses = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val bondedAddresses: SharedFlow<String> = mutableBondedAddresses

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                if (device != null && bondState == BluetoothDevice.BOND_BONDED) {
                    bondedDeviceManager.refreshBondedDevices()
                    mutableBondedAddresses.tryEmit(device.address)
                }
            }
        }
    }

    fun register() {
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
    }

    fun unregister() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission")
    fun requestBond(
        device: BluetoothDevice,
        currentSessionDeviceAddress: String? = null,
    ): Boolean {
        val rePairDecision = BluetoothSessionPolicy.canRePairSelectedDevice(
            currentSessionDeviceAddress = currentSessionDeviceAddress,
            targetDeviceAddress = device.address,
        )
        if (currentSessionDeviceAddress != null && !rePairDecision.allowed) {
            return false
        }
        return device.createBond()
    }
}
