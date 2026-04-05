package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface ProtocolHandler {
    fun startSession(): Flow<MeasurementRecord>

    suspend fun triggerSingleMeasurement(): MeasurementRecord?

    fun stopSession()
}

interface ProtocolHandlerFactory {
    fun create(
        model: InstrumentModel,
        session: Session,
        startingSequence: Long,
        timeProvider: () -> String,
        onOverflow: () -> Unit = {},
    ): ProtocolHandler
}
