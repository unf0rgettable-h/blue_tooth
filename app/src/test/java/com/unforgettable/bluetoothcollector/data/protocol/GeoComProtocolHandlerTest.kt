package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComCommand
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComCommandClient
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComResponse
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeoComProtocolHandlerTest {

    @Test
    fun startSession_polls_client_and_emits_measurement_record() = runTest {
        val handler = GeoComProtocolHandler(
            client = FakeGeoComCommandClient(
                responses = ArrayDeque(
                    listOf(
                        GeoComResponse.parse("%R1P,0,0:0,1.0,2.0,3.0\r\n")!!,
                    ),
                ),
            ),
            session = sampleSession(),
            startingSequence = 5L,
            timeProvider = { "2026-03-31T10:00:01+08:00" },
            idProvider = { "record-6" },
            pollIntervalMillis = 0L,
        )

        val record = handler.startSession().first()

        assertEquals("record-6", record.id)
        assertEquals(6L, record.sequence)
        assertEquals("GEOCOM", record.protocolType)
        assertEquals("GEOCOM", record.parsedCode)
        assertNotNull(record.hzAngleRad)
        assertEquals(1.0, record.hzAngleRad ?: error("missing hzAngleRad"), 0.0)
        assertEquals(2.0, record.vAngleRad ?: error("missing vAngleRad"), 0.0)
        assertEquals(3.0, record.slopeDistanceM ?: error("missing slopeDistanceM"), 0.0)
    }

    @Test
    fun triggerSingleMeasurement_returns_record_for_successful_response() = runTest {
        val handler = GeoComProtocolHandler(
            client = FakeGeoComCommandClient(
                responses = ArrayDeque(
                    listOf(
                        GeoComResponse.parse("%R1P,0,0:0,4.0,5.0,6.0\r\n")!!,
                    ),
                ),
            ),
            session = sampleSession(),
            startingSequence = 1L,
            timeProvider = { "2026-03-31T10:00:02+08:00" },
            idProvider = { "record-2" },
            pollIntervalMillis = 0L,
        )

        val record = handler.triggerSingleMeasurement()

        requireNotNull(record)
        assertEquals("record-2", record.id)
        assertEquals(2L, record.sequence)
        assertEquals("GEOCOM", record.protocolType)
        assertEquals(4.0, record.hzAngleRad ?: error("missing hzAngleRad"), 0.0)
        assertEquals(5.0, record.vAngleRad ?: error("missing vAngleRad"), 0.0)
        assertEquals(6.0, record.slopeDistanceM ?: error("missing slopeDistanceM"), 0.0)
    }
}

private class FakeGeoComCommandClient(
    private val responses: ArrayDeque<GeoComResponse>,
) : GeoComCommandClient {
    override suspend fun sendCommand(command: GeoComCommand): GeoComResponse? {
        return responses.removeFirstOrNull()
    }
}

private fun sampleSession(): Session {
    return Session(
        sessionId = "session-1",
        startedAt = "2026-03-31T10:00:00+08:00",
        updatedAt = "2026-03-31T10:00:00+08:00",
        instrumentBrand = "leica",
        instrumentModel = "TS60",
        bluetoothDeviceName = "Leica TS60",
        bluetoothDeviceAddress = "00:11:22:33:44:55",
        delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        isCurrent = true,
    )
}
