package com.unforgettable.bluetoothcollector.data.protocol

import com.unforgettable.bluetoothcollector.data.bluetooth.TextStreamRecordParser
import com.unforgettable.bluetoothcollector.data.protocol.geocom.GeoComClient
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.Session

class DefaultProtocolHandlerFactory(
    private val transport: ProtocolTransport,
) : ProtocolHandlerFactory {
    override fun create(
        model: InstrumentModel,
        session: Session,
        startingSequence: Long,
        timeProvider: () -> String,
        onOverflow: () -> Unit,
    ): ProtocolHandler {
        return if (model.firmwareFamily == CAPTIVATE_FIRMWARE_FAMILY) {
            GeoComProtocolHandler(
                client = GeoComClient(transport),
                session = session,
                startingSequence = startingSequence,
                timeProvider = timeProvider,
            )
        } else {
            PassiveStreamProtocolHandler(
                transport = transport,
                parser = TextStreamRecordParser(),
                delimiterStrategy = model.delimiterStrategy,
                session = session,
                startingSequence = startingSequence,
                timeProvider = timeProvider,
                onOverflow = onOverflow,
            )
        }
    }

    companion object {
        private const val CAPTIVATE_FIRMWARE_FAMILY: String = "Captivate"
    }
}
