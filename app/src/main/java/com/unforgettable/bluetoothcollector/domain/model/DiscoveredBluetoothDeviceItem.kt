package com.unforgettable.bluetoothcollector.domain.model

data class DiscoveredBluetoothDeviceItem(
    val name: String?,
    val address: String,
    val bondState: Int,
)
