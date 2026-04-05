package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoComCsvExportWriterTest {

    @Test
    fun writes_geocom_angles_in_rad_deg_and_gon_columns() {
        val writer = GeoComCsvExportWriter()
        val directory = Files.createTempDirectory("geocom-csv-export").toFile()
        val exported = writer.write(
            directory = directory,
            session = sampleSession(),
            records = listOf(
                MeasurementRecord(
                    id = "row-1",
                    sequence = 1,
                    receivedAt = "2026-03-31T10:00:01+08:00",
                    instrumentBrand = "leica",
                    instrumentModel = "TS60",
                    bluetoothDeviceName = "Leica TS60",
                    bluetoothDeviceAddress = "00:11:22:33:44:55",
                    rawPayload = "%R1P,0,0:0,3.141592653589793,1.5707963267948966,12.345",
                    parsedCode = "GEOCOM",
                    parsedValue = "3.141592653589793,1.5707963267948966,12.345",
                    protocolType = "GEOCOM",
                    hzAngleRad = 3.141592653589793,
                    vAngleRad = 1.5707963267948966,
                    slopeDistanceM = 12.345,
                ),
            ),
            exportedAt = OffsetDateTime.parse("2026-03-31T10:05:00+08:00"),
        )

        val lines = exported.readText(UTF_8).trim().lines()

        assertEquals(
            "id,sequence,received_at,instrument_brand,instrument_model,bluetooth_device_name," +
                "bluetooth_device_address,raw_payload,parsed_code,parsed_value,protocol_type," +
                "hz_angle_rad,hz_angle_deg,hz_angle_gon,v_angle_rad,v_angle_deg,v_angle_gon," +
                "slope_distance_m,coordinate_e,coordinate_n,coordinate_h",
            lines.first(),
        )
        assertTrue(lines[1].contains(",GEOCOM,3.141592653589793,180.0,200.0,1.5707963267948966,90.0,100.0,12.345,,,"))
    }

    private fun sampleSession(): Session {
        return Session(
            sessionId = "session-1",
            startedAt = "2026-03-31T10:00:00+08:00",
            updatedAt = "2026-03-31T10:01:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS60",
            bluetoothDeviceName = "Leica TS60",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            isCurrent = true,
        )
    }
}
