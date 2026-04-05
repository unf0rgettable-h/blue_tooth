package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.Session
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultProtocolHandlerFactoryTest {

    @Test
    fun create_returns_geocom_handler_for_captivate_models() {
        val factory = DefaultProtocolHandlerFactory(transport = NoOpProtocolTransport())

        val handler = factory.create(
            model = InstrumentModel(
                modelId = "TS60",
                brandId = "leica",
                displayName = "TS60",
                delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
                firmwareFamily = "Captivate",
            ),
            session = sampleSession(modelId = "TS60"),
            startingSequence = 0L,
            timeProvider = { "2026-03-31T10:00:01+08:00" },
        )

        assertTrue(handler is GeoComProtocolHandler)
    }

    @Test
    fun create_returns_passive_handler_for_flexLine_models() {
        val factory = DefaultProtocolHandlerFactory(transport = NoOpProtocolTransport())

        val handler = factory.create(
            model = InstrumentModel(
                modelId = "TS02",
                brandId = "leica",
                displayName = "TS02",
                delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
                firmwareFamily = "FlexLine",
            ),
            session = sampleSession(modelId = "TS02"),
            startingSequence = 0L,
            timeProvider = { "2026-03-31T10:00:01+08:00" },
        )

        assertTrue(handler is PassiveStreamProtocolHandler)
    }
}

private class NoOpProtocolTransport : ProtocolTransport {
    override suspend fun sendBytes(data: ByteArray) = Unit

    override suspend fun blockingReadBytes(): ByteArray = ByteArray(0)

    override suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray? = ByteArray(0)
}

private fun sampleSession(modelId: String): Session {
    return Session(
        sessionId = "session-1",
        startedAt = "2026-03-31T10:00:00+08:00",
        updatedAt = "2026-03-31T10:00:00+08:00",
        instrumentBrand = "leica",
        instrumentModel = modelId,
        bluetoothDeviceName = "Leica $modelId",
        bluetoothDeviceAddress = "00:11:22:33:44:55",
        delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        isCurrent = true,
    )
}
