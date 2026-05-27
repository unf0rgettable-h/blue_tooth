package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverDiagnosticEntry
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverDiagnosticSeverity
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState

/**
 * TS60 Android 蓝牙实验监听诊断面板。
 *
 * 只展示结构化诊断结果；失败归因的 code 由 ViewModel/ReceiverManager 负责生成。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReceiverModePanel(
    receiverState: ReceiverState,
    isReceiverDiscoverable: Boolean,
    receiverDiagnostics: List<ReceiverDiagnosticEntry>,
    showReceiverMode: Boolean,
    onStartReceiver: () -> Unit,
    onStopReceiver: () -> Unit,
) {
    if (!showReceiverMode && receiverState is ReceiverState.Idle) return

    PanelCard(
        title = "实验性接收模式",
        subtitle = "TS60/Captivate：手机作为蓝牙服务端，等待仪器主动连入并发送数据。此功能尚未经实地验证。",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "状态：${receiverState.toDisplayText()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "可发现性：${if (isReceiverDiscoverable) "已开启" else "未开启"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReceiverDiscoverable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (receiverState is ReceiverState.Receiving) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已接收 ${receiverState.bytesReceived} 字节",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (receiverState is ReceiverState.Failed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "错误：${receiverState.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (receiverDiagnostics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "诊断日志：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    receiverDiagnostics.takeLast(5).forEach { line ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• ${line.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = line.severity.toDiagnosticColor(),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onStartReceiver,
                enabled = receiverState is ReceiverState.Idle ||
                    receiverState is ReceiverState.Completed ||
                    receiverState is ReceiverState.Failed ||
                    receiverState is ReceiverState.Cancelled,
            ) {
                Text(text = "开始监听")
            }
            OutlinedButton(
                onClick = onStopReceiver,
                enabled = receiverState is ReceiverState.Listening ||
                    receiverState is ReceiverState.Receiving ||
                    receiverState is ReceiverState.RequestingDiscoverability,
            ) {
                Text(text = "停止监听")
            }
        }
    }
}

/**
 * 诊断级别到 UI 颜色的唯一映射。
 *
 * 让 UI 真正消费 ReceiverDiagnosticSeverity，避免 severity 只停留在测试数据中。
 */
@Composable
private fun ReceiverDiagnosticSeverity.toDiagnosticColor(): Color {
    return when (this) {
        ReceiverDiagnosticSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        ReceiverDiagnosticSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        ReceiverDiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
        ReceiverDiagnosticSeverity.SUCCESS -> MaterialTheme.colorScheme.primary
    }
}

internal fun ReceiverState.toDisplayText(): String {
    return when (this) {
        is ReceiverState.Idle -> "空闲"
        is ReceiverState.RequestingDiscoverability -> "请求可发现性..."
        is ReceiverState.Listening -> "等待仪器连接..."
        is ReceiverState.Receiving -> "正在接收数据..."
        is ReceiverState.Completed -> "接收完成（${bytesReceived} 字节）"
        is ReceiverState.Failed -> "失败"
        is ReceiverState.Cancelled -> "已取消"
    }
}
