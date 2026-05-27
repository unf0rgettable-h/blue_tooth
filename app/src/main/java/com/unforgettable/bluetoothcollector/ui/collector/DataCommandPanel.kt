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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode

/**
 * 数据接收、导入、保存命令区域。
 *
 * 该组件消费 CollectorUiState 的派生能力，不直接判断 TS60 字符串。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DataActionPanel(
    uiState: CollectorUiState,
    onStartReceivingRequested: () -> Unit,
    onStopReceivingRequested: () -> Unit,
    onSingleMeasureRequested: () -> Unit,
    onStartImportRequested: () -> Unit,
    onStartReceiverRequested: () -> Unit,
    onClearRequested: () -> Unit,
    onExportRequested: () -> Unit,
    onSaveToLocalRequested: () -> Unit,
) {
    val importProfile = uiState.currentImportProfile()
    val isGeoCom = uiState.usesCaptivateProtocol()

    PanelCard(
        title = "数据控制",
        subtitle = "实时接收、批量导入、清空、导出与保存都留在数据页面。",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "当前协议路径：${importProfile.protocolSummary}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = importProfile.guidanceMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        TransferRouteSummary(uiState = uiState)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isGeoCom) {
                GeoComControlPanel(
                    isConnected = uiState.connectionState == BluetoothConnectionState.CONNECTED,
                    isMeasuring = uiState.isReceiving && !uiState.isImporting,
                    onStartStopClick = {
                        if (uiState.isReceiving) onStopReceivingRequested() else onStartReceivingRequested()
                    },
                    onSingleMeasureClick = onSingleMeasureRequested,
                )
            } else {
                FilledTonalButton(
                    onClick = onStartReceivingRequested,
                    enabled = uiState.connectionState == BluetoothConnectionState.CONNECTED && !uiState.isReceiving,
                ) {
                    Text(text = importProfile.liveReceiveLabel)
                }
            }
            FilledTonalButton(
                onClick = {
                    when (importProfile.executionMode) {
                        ImportExecutionMode.CLIENT_STREAM -> onStartImportRequested()
                        ImportExecutionMode.RECEIVER_STREAM -> onStartReceiverRequested()
                        ImportExecutionMode.GUIDANCE_ONLY -> onStartImportRequested()
                    }
                },
                enabled = uiState.canStartPrimaryImportAction(),
            ) {
                Text(text = uiState.primaryImportActionLabel())
            }
            if (!isGeoCom || uiState.isImporting) {
                OutlinedButton(
                    onClick = onStopReceivingRequested,
                    enabled = uiState.isReceiving,
                ) {
                    Text(text = if (uiState.isImporting) "中止导入" else "停止接收")
                }
            }
            OutlinedButton(
                onClick = onClearRequested,
                enabled = uiState.connectionState == BluetoothConnectionState.DISCONNECTED,
            ) {
                Text(text = "清空数据")
            }
            if (uiState.currentSession != null && uiState.previewRecords.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onExportRequested,
                    enabled = true,
                ) {
                    Text(text = "导出当前记录")
                }
            }
            if ((uiState.currentSession != null && uiState.previewRecords.isNotEmpty()) ||
                uiState.importedFileInfo != null
            ) {
                OutlinedButton(
                    onClick = onSaveToLocalRequested,
                    enabled = true,
                ) {
                    Text(text = "保存到本地")
                }
            }
        }
    }
}

/**
 * 当前仪器的数据连接能力摘要。
 *
 * route 标签来自 ImportProfile capability，避免 UI 使用 TS60/TS09 字符串分支。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferRouteSummary(uiState: CollectorUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "推荐连接",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.recommendedTransferRouteLabels().forEach { label ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        uiState.experimentalTransferRouteLabel()?.let { label ->
            Text(
                text = "实验监听",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
