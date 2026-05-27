package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem

/**
 * 蓝牙设备列表区域。
 *
 * 设备选择和配对 UI 与连接命令拆开，便于 agents 分别修改列表和操作按钮。
 */
@Composable
internal fun BluetoothPanel(
    uiState: CollectorUiState,
    selectionLocked: Boolean,
    onTargetDeviceSelected: (String) -> Unit,
    onPairDeviceRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelCard(
        modifier = modifier,
        title = "蓝牙设备",
        subtitle = "右侧分成附近设备与已配对设备。附近设备可先配对，已配对设备可作为连接目标。",
    ) {
        DeviceSection(
            modifier = Modifier.testTag(CollectorScreenTags.NearbySection),
            title = "附近设备",
            devices = uiState.nearbyDevices,
            selectedAddress = uiState.selectedTargetDeviceAddress,
            pairedAddresses = uiState.pairedDevices.map(BondedBluetoothDeviceItem::address).toSet(),
            onSelect = if (selectionLocked) ({}) else onTargetDeviceSelected,
            onAction = onPairDeviceRequested,
            actionLabel = { paired -> if (paired) "已配对" else "配对" },
            actionEnabled = { address, paired ->
                !paired &&
                    uiState.permissionState.canConnect &&
                    (
                        !selectionLocked ||
                            address == uiState.selectedTargetDeviceAddress ||
                            address == uiState.currentSession?.bluetoothDeviceAddress
                        )
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        DeviceSection(
            modifier = Modifier.testTag(CollectorScreenTags.PairedSection),
            title = "已配对设备",
            devices = uiState.pairedDevices,
            selectedAddress = uiState.selectedTargetDeviceAddress,
            pairedAddresses = uiState.pairedDevices.map(BondedBluetoothDeviceItem::address).toSet(),
            onSelect = if (selectionLocked) ({}) else onTargetDeviceSelected,
            onAction = if (selectionLocked) ({}) else onTargetDeviceSelected,
            actionLabel = { "选中" },
            actionEnabled = { _, _ -> !selectionLocked },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BluetoothActionPanel(
    uiState: CollectorUiState,
    onDiscoveryRequested: () -> Unit,
    onStopDiscoveryRequested: () -> Unit,
    onConnectRequested: () -> Unit,
    onDisconnectRequested: () -> Unit,
) {
    PanelCard(
        title = "蓝牙控制",
        subtitle = "搜索、配对、连接、断开都留在蓝牙页面。",
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onDiscoveryRequested,
                enabled = uiState.permissionState.canDiscover &&
                    uiState.permissionState.bluetoothEnabled &&
                    uiState.connectionState == BluetoothConnectionState.DISCONNECTED &&
                    !uiState.isDiscovering,
            ) {
                Text(text = "搜索蓝牙")
            }
            OutlinedButton(
                onClick = onStopDiscoveryRequested,
                enabled = uiState.isDiscovering,
            ) {
                Text(text = "停止搜索")
            }
            FilledTonalButton(
                onClick = onConnectRequested,
                enabled = uiState.permissionState.canConnect &&
                    uiState.permissionState.bluetoothEnabled &&
                    uiState.selectedTargetDeviceAddress != null &&
                    uiState.connectionState == BluetoothConnectionState.DISCONNECTED,
            ) {
                Text(text = "连接")
            }
            OutlinedButton(
                onClick = onDisconnectRequested,
                enabled = uiState.connectionState != BluetoothConnectionState.DISCONNECTED,
            ) {
                Text(text = "断开")
            }
        }
    }
}

@Composable
private fun <T> DeviceSection(
    title: String,
    devices: List<T>,
    selectedAddress: String?,
    pairedAddresses: Set<String>,
    onSelect: (String) -> Unit,
    onAction: (String) -> Unit,
    actionLabel: (Boolean) -> String,
    actionEnabled: (String, Boolean) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (devices.isEmpty()) {
            EmptyPlaceholder(text = "暂无设备")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEach { item ->
                    val name = when (item) {
                        is DiscoveredBluetoothDeviceItem -> item.name
                        is BondedBluetoothDeviceItem -> item.name
                        else -> null
                    }.orEmpty().ifBlank { "未命名设备" }
                    val address = when (item) {
                        is DiscoveredBluetoothDeviceItem -> item.address
                        is BondedBluetoothDeviceItem -> item.address
                        else -> ""
                    }
                    val paired = pairedAddresses.contains(address)
                    val selected = selectedAddress == address
                    DeviceRow(
                        name = name,
                        address = address,
                        selected = selected,
                        actionLabel = actionLabel(paired),
                        actionEnabled = actionEnabled(address, paired),
                        onSelect = { onSelect(address) },
                        onAction = { onAction(address) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    address: String,
    selected: Boolean,
    actionLabel: String,
    actionEnabled: Boolean,
    onSelect: () -> Unit,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onAction,
                enabled = actionEnabled,
            ) {
                Text(text = actionLabel)
            }
        }
    }
}
