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
    fun failed_exposes_structured_reason_code() {
        val state = ReceiverState.Failed(
            code = ReceiverDiagnosticCode.RFCOMM_SERVER_OPEN_FAILED,
            detail = "socket unavailable",
        )

        assertEquals(ReceiverDiagnosticCode.RFCOMM_SERVER_OPEN_FAILED, state.code)
        assertEquals("rfcomm_server_open_failed", state.reason)
        assertEquals("socket unavailable", state.detail)
    }

    @Test
    fun diagnostic_entry_keeps_agent_readable_code_severity_and_message() {
        val entry = ReceiverDiagnosticEntry(
            code = ReceiverDiagnosticCode.NO_INCOMING_CONNECTION,
            severity = ReceiverDiagnosticSeverity.ERROR,
            message = "未观察到 TS60 主动连接 Android RFCOMM 服务",
        )

        assertEquals(ReceiverDiagnosticCode.NO_INCOMING_CONNECTION, entry.code)
        assertEquals(ReceiverDiagnosticSeverity.ERROR, entry.severity)
        assertEquals("未观察到 TS60 主动连接 Android RFCOMM 服务", entry.message)
    }
}
