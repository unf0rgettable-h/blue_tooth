package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class CsvExportWriter(
    private val exportFileNamer: ExportFileNamer = ExportFileNamer(),
) {

    fun write(
        directory: File,
        session: Session,
        records: List<MeasurementRecord>,
        exportedAt: OffsetDateTime,
    ): File {
        return exportFileNamer.writeExportFile(
            directory = directory,
            sessionId = session.sessionId,
            format = ExportFormat.CSV,
            exportedAt = exportedAt,
            content = buildCsv(records).toByteArray(UTF_8),
        )
    }

    private fun buildCsv(records: List<MeasurementRecord>): String {
        val header = listOf(
            "id",
            "sequence",
            "received_at",
            "instrument_brand",
            "instrument_model",
            "bluetooth_device_name",
            "bluetooth_device_address",
            "raw_payload",
            "parsed_code",
            "parsed_value",
        ).joinToString(separator = ",")
        val rows = records
            .sortedBy { it.sequence }
            .map { record ->
                listOf(
                    record.id,
                    record.sequence.toString(),
                    formatTimestamp(record.receivedAt),
                    record.instrumentBrand,
                    record.instrumentModel,
                    record.bluetoothDeviceName,
                    record.bluetoothDeviceAddress,
                    record.rawPayload,
                    record.parsedCode.orEmpty(),
                    record.parsedValue.orEmpty(),
                ).joinToString(separator = ",", transform = ::escapeCsv)
            }

        return buildList {
            add(header)
            addAll(rows)
        }.joinToString(separator = CRLF, postfix = CRLF)
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return OffsetDateTime.parse(timestamp).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private companion object {
        const val CRLF = "\r\n"
    }
}
