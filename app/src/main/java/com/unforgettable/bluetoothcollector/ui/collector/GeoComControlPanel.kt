package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Captivate/GeoCOM 测量控制条。
 *
 * 按钮文案使用中文稳定标签，测试和 agents 可以通过标签定位行为；
 * 图标只强化视觉，不作为状态机事实来源。
 */
@Composable
fun GeoComControlPanel(
    isConnected: Boolean,
    isMeasuring: Boolean,
    onStartStopClick: () -> Unit,
    onSingleMeasureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier.heightIn(min = 44.dp),
            onClick = onStartStopClick,
            enabled = isConnected,
        ) {
            if (isMeasuring) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (isMeasuring) "停止测量" else "自动测量")
        }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 44.dp),
            onClick = onSingleMeasureClick,
            enabled = isConnected && !isMeasuring,
        ) {
            Icon(imageVector = if (isMeasuring) Icons.Filled.Stop else Icons.Filled.GpsFixed, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("单次测量")
        }
    }
}
