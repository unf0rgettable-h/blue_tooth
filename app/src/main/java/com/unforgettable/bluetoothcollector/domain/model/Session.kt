package com.unforgettable.bluetoothcollector.domain.model

data class Session(
    val sessionId: String,
    val startedAt: String,
    val updatedAt: String,
    val instrumentBrand: String,
    val instrumentModel: String,
    val bluetoothDeviceName: String,
    val bluetoothDeviceAddress: String,
    val delimiterStrategy: DelimiterStrategy,
    val isCurrent: Boolean,
)
