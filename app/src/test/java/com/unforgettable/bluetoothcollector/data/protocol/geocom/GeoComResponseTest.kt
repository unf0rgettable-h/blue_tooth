package com.unforgettable.bluetoothcollector.data.protocol.geocom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoComResponseTest {

    @Test
    fun parse_extracts_transaction_return_codes_and_parameters() {
        val response = GeoComResponse.parse("%R1P,0,7:0,1.0,2.0,3.0\r\n")

        requireNotNull(response)
        assertEquals(0, response.communicationReturnCode)
        assertEquals(7, response.transactionId)
        assertEquals(0, response.returnCode)
        assertEquals(listOf("1.0", "2.0", "3.0"), response.parameters)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun parse_rejects_non_zero_rcCom() {
        val response = GeoComResponse.parse("%R1P,8,7:0,1.0,2.0,3.0\r\n")

        assertNull(response)
    }

    @Test
    fun parse_marks_command_failure_even_when_transport_is_ok() {
        val response = GeoComResponse.parse("%R1P,0,4:1283\r\n")

        requireNotNull(response)
        assertEquals(1283, response.returnCode)
        assertFalse(response.isSuccessful)
    }
}
