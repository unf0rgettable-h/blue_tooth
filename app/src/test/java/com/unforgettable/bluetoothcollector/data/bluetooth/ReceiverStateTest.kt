package com.unforgettable.bluetoothcollector.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverStateTest {

    @Test
    fun idle_is_the_default_state() {
        val state: ReceiverState = ReceiverState.Idle
        assertTrue(state is ReceiverState.Idle)
    }

    @Test
    fun receiving_tracks_byte_count() {
        val state = ReceiverState.Receiving(bytesReceived = 4096)
        assertEquals(4096L, state.bytesReceived)
    }

    @Test
    fun completed_exposes_filename_and_byte_count() {
        val state = ReceiverState.Completed(bytesReceived = 12345, fileName = "receiver-20260410.gsi")
        assertEquals(12345L, state.bytesReceived)
        assertEquals("receiver-20260410.gsi", state.fileName)
    }

    @Test
    fun failed_exposes_reason() {
        val state = ReceiverState.Failed(reason = "rfcomm_server_open_failed")
        assertEquals("rfcomm_server_open_failed", state.reason)
    }
}
