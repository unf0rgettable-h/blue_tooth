package com.unforgettable.bluetoothcollector.domain.model

data class MeasurementRecord(
    val id: String,
    val sequence: Long,
    val receivedAt: String,
    val instrumentBrand: String,
    val instrumentModel: String,
    val bluetoothDeviceName: String,
    val bluetoothDeviceAddress: String,
    val rawPayload: String,
    val parsedCode: String?,
    val parsedValue: String?,
    val protocolType: String? = null,
    val hzAngleRad: Double? = null,
    val vAngleRad: Double? = null,
    val slopeDistanceM: Double? = null,
    val coordinateE: Double? = null,
    val coordinateN: Double? = null,
    val coordinateH: Double? = null,
)
