package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BondedDeviceManager(
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    private val mutableBondedDevices = MutableStateFlow<List<BondedBluetoothDeviceItem>>(emptyList())
    val bondedDevices: StateFlow<List<BondedBluetoothDeviceItem>> = mutableBondedDevices

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices(): List<BondedBluetoothDeviceItem> {
        val bondedDevices = bluetoothAdapter?.bondedDevices.orEmpty()
        val refreshed = bondedDevices
            .map { device ->
                BondedBluetoothDeviceItem(
                    name = device.name,
                    address = device.address,
                )
            }
            .sortedBy { it.name ?: it.address }
        mutableBondedDevices.value = refreshed
        return refreshed
    }

    fun findSavedTarget(address: String?): BondedBluetoothDeviceItem? {
        if (address.isNullOrBlank()) return null
        return bondedDevices.value.firstOrNull { it.address == address } ?: refreshBondedDevices().firstOrNull { it.address == address }
    }
}
