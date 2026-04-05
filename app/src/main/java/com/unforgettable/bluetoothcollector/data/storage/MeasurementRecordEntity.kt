package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_records",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class MeasurementRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
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
