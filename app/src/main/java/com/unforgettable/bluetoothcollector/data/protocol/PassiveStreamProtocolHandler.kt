package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.data.bluetooth.TextStreamRecordParser
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.nio.charset.Charset
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class PassiveStreamProtocolHandler(
    private val transport: ProtocolTransport,
    private val parser: TextStreamRecordParser,
    private val delimiterStrategy: DelimiterStrategy,
    private val session: Session,
    private val startingSequence: Long,
    private val dataCharset: Charset = Charset.forName("GBK"),
    private val timeProvider: () -> String,
    private val idProvider: () -> String = { "${session.sessionId}-${UUID.randomUUID()}" },
    private val onOverflow: () -> Unit = {},
) : ProtocolHandler {
    @Volatile
    private var isSessionActive: Boolean = true

    override fun startSession(): Flow<MeasurementRecord> = flow {
        isSessionActive = true
        var nextSequence = startingSequence
        while (isSessionActive && currentCoroutineContext().isActive) {
            val incoming = transport.blockingReadBytesWithTimeout(READ_TIMEOUT_MS)
                ?: continue
            if (incoming.isEmpty()) continue

            val parseResult = parser.accept(
                chunk = incoming.toString(dataCharset),
                delimiterStrategy = delimiterStrategy,
            )
            if (parseResult.overflowed) {
                onOverflow()
            }
            parseResult.completed.forEach { parsedRecord ->
                nextSequence += 1
                emit(
                    MeasurementRecord(
                        id = idProvider(),
                        sequence = nextSequence,
                        receivedAt = timeProvider(),
                        instrumentBrand = session.instrumentBrand,
                        instrumentModel = session.instrumentModel,
                        bluetoothDeviceName = session.bluetoothDeviceName,
                        bluetoothDeviceAddress = session.bluetoothDeviceAddress,
                        rawPayload = parsedRecord.rawPayload,
                        parsedCode = parsedRecord.parsedCode,
                        parsedValue = parsedRecord.parsedValue,
                        protocolType = PROTOCOL_TYPE_GSI,
                    ),
                )
            }
        }
    }

    override suspend fun triggerSingleMeasurement(): MeasurementRecord? = null

    override fun stopSession() {
        isSessionActive = false
        parser.dropIncompleteFragment()
    }

    companion object {
        const val PROTOCOL_TYPE_GSI: String = "GSI"
        private const val READ_TIMEOUT_MS: Long = 1_000L
    }
}
