package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo
import com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.InstrumentBrand
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord

object CollectorScreenTags {
    const val NearbySection = "nearby_section"
    const val PairedSection = "paired_section"
    const val PreviewSection = "preview_section"
    const val ImportedFilePanel = "imported_file_panel"
    const val BottomNavBluetooth = "bottom_nav_bluetooth"
    const val BottomNavData = "bottom_nav_data"
    const val ExportDialog = "export_dialog"
    const val ExportCsv = "export_csv"
    const val ExportTxt = "export_txt"
}

@Composable
fun CollectorScreen(
    uiState: CollectorUiState,
    onInstrumentBrandSelected: (String) -> Unit,
    onInstrumentModelSelected: (String) -> Unit,
    onTargetDeviceSelected: (String) -> Unit,
    onDiscoveryRequested: () -> Unit,
    onStopDiscoveryRequested: () -> Unit,
    onPairDeviceRequested: (String) -> Unit,
    onConnectRequested: () -> Unit,
    onDisconnectRequested: () -> Unit,
    onStartReceivingRequested: () -> Unit,
    onStopReceivingRequested: () -> Unit,
    onSingleMeasureRequested: () -> Unit,
    onStartImportRequested: () -> Unit,
    onStartReceiverRequested: () -> Unit,
    onStopReceiverRequested: () -> Unit,
    onShareImportedFile: () -> Unit,
    onSaveToLocalRequested: () -> Unit,
    onClearRequested: () -> Unit,
    onExportRequested: () -> Unit,
    onExportFormatSelected: (ExportFormat) -> Unit,
    onDismissExportDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDestination by remember { mutableStateOf(CollectorDestination.BLUETOOTH) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderBlock(uiState = uiState)
                when (selectedDestination) {
                    CollectorDestination.BLUETOOTH -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val stacked = maxWidth < 760.dp
                            if (stacked) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    InstrumentPanel(
                                        brands = uiState.availableBrands,
                                        models = uiState.filteredModels(),
                                        selectedBrandId = uiState.selectedBrandId,
                                        selectedModelId = uiState.selectedModelId,
                                        selectionLocked = uiState.isSelectionLocked(),
                                        onBrandSelected = onInstrumentBrandSelected,
                                        onModelSelected = onInstrumentModelSelected,
                                    )
                                    BluetoothPanel(
                                        uiState = uiState,
                                        selectionLocked = uiState.isSelectionLocked(),
                                        onTargetDeviceSelected = onTargetDeviceSelected,
                                        onPairDeviceRequested = onPairDeviceRequested,
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    InstrumentPanel(
                                        modifier = Modifier.weight(1f),
                                        brands = uiState.availableBrands,
                                        models = uiState.filteredModels(),
                                        selectedBrandId = uiState.selectedBrandId,
                                        selectedModelId = uiState.selectedModelId,
                                        selectionLocked = uiState.isSelectionLocked(),
                                        onBrandSelected = onInstrumentBrandSelected,
                                        onModelSelected = onInstrumentModelSelected,
                                    )
                                    BluetoothPanel(
                                        modifier = Modifier.weight(1.15f),
                                        uiState = uiState,
                                        selectionLocked = uiState.isSelectionLocked(),
                                        onTargetDeviceSelected = onTargetDeviceSelected,
                                        onPairDeviceRequested = onPairDeviceRequested,
                                    )
                                }
                            }
                        }
                        BluetoothActionPanel(
                            uiState = uiState,
                            onDiscoveryRequested = onDiscoveryRequested,
                            onStopDiscoveryRequested = onStopDiscoveryRequested,
                            onConnectRequested = onConnectRequested,
                            onDisconnectRequested = onDisconnectRequested,
                        )
                    }

                    CollectorDestination.DATA -> {
                        DataActionPanel(
                            uiState = uiState,
                            onStartReceivingRequested = onStartReceivingRequested,
                            onStopReceivingRequested = onStopReceivingRequested,
                            onSingleMeasureRequested = onSingleMeasureRequested,
                            onStartImportRequested = onStartImportRequested,
                            onStartReceiverRequested = onStartReceiverRequested,
                            onClearRequested = onClearRequested,
                            onExportRequested = onExportRequested,
                            onSaveToLocalRequested = onSaveToLocalRequested,
                        )
                        ReceiverModePanel(
                            receiverState = uiState.receiverState,
                            isReceiverDiscoverable = uiState.isReceiverDiscoverable,
                            receiverDiagnostics = uiState.receiverDiagnostics,
                            showReceiverMode = uiState.usesReceiverImportMode(),
                            onStartReceiver = onStartReceiverRequested,
                            onStopReceiver = onStopReceiverRequested,
                        )
                        ImportedFilePanel(
                            fileInfo = uiState.importedFileInfo,
                            onShareFile = onShareImportedFile,
                        )
                        PreviewPanel(records = uiState.previewRecords)
                    }
                }
            }
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.testTag(CollectorScreenTags.BottomNavBluetooth),
                    selected = selectedDestination == CollectorDestination.BLUETOOTH,
                    onClick = { selectedDestination = CollectorDestination.BLUETOOTH },
                    icon = { Spacer(modifier = Modifier.size(1.dp)) },
                    label = { Text(text = "蓝牙") },
                )
                NavigationBarItem(
                    modifier = Modifier.testTag(CollectorScreenTags.BottomNavData),
                    selected = selectedDestination == CollectorDestination.DATA,
                    onClick = { selectedDestination = CollectorDestination.DATA },
                    icon = { Spacer(modifier = Modifier.size(1.dp)) },
                    label = { Text(text = "数据") },
                )
            }
            if (uiState.isExportDialogVisible) {
                Box(modifier = Modifier.testTag(CollectorScreenTags.ExportDialog)) {
                    CollectorExportDialog(
                        onSelect = onExportFormatSelected,
                        onDismiss = onDismissExportDialog,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectorExportDialog(
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

@Composable
private fun HeaderBlock(uiState: CollectorUiState) {
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
                StatusChip(
                    label = "连接 ${uiState.connectionState.toDisplayText()}",
                )
                StatusChip(
                    label = when {
                        uiState.isImporting -> "导入中..."
                        uiState.isReceiving -> "接收中"
                        else -> "未接收"
                    },
                )
                StatusChip(
                    label = "已接收 ${uiState.receivedCount} 条",
                )
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

@Composable
private fun InstrumentPanel(
    brands: List<InstrumentBrand>,
    models: List<InstrumentModel>,
    selectedBrandId: String?,
    selectedModelId: String?,
    selectionLocked: Boolean,
    onBrandSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelCard(
        modifier = modifier,
        title = "仪器选择",
        subtitle = "左侧先定品牌与型号，后续收数使用型号的默认分隔策略。",
    ) {
        DropdownSelector(
            label = "仪器品牌",
            options = brands,
            selectedId = selectedBrandId,
            enabled = !selectionLocked,
            displayName = { it.displayName },
            optionId = { it.id },
            onSelect = onBrandSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        DropdownSelector(
            label = "仪器型号",
            options = models,
            selectedId = selectedModelId,
            enabled = !selectionLocked,
            displayName = { it.displayName },
            optionId = { it.modelId },
            onSelect = onModelSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (selectedModelId == null) {
                "尚未选择型号。"
            } else {
                "当前型号：$selectedModelId"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BluetoothPanel(
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
private fun BluetoothActionPanel(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DataActionPanel(
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
    val isGeoCom = uiState.availableModels.firstOrNull { it.modelId == uiState.selectedModelId }?.firmwareFamily == "Captivate"

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
                    onSingleMeasureClick = onSingleMeasureRequested
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
                Text(text = importProfile.actionLabel)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiverModePanel(
    receiverState: ReceiverState,
    isReceiverDiscoverable: Boolean,
    receiverDiagnostics: List<String>,
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
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun ReceiverState.toDisplayText(): String {
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

@Composable
private fun PreviewPanel(records: List<MeasurementRecord>) {
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
private fun PanelCard(
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

@Composable
private fun <T> DropdownSelector(
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
private fun EmptyPlaceholder(text: String) {
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
private fun StatusChip(label: String) {
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

@Composable
private fun ImportedFilePanel(
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
    }
}

private fun BluetoothConnectionState.toDisplayText(): String {
    return when (this) {
        BluetoothConnectionState.DISCONNECTED -> "未连接"
        BluetoothConnectionState.CONNECTING -> "连接中"
        BluetoothConnectionState.CONNECTED -> "已连接"
    }
}

private fun buildPermissionSummary(uiState: CollectorUiState): String {
    val parts = buildList {
        if (!uiState.permissionState.bluetoothEnabled) add("蓝牙未开启")
        if (!uiState.permissionState.canDiscover) add("缺少搜索权限")
        if (!uiState.permissionState.canConnect) add("缺少连接权限")
        if (!uiState.permissionState.canAdvertise) add("缺少广播/可发现权限")
    }
    return parts.joinToString(separator = " / ")
}
