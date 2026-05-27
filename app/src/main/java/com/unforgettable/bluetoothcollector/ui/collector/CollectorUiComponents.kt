package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState

/**
 * collector 页面共享卡片外壳。
 *
 * 该组件是 UI 重构的稳定边界：页面区域只描述 title/subtitle/content，不重复写 card chrome。
 */
@Composable
internal fun PanelCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 统一下拉选择器。
 *
 * 保持 optionId/displayName 显式传入，方便 coding agents 看清 UI 与 domain model 的映射。
 */
@Composable
internal fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selectedId: String?,
    enabled: Boolean,
    displayName: (T) -> String,
    optionId: (T) -> String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { optionId(it) == selectedId }?.let(displayName) ?: "请选择"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = { expanded = true },
        ) {
            Text(
                text = selectedLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 240.dp, max = 320.dp)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = displayName(option)) },
                    onClick = {
                        expanded = false
                        onSelect(optionId(option))
                    },
                )
            }
        }
    }
}

@Composable
internal fun EmptyPlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun StatusChip(label: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = label) },
        leadingIcon = {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        },
    )
}

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
    }
}

internal fun BluetoothConnectionState.toDisplayText(): String {
    return when (this) {
        BluetoothConnectionState.DISCONNECTED -> "未连接"
        BluetoothConnectionState.CONNECTING -> "连接中"
        BluetoothConnectionState.CONNECTED -> "已连接"
    }
}

internal fun buildPermissionSummary(uiState: CollectorUiState): String {
    val parts = buildList {
        if (!uiState.permissionState.bluetoothEnabled) add("蓝牙未开启")
        if (!uiState.permissionState.canDiscover) add("缺少搜索权限")
        if (!uiState.permissionState.canConnect) add("缺少连接权限")
        if (!uiState.permissionState.canAdvertise) add("缺少广播/可发现权限")
    }
    return parts.joinToString(separator = " / ")
}
