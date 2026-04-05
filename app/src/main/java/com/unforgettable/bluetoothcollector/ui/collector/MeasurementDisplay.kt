package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord

enum class AngleUnit(val displayName: String) {
    DEG("Deg(°)"),
    RAD("Rad"),
    GON("Gon")
}

@Composable
fun GeoComMeasurementDisplay(
    measurement: MeasurementRecord,
    modifier: Modifier = Modifier
) {
    if (measurement.hzAngleRad != null && measurement.vAngleRad != null) {
        var currentUnit by remember { mutableStateOf(AngleUnit.DEG) }

        val formatAngle = { rad: Double ->
            when (currentUnit) {
                AngleUnit.DEG -> String.format("%.4f°", Math.toDegrees(rad))
                AngleUnit.RAD -> String.format("%.4f rad", rad)
                AngleUnit.GON -> String.format("%.4f gon", rad * 200.0 / Math.PI)
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Angle Format:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentUnit.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        currentUnit = when (currentUnit) {
                            AngleUnit.DEG -> AngleUnit.RAD
                            AngleUnit.RAD -> AngleUnit.GON
                            AngleUnit.GON -> AngleUnit.DEG
                        }
                    }
                )
            }
            Text(
                text = "Hz: ${formatAngle(measurement.hzAngleRad)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "V: ${formatAngle(measurement.vAngleRad)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (measurement.slopeDistanceM != null) {
                Text(
                    text = "Dist: ${String.format("%.3f", measurement.slopeDistanceM)}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
