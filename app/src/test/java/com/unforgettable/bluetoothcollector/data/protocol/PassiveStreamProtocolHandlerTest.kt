package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.data.bluetooth.TextStreamRecordParser
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassiveStreamProtocolHandlerTest {

    @Test
    fun protocolHandler_stopSession_is_synchronous() {
        val method = ProtocolHandler::class.java.methods.single { it.name == "stopSession" }

        assertEquals(0, method.parameterCount)
    }

    @Test
    fun startSession_wraps_gsi_stream_into_measurement_records() = runTest {
        val handler = PassiveStreamProtocolHandler(
            transport = SingleReadProtocolTransport("01123.456\n".toByteArray()),
            parser = TextStreamRecordParser(),
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            session = sampleSession(),
            startingSequence = 0L,
            timeProvider = { "2026-03-31T10:00:01+08:00" },
            idProvider = { "record-1" },
        )

        val record = handler.startSession().first()

        assertEquals("record-1", record.id)
        assertEquals(1L, record.sequence)
        assertEquals("GSI", record.protocolType)
        assertEquals("01", record.parsedCode)
        assertEquals("123.456", record.parsedValue)
    }

    @Test
    fun triggerSingleMeasurement_returns_null_for_passive_handler() = runTest {
        val handler = PassiveStreamProtocolHandler(
            transport = SingleReadProtocolTransport("01123.456\n".toByteArray()),
            parser = TextStreamRecordParser(),
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            session = sampleSession(),
            startingSequence = 0L,
            timeProvider = { "2026-03-31T10:00:01+08:00" },
            idProvider = { "record-1" },
        )

        val record = handler.triggerSingleMeasurement()

        assertEquals(null, record)
    }
}

private class SingleReadProtocolTransport(
    private val payload: ByteArray,
) : ProtocolTransport {
    private var consumed = false

    override suspend fun sendBytes(data: ByteArray) = Unit

    override suspend fun blockingReadBytes(): ByteArray {
        return if (consumed) ByteArray(0) else payload.also { consumed = true }
    }

    override suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray? = blockingReadBytes()
}

private fun sampleSession(): Session {
    return Session(
        sessionId = "session-1",
        startedAt = "2026-03-31T10:00:00+08:00",
        updatedAt = "2026-03-31T10:00:00+08:00",
        instrumentBrand = "leica",
        instrumentModel = "TS02",
        bluetoothDeviceName = "Leica TS02",
        bluetoothDeviceAddress = "00:11:22:33:44:55",
        delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        isCurrent = true,
    )
}
