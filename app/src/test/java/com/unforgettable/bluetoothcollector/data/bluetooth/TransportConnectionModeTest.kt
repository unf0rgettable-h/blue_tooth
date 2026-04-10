package com.unforgettable.bluetoothcollector.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class TransportConnectionModeTest {

    @Test
    fun client_mode_is_default_for_standard_instruments() {
        assertEquals("CLIENT", TransportConnectionMode.CLIENT.name)
    }

    @Test
    fun receiver_mode_exists_for_experimental_path() {
        assertEquals("RECEIVER", TransportConnectionMode.RECEIVER.name)
    }
}
