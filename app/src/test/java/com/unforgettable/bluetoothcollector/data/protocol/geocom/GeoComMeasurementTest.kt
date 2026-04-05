package com.unforgettable.bluetoothcollector.data.protocol.geocom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeoComMeasurementTest {

    @Test
    fun fromResponse_parses_angles_and_exposes_degree_and_gon_conversions() {
        val response = GeoComResponse.parse("%R1P,0,0:0,3.141592653589793,1.5707963267948966,12.345\r\n")

        val measurement = GeoComMeasurement.fromResponse(requireNotNull(response))

        assertNotNull(measurement)
        measurement ?: error("missing measurement")
        assertEquals(180.0, measurement.hzAngleDeg, 1e-9)
        assertEquals(200.0, measurement.hzAngleGon, 1e-9)
        assertEquals(90.0, measurement.vAngleDeg, 1e-9)
        assertEquals(100.0, measurement.vAngleGon, 1e-9)
        assertEquals(12.345, measurement.slopeDistanceM, 1e-9)
    }
}
