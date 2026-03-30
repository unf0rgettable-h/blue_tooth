package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem

class BondedDeviceManager(
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    @SuppressLint("MissingPermission")
    fun reloadBondedDevices(): List<BondedBluetoothDeviceItem> {
        val bondedDevices = bluetoothAdapter?.bondedDevices.orEmpty()
        return bondedDevices
            .map { device ->
                BondedBluetoothDeviceItem(
                    name = device.name,
                    address = device.address,
                )
            }
            .sortedBy { it.name ?: it.address }
    }

    fun findSavedTarget(address: String?): BondedBluetoothDeviceItem? {
        if (address.isNullOrBlank()) return null
        return reloadBondedDevices().firstOrNull { it.address == address }
    }
}
