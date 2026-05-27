package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord

enum class AngleUnit(val displayName: String) {
    DEG("Deg(°)"),
    RAD("Rad"),
    GON("Gon"),
}

/**
 * GeoCOM 测量结果数值区。
 *
 * 每个值独立成 metric tile，便于 agent 直接定位角度/距离显示，不需要解析一整行文本。
 */
@Composable
fun GeoComMeasurementDisplay(
    measurement: MeasurementRecord,
    modifier: Modifier = Modifier,
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

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "角度单位",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeasurementMetricTile(
                    label = "水平角",
                    value = formatAngle(measurement.hzAngleRad),
                    modifier = Modifier.weight(1f),
                )
                MeasurementMetricTile(
                    label = "垂直角",
                    value = formatAngle(measurement.vAngleRad),
                    modifier = Modifier.weight(1f),
                )
            }
            measurement.slopeDistanceM?.let { distance ->
                MeasurementMetricTile(
                    label = "斜距",
                    value = "${String.format("%.3f", distance)}m",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MeasurementMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
