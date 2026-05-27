package com.unforgettable.bluetoothcollector.ui.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat

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

/**
 * Collector 主页面编排层。
 *
 * 该文件只保留页面导航、布局组合和对外回调契约；具体 UI 区块拆到同包小文件，
 * 便于多个 coding agents 分别维护蓝牙、数据、诊断和预览区域。
 */
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
        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CollectorStatusHeader(uiState = uiState)
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
                    icon = { Icon(imageVector = Icons.Filled.Bluetooth, contentDescription = null) },
                    label = { Text(text = "蓝牙") },
                )
                NavigationBarItem(
                    modifier = Modifier.testTag(CollectorScreenTags.BottomNavData),
                    selected = selectedDestination == CollectorDestination.DATA,
                    onClick = { selectedDestination = CollectorDestination.DATA },
                    icon = { Icon(imageVector = Icons.Filled.Storage, contentDescription = null) },
                    label = { Text(text = "数据") },
                )
            }
            if (uiState.isExportDialogVisible) {
                ExportFormatDialog(
                    onSelect = onExportFormatSelected,
                    onDismiss = onDismissExportDialog,
                )
            }
        }
    }
}
