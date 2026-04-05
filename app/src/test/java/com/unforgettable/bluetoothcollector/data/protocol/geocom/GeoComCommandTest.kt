package com.unforgettable.bluetoothcollector.data.protocol.geocom

import java.nio.charset.StandardCharsets.US_ASCII
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoComCommandTest {

    @Test
    fun getSimpleMeasurement_formats_ascii_request_with_crlf() {
        val request = GeoComCommand.GetSimpleMeasurement.toRequest()

        assertEquals("%R1Q,2108:1,1\r\n", request)
        assertArrayEquals(request.toByteArray(US_ASCII), request.toByteArray())
    }

    @Test
    fun getCoordinate_formats_ascii_request_with_crlf() {
        val request = GeoComCommand.GetCoordinate.toRequest()

        assertEquals("%R1Q,2082:1,1\r\n", request)
        assertArrayEquals(request.toByteArray(US_ASCII), request.toByteArray())
    }
}
