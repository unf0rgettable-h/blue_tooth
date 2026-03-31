package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.time.OffsetDateTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportWriterTest {

    @Test
    fun writes_utf8_csv_with_required_columns_quoting_and_crlf() {
        val writer = CsvExportWriter()
        val directory = Files.createTempDirectory("csv-export").toFile()
        val session = sampleSession()
        val exportedFile = writer.write(
            directory = directory,
            session = session,
            records = listOf(
                MeasurementRecord(
                    id = "row-1",
                    sequence = 1,
                    receivedAt = "2026-03-31T10:00:01+08:00",
                    instrumentBrand = "徕卡",
                    instrumentModel = "TS02",
                    bluetoothDeviceName = "Leica, \"TS02\"",
                    bluetoothDeviceAddress = "00:11:22:33:44:55",
                    rawPayload = "RAW, \"payload\"",
                    parsedCode = "01",
                    parsedValue = "123.456",
                ),
            ),
            exportedAt = OffsetDateTime.parse("2026-03-31T10:05:00+08:00"),
        )

        val expected = (
            "id,sequence,received_at,instrument_brand,instrument_model," +
                "bluetooth_device_name,bluetooth_device_address,raw_payload,parsed_code,parsed_value\r\n" +
                "row-1,1,2026-03-31T10:00:01+08:00,徕卡,TS02,\"Leica, \"\"TS02\"\"\",00:11:22:33:44:55," +
                "\"RAW, \"\"payload\"\"\",01,123.456\r\n"
            ).toByteArray(UTF_8)

        assertArrayEquals(expected, exportedFile.readBytes())
    }

    @Test
    fun uses_timestamped_unique_file_names_without_overwriting_existing_export() {
        val writer = CsvExportWriter()
        val directory = Files.createTempDirectory("csv-export").toFile()
        val session = sampleSession(sessionId = "session-1")
        val exportedAt = OffsetDateTime.parse("2026-03-31T10:05:00+08:00")

        val first = writer.write(
            directory = directory,
            session = session,
            records = listOf(sampleRecord(id = "row-1", sequence = 1, rawPayload = "first")),
            exportedAt = exportedAt,
        )
        val second = writer.write(
            directory = directory,
            session = session,
            records = listOf(sampleRecord(id = "row-2", sequence = 2, rawPayload = "second")),
            exportedAt = exportedAt,
        )

        assertTrue(first.name.contains("20260331T100500+0800"))
        assertTrue(second.name.contains("20260331T100500+0800"))
        assertNotEquals(first.absolutePath, second.absolutePath)
        assertTrue(first.readText(UTF_8).contains("row-1"))
        assertTrue(second.readText(UTF_8).contains("row-2"))
    }

    @Test
    fun failed_write_cleans_reserved_file_and_preserves_preferred_name_for_retry() {
        val directory = Files.createTempDirectory("csv-export").toFile()
        val session = sampleSession(sessionId = "session-1")
        val exportedAt = OffsetDateTime.parse("2026-03-31T10:05:00+08:00")
        val failingWriter = CsvExportWriter(
            exportFileNamer = ExportFileNamer(
                fileWriter = { _, _ -> throw IOException("disk full") },
            ),
        )

        val failure = runCatching {
            failingWriter.write(
                directory = directory,
                session = session,
                records = listOf(sampleRecord(id = "row-1", sequence = 1, rawPayload = "first")),
                exportedAt = exportedAt,
            )
        }

        assertTrue(failure.isFailure)
        assertFalse(File(directory, "session-session-1-20260331T100500+0800.csv").exists())
        assertEquals(0, directory.listFiles()?.size ?: 0)

        val retry = CsvExportWriter().write(
            directory = directory,
            session = session,
            records = listOf(sampleRecord(id = "row-2", sequence = 2, rawPayload = "retry")),
            exportedAt = exportedAt,
        )

        assertEquals("session-session-1-20260331T100500+0800.csv", retry.name)
        assertTrue(retry.readText(UTF_8).contains("row-2"))
    }

    private fun sampleSession(sessionId: String = "session-1"): Session {
        return Session(
            sessionId = sessionId,
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

    private fun sampleRecord(
        id: String,
        sequence: Long,
        rawPayload: String,
    ): MeasurementRecord {
        return MeasurementRecord(
            id = id,
            sequence = sequence,
            receivedAt = "2026-03-31T10:00:01+08:00",
            instrumentBrand = "徕卡",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            rawPayload = rawPayload,
            parsedCode = "01",
            parsedValue = "123.456",
        )
    }
}
