package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverDiagnosticEntry
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.ftp.FtpReceiveState
import com.unforgettable.bluetoothcollector.data.ftp.FtpReceivedFile
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo
import com.unforgettable.bluetoothcollector.data.import_.ImportProfile
import com.unforgettable.bluetoothcollector.data.import_.ImportProfileRegistry
import com.unforgettable.bluetoothcollector.data.import_.TransferConfidence
import com.unforgettable.bluetoothcollector.data.instrument.InstrumentCatalog
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.InstrumentBrand
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session

data class CollectorPermissionUiState(
    val canDiscover: Boolean = true,
    val canConnect: Boolean = true,
    val canAdvertise: Boolean = true,
    val bluetoothEnabled: Boolean = true,
)

data class CollectorUiState(
    val availableBrands: List<InstrumentBrand> = InstrumentCatalog.brands,
    val availableModels: List<InstrumentModel> = InstrumentCatalog.models,
    val selectedBrandId: String? = InstrumentCatalog.brands.firstOrNull()?.id,
    val selectedModelId: String? = null,
    val nearbyDevices: List<DiscoveredBluetoothDeviceItem> = emptyList(),
    val pairedDevices: List<BondedBluetoothDeviceItem> = emptyList(),
    val selectedTargetDeviceAddress: String? = null,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val isDiscovering: Boolean = false,
    val isReceiving: Boolean = false,
    val isImporting: Boolean = false,
    val currentSession: Session? = null,
    val previewRecords: List<MeasurementRecord> = emptyList(),
    val receivedCount: Int = 0,
    val isExportDialogVisible: Boolean = false,
    val importedFileInfo: ImportedFileInfo? = null,
    val exportFormatOptions: List<ExportFormat> = listOf(ExportFormat.CSV, ExportFormat.TXT),
    val permissionState: CollectorPermissionUiState = CollectorPermissionUiState(),
    val statusMessage: String? = null,
    val receiverState: ReceiverState = ReceiverState.Idle,
    val isReceiverDiscoverable: Boolean = false,
    val receiverDiagnostics: List<ReceiverDiagnosticEntry> = emptyList(),
    val ftpReceiveState: FtpReceiveState = FtpReceiveState.Idle,
    val ftpEndpointText: String? = null,
    val ftpReceivedFiles: List<FtpReceivedFile> = emptyList(),
)

fun CollectorUiState.filteredModels(): List<InstrumentModel> {
    val brandId = selectedBrandId ?: return emptyList()
    return availableModels.filter { it.brandId == brandId }
}

/**
 * 当前选择的仪器型号。
 *
 * 该函数把型号解析集中在 UI state，避免 ViewModel 和 Composable 各自遍历 catalog。
 */
fun CollectorUiState.selectedInstrumentModel(): InstrumentModel? {
    return availableModels.firstOrNull {
        it.brandId == selectedBrandId && it.modelId == selectedModelId
    }
}

fun CollectorUiState.isSelectionLocked(): Boolean {
    return connectionState != BluetoothConnectionState.DISCONNECTED
}

fun CollectorUiState.currentImportProfile(): ImportProfile {
    return ImportProfileRegistry.resolve(
        brandId = selectedBrandId,
        modelId = selectedModelId,
    )
}

/**
 * 是否使用 Captivate/GeoCOM 逻辑。
 *
 * UI 不应硬编码 TS60；需要判断协议族时通过该函数读取 catalog 结果。
 */
fun CollectorUiState.usesCaptivateProtocol(): Boolean {
    return selectedInstrumentModel()?.firmwareFamily == "Captivate"
}

fun CollectorUiState.usesReceiverImportMode(): Boolean {
    return currentImportProfile().executionMode == com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.RECEIVER_STREAM
}

/**
 * 是否使用 TS60 WLAN/FTP 项目文件传输模式。
 *
 * 这个判断把 TS60 的主 channel 从蓝牙 receiver 中拆出来，避免 UI 继续把完整项目传输误导为 GeoCOM 实时测量。
 */
fun CollectorUiState.usesFtpProjectTransferMode(): Boolean {
    return currentImportProfile().executionMode == com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.FTP_SERVER
}

fun CollectorUiState.canStartPrimaryImportAction(): Boolean {
    return when (currentImportProfile().executionMode) {
        com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.CLIENT_STREAM ->
            connectionState == BluetoothConnectionState.CONNECTED && !isReceiving

        com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.RECEIVER_STREAM ->
            receiverState is ReceiverState.Idle ||
                receiverState is ReceiverState.Completed ||
                receiverState is ReceiverState.Failed ||
                receiverState is ReceiverState.Cancelled

        com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.FTP_SERVER ->
            ftpReceiveState is FtpReceiveState.Idle ||
                ftpReceiveState is FtpReceiveState.Failed ||
                ftpReceiveState is FtpReceiveState.Stopped

        com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.GUIDANCE_ONLY ->
            connectionState == BluetoothConnectionState.CONNECTED && !isReceiving
    }
}

/**
 * 当前主导入按钮文案。
 *
 * 让按钮文案来自 profile，减少 Composable 对具体型号的字符串分支。
 */
fun CollectorUiState.primaryImportActionLabel(): String {
    return currentImportProfile().actionLabel
}

/**
 * 厂家文档更明确的推荐连接路径标签。
 *
 * UI 用此函数展示 TS60 的 WLAN/线缆建议；TS09 则展示已验证蓝牙导入。
 */
fun CollectorUiState.recommendedTransferRouteLabels(): List<String> {
    return currentImportProfile().capability.recommendedRoutes.map { it.label }
}

/**
 * 实验诊断路径标签。
 *
 * 只返回 EXPERIMENTAL_DIAGNOSTIC，避免 UI 把实验路径和推荐路径混在一起。
 */
fun CollectorUiState.experimentalTransferRouteLabel(): String? {
    return currentImportProfile().capability.experimentalRoutes
        .firstOrNull { it.confidence == TransferConfidence.EXPERIMENTAL_DIAGNOSTIC }
        ?.label
}
