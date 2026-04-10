package com.unforgettable.bluetoothcollector.data.import_

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportExecutionModeTest {

    @Test
    fun client_stream_is_the_standard_import_mode() {
        assertEquals("CLIENT_STREAM", ImportExecutionMode.CLIENT_STREAM.name)
    }

    @Test
    fun receiver_stream_is_the_experimental_mode() {
        assertEquals("RECEIVER_STREAM", ImportExecutionMode.RECEIVER_STREAM.name)
    }

    @Test
    fun guidance_only_blocks_import_execution() {
        assertEquals("GUIDANCE_ONLY", ImportExecutionMode.GUIDANCE_ONLY.name)
    }
}
