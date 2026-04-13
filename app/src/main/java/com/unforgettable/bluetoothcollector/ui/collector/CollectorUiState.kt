package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo
import com.unforgettable.bluetoothcollector.data.import_.ImportProfile
import com.unforgettable.bluetoothcollector.data.import_.ImportProfileRegistry
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
    val receiverDiagnostics: List<String> = emptyList(),
)

fun CollectorUiState.filteredModels(): List<InstrumentModel> {
    val brandId = selectedBrandId ?: return emptyList()
    return availableModels.filter { it.brandId == brandId }
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

fun CollectorUiState.usesReceiverImportMode(): Boolean {
    return currentImportProfile().executionMode == com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.RECEIVER_STREAM
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

        com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode.GUIDANCE_ONLY ->
            connectionState == BluetoothConnectionState.CONNECTED && !isReceiving
    }
}
