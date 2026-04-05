package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComCommand
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComCommandClient
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComMeasurement
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComResponse
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GeoComProtocolHandler(
    private val client: GeoComCommandClient,
    private val session: Session,
    private val startingSequence: Long,
    private val timeProvider: () -> String,
    private val idProvider: () -> String = { "${session.sessionId}-${UUID.randomUUID()}" },
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MS,
) : ProtocolHandler {
    private val sequenceMutex = Mutex()

    @Volatile
    private var isSessionActive: Boolean = true
    private var nextSequence: Long = startingSequence

    override fun startSession(): Flow<MeasurementRecord> = flow {
        isSessionActive = true
        while (isSessionActive && currentCoroutineContext().isActive) {
            val record = fetchMeasurementRecord()
            if (record != null) {
                emit(record)
            }
            delay(pollIntervalMillis)
        }
    }

    override suspend fun triggerSingleMeasurement(): MeasurementRecord? {
        return if (isSessionActive) {
            fetchMeasurementRecord()
        } else {
            null
        }
    }

    override fun stopSession() {
        isSessionActive = false
    }

    private suspend fun fetchMeasurementRecord(): MeasurementRecord? {
        val response = client.sendCommand(GeoComCommand.GetSimpleMeasurement)
        if (response?.isSuccessful != true) return null

        val measurement = GeoComMeasurement.fromResponse(response) ?: return null
        return buildMeasurementRecord(response, measurement)
    }

    private suspend fun buildMeasurementRecord(
        response: GeoComResponse,
        measurement: GeoComMeasurement,
    ): MeasurementRecord = sequenceMutex.withLock {
        nextSequence += 1
        MeasurementRecord(
            id = idProvider(),
            sequence = nextSequence,
            receivedAt = timeProvider(),
            instrumentBrand = session.instrumentBrand,
            instrumentModel = session.instrumentModel,
            bluetoothDeviceName = session.bluetoothDeviceName,
            bluetoothDeviceAddress = session.bluetoothDeviceAddress,
            rawPayload = response.rawPayload,
            parsedCode = PROTOCOL_TYPE_GEOCOM,
            parsedValue = listOf(
                measurement.hzAngleRad,
                measurement.vAngleRad,
                measurement.slopeDistanceM,
            ).joinToString(separator = ","),
            protocolType = PROTOCOL_TYPE_GEOCOM,
            hzAngleRad = measurement.hzAngleRad,
            vAngleRad = measurement.vAngleRad,
            slopeDistanceM = measurement.slopeDistanceM,
            coordinateE = measurement.coordinateE,
            coordinateN = measurement.coordinateN,
            coordinateH = measurement.coordinateH,
        )
    }

    companion object {
        const val PROTOCOL_TYPE_GEOCOM: String = "GEOCOM"
        private const val DEFAULT_POLL_INTERVAL_MS: Long = 2_000L
    }
}
