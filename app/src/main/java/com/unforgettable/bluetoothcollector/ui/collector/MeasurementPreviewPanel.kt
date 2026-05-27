package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord

/**
 * 测量记录预览列表。
 *
 * 预览渲染和数据控制区分离，避免后续 agent 修改导入操作时误碰记录展示逻辑。
 */
@Composable
internal fun PreviewPanel(records: List<MeasurementRecord>) {
    PanelCard(
        modifier = Modifier.testTag(CollectorScreenTags.PreviewSection),
        title = "实时/离线预览",
        subtitle = "按接收顺序展示原始测量文本和轻量解析结果。",
    ) {
        if (records.isEmpty()) {
            EmptyPlaceholder(text = "当前没有接收到测量记录。")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(records, key = MeasurementRecord::id) { record ->
                    PreviewRow(record = record)
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(record: MeasurementRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${record.sequence}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = record.receivedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    text = record.rawPayload,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (record.parsedCode != null || record.parsedValue != null) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = "解析：${record.parsedCode.orEmpty()} ${record.parsedValue.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (record.hzAngleRad != null) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                Spacer(modifier = Modifier.height(6.dp))
                GeoComMeasurementDisplay(measurement = record)
            }
        }
    }
}

@Composable
internal fun ImportedFilePanel(
    fileInfo: ImportedFileInfo?,
    onShareFile: () -> Unit,
) {
    PanelCard(
        modifier = Modifier.testTag(CollectorScreenTags.ImportedFilePanel),
        title = "已导入文件",
        subtitle = "从仪器接收的原始数据文件。",
    ) {
        if (fileInfo == null) {
            EmptyPlaceholder(text = "暂无导入文件")
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = fileInfo.file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "格式：${fileInfo.format.displayName}　大小：${formatFileSize(fileInfo.sizeBytes)}　时间：${fileInfo.receivedAt.take(19)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onShareFile) {
                            Text(text = "分享文件")
                        }
                    }
                }
            }
        }
    }
}
