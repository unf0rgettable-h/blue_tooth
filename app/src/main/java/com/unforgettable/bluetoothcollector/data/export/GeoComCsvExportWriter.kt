package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI

class GeoComCsvExportWriter(
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
            "protocol_type",
            "hz_angle_rad",
            "hz_angle_deg",
            "hz_angle_gon",
            "v_angle_rad",
            "v_angle_deg",
            "v_angle_gon",
            "slope_distance_m",
            "coordinate_e",
            "coordinate_n",
            "coordinate_h",
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
                    record.protocolType.orEmpty(),
                    formatDouble(record.hzAngleRad),
                    formatDouble(record.hzAngleRad?.let(Math::toDegrees)),
                    formatDouble(record.hzAngleRad?.let { it * 200.0 / PI }),
                    formatDouble(record.vAngleRad),
                    formatDouble(record.vAngleRad?.let(Math::toDegrees)),
                    formatDouble(record.vAngleRad?.let { it * 200.0 / PI }),
                    formatDouble(record.slopeDistanceM),
                    formatDouble(record.coordinateE),
                    formatDouble(record.coordinateN),
                    formatDouble(record.coordinateH),
                ).joinToString(separator = ",", transform = ::escapeCsv)
            }

        return buildList {
            add(header)
            addAll(rows)
        }.joinToString(separator = CRLF, postfix = CRLF)
    }

    private fun formatDouble(value: Double?): String = value?.toString().orEmpty()

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
