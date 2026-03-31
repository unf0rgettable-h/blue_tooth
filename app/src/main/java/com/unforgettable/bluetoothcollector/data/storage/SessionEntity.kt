package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAt: String,
    val updatedAt: String,
    val instrumentBrand: String,
    val instrumentModel: String,
    val bluetoothDeviceName: String,
    val bluetoothDeviceAddress: String,
    val delimiterStrategy: DelimiterStrategy,
    val isCurrent: Boolean,
)
