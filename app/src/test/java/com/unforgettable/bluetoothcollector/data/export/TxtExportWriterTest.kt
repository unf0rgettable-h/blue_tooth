package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class TxtExportWriterTest {

    @Test
    fun prepends_utf8_bom_to_reduce_chinese_garbled_text_in_common_text_viewers() {
        val writer = TxtExportWriter()
        val directory = Files.createTempDirectory("txt-export-bom").toFile()
        val exportedFile = writer.write(
            directory = directory,
            session = sampleSession(),
            records = listOf(sampleRecord(sequence = 1, rawPayload = "中文原始记录")),
            exportedAt = OffsetDateTime.parse("2026-03-31T10:15:00+08:00"),
        )

        val bytes = exportedFile.readBytes()

        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        assertTrue(String(bytes, StandardCharsets.UTF_8).contains("中文原始记录"))
    }

    @Test
    fun writes_header_with_session_and_device_metadata_and_payloads_in_receive_order() {
        val writer = TxtExportWriter()
        val directory = Files.createTempDirectory("txt-export").toFile()
        val exportedFile = writer.write(
            directory = directory,
            session = sampleSession(),
            records = listOf(
                sampleRecord(sequence = 2, rawPayload = "SECOND"),
                sampleRecord(sequence = 1, rawPayload = "FIRST"),
            ),
            exportedAt = OffsetDateTime.parse("2026-03-31T10:15:00+08:00"),
        )

        val expected = """
            Session start time: 2026-03-31T10:00:00+08:00
            Instrument brand/model: 徕卡 / TS02
            Bluetooth device name/address: Leica TS02 / 00:11:22:33:44:55
            Export time: 2026-03-31T10:15:00+08:00
            
            Raw payload:
            FIRST
            SECOND
            
        """.trimIndent().replace("\n", "\r\n").toByteArray(UTF_8)
        val expectedWithBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + expected

        assertEquals(expectedWithBom.decodeToString(), exportedFile.readText(UTF_8))
        assertTrue(exportedFile.readText(UTF_8).indexOf("FIRST") < exportedFile.readText(UTF_8).indexOf("SECOND"))
    }

    private fun sampleSession(): Session {
        return Session(
            sessionId = "session-1",
            startedAt = "2026-03-31T10:00:00+08:00",
            updatedAt = "2026-03-31T10:01:00+08:00",
            instrumentBrand = "徕卡",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            isCurrent = true,
        )
    }

    private fun sampleRecord(sequence: Long, rawPayload: String): MeasurementRecord {
        return MeasurementRecord(
            id = "row-$sequence",
            sequence = sequence,
            receivedAt = "2026-03-31T10:00:0${sequence}+08:00",
            instrumentBrand = "徕卡",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            rawPayload = rawPayload,
            parsedCode = null,
            parsedValue = null,
        )
    }
}
