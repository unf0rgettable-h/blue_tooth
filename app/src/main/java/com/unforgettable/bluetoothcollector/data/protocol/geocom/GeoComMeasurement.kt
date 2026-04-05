package com.unforgettable.bluetoothcollector.data.protocol.geocom

import kotlin.math.PI

data class GeoComMeasurement(
    val hzAngleRad: Double,
    val vAngleRad: Double,
    val slopeDistanceM: Double,
    val coordinateE: Double? = null,
    val coordinateN: Double? = null,
    val coordinateH: Double? = null,
) {
    val hzAngleDeg: Double
        get() = Math.toDegrees(hzAngleRad)

    val hzAngleGon: Double
        get() = hzAngleRad * 200.0 / PI

    val vAngleDeg: Double
        get() = Math.toDegrees(vAngleRad)

    val vAngleGon: Double
        get() = vAngleRad * 200.0 / PI

    companion object {
        fun fromResponse(response: GeoComResponse): GeoComMeasurement? {
            if (response.parameters.size < 3) return null
            return GeoComMeasurement(
                hzAngleRad = response.parameters[0].toDoubleOrNull() ?: return null,
                vAngleRad = response.parameters[1].toDoubleOrNull() ?: return null,
                slopeDistanceM = response.parameters[2].toDoubleOrNull() ?: return null,
                coordinateE = response.parameters.getOrNull(3)?.toDoubleOrNull(),
                coordinateN = response.parameters.getOrNull(4)?.toDoubleOrNull(),
                coordinateH = response.parameters.getOrNull(5)?.toDoubleOrNull(),
            )
        }
    }
}
