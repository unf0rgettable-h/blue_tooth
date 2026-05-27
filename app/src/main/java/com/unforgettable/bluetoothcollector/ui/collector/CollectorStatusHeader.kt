package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 页面顶部状态总览。
 *
 * 这里只消费 CollectorUiState 的派生结果，不做蓝牙或协议状态机决策。
 */
@Composable
internal fun CollectorStatusHeader(uiState: CollectorUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SurvLink",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "现场即连即传即看。只保留仪器选择、蓝牙连接、实时预览、导出与分享。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = "连接 ${uiState.connectionState.toDisplayText()}")
                StatusChip(
                    label = when {
                        uiState.isImporting -> "导入中..."
                        uiState.isReceiving -> "接收中"
                        else -> "未接收"
                    },
                )
                StatusChip(label = "已接收 ${uiState.receivedCount} 条")
            }
            if (!uiState.permissionState.bluetoothEnabled || !uiState.permissionState.canDiscover || !uiState.permissionState.canConnect) {
                Text(
                    text = buildPermissionSummary(uiState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            uiState.statusMessage?.let { message ->
                Text(
                    text = "状态提示：$message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
