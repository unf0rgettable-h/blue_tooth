package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat

@Composable
fun ExportFormatDialog(
    onSelect: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择导出格式")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "当前会话会先落盘到手机，再通过系统分享发送。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(
                    onClick = { onSelect(ExportFormat.CSV) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CollectorScreenTags.ExportCsv),
                ) {
                    Text(text = "CSV")
                }
                FilledTonalButton(
                    onClick = { onSelect(ExportFormat.TXT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CollectorScreenTags.ExportTxt),
                ) {
                    Text(text = "TXT 原始日志")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = onDismiss,
            ) {
                Text(text = "取消")
            }
        },
    )
}
