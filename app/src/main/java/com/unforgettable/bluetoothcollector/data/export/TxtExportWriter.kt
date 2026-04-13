package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class TxtExportWriter(
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
            format = ExportFormat.TXT,
            exportedAt = exportedAt,
            content = ExportEncoding.utf8WithBom(buildTxt(session, records, exportedAt)),
        )
    }

    private fun buildTxt(
        session: Session,
        records: List<MeasurementRecord>,
        exportedAt: OffsetDateTime,
    ): String {
        val lines = mutableListOf(
            "Session start time: ${formatTimestamp(session.startedAt)}",
            "Instrument brand/model: ${session.instrumentBrand} / ${session.instrumentModel}",
            "Bluetooth device name/address: ${session.bluetoothDeviceName} / ${session.bluetoothDeviceAddress}",
            "Export time: ${exportedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}",
            "",
            "Raw payload:",
        )
        records.sortedBy { it.sequence }.forEach { record ->
            lines += record.rawPayload
        }
        lines += ""
        return lines.joinToString(separator = CRLF)
    }

    private fun formatTimestamp(timestamp: String): String {
        return OffsetDateTime.parse(timestamp).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private companion object {
        const val CRLF = "\r\n"
    }
}
