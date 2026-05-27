package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.ftp.FtpReceiveState

/**
 * TS60 WLAN/FTP 项目接收面板。
 *
 * 该面板只展示 FTP channel，不处理 TS09 蓝牙导入，避免两个 channel 的状态和操作混在一起。
 */
@Composable
internal fun FtpProjectTransferPanel(
    uiState: CollectorUiState,
    onStartFtpReceive: () -> Unit,
    onStopFtpReceive: () -> Unit,
) {
    if (!uiState.usesFtpProjectTransferMode()) return

    val runningState = uiState.ftpReceiveState as? FtpReceiveState.Running
    PanelCard(
        modifier = Modifier.testTag(CollectorScreenTags.FtpProjectTransferPanel),
        title = "TS60 WLAN项目接收",
        subtitle = "手机开启热点后，让 TS60 通过 Captivate FTP data transfer 上传完整项目文件。",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = uiState.ftpEndpointText ?: "FTP服务未启动",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "用户名：${runningState?.username ?: "survlink"}　密码：${runningState?.password ?: "启动后显示"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "TS60 操作", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "1. 手机开启热点，TS60 连接该热点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Settings > Tools > FTP data transfer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "3. 在 TS60 输入上方 FTP 地址、用户名和密码后上传 job/dbx/csv/xml/dxf/zip。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val receivedFiles = uiState.ftpReceivedFiles
            Text(
                text = "已接收：${receivedFiles.size}个文件，${formatFileSize(receivedFiles.sumOf { it.sizeBytes })}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (receivedFiles.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.height(110.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(receivedFiles, key = { it.relativePath }) { file ->
                        Text(
                            text = "${file.relativePath}　${formatFileSize(file.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onStartFtpReceive,
                    enabled = runningState == null,
                ) {
                    Text(text = "启动FTP接收")
                }
                OutlinedButton(
                    onClick = onStopFtpReceive,
                    enabled = runningState != null,
                ) {
                    Text(text = "停止并打包项目")
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
