package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolTransport
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Collector 功能的边界契约。
 *
 * ViewModel、Route 装配和测试 fake 都依赖这里，避免 agents 为理解接口去打开超大实现文件。
 */
data class RestoredCollectorSession(
    val session: Session,
    val records: List<MeasurementRecord>,
)

sealed interface CollectorBluetoothControllerEvent {
    data object AdapterPoweredOff : CollectorBluetoothControllerEvent
    data object LinkLost : CollectorBluetoothControllerEvent
    data class DiscoverabilityChanged(
        val isDiscoverable: Boolean,
    ) : CollectorBluetoothControllerEvent
}

sealed interface CollectorUiEvent {
    data class ShareExport(
        val file: File,
        val format: ExportFormat,
    ) : CollectorUiEvent

    data class ShareImportedFile(
        val file: File,
        val mimeType: String,
    ) : CollectorUiEvent

    data class SavedToLocal(
        val fileName: String,
    ) : CollectorUiEvent
}

fun interface CollectorTimeProvider {
    fun now(): String
}

interface CollectorDataRepository {
    suspend fun restoreCurrentSession(): RestoredCollectorSession?

    suspend fun ensureCurrentSession(
        startedAt: String,
        instrumentBrand: String,
        instrumentModel: String,
        bluetoothDeviceName: String,
        bluetoothDeviceAddress: String,
        delimiterStrategy: DelimiterStrategy,
    ): Session

    suspend fun appendRecord(
        sessionId: String,
        record: MeasurementRecord,
    )

    suspend fun clearCurrentSession()
}

interface CollectorBluetoothController : ProtocolTransport {
    val nearbyDevices: StateFlow<List<DiscoveredBluetoothDeviceItem>>
    val pairedDevices: StateFlow<List<BondedBluetoothDeviceItem>>
    val isDiscovering: StateFlow<Boolean>
    val permissionState: StateFlow<CollectorPermissionUiState>
    val controllerEvents: Flow<CollectorBluetoothControllerEvent>

    fun refreshPermissionState()

    suspend fun refreshPairedDevices()

    suspend fun startDiscovery(connectionState: BluetoothConnectionState): Boolean

    suspend fun cancelDiscovery(): Boolean

    suspend fun connect(
        address: String,
        currentSessionDeviceAddress: String? = null,
    ): Result<BondedBluetoothDeviceItem>

    suspend fun requestBond(
        address: String,
        currentSessionDeviceAddress: String? = null,
    ): Boolean

    suspend fun disconnect()

    suspend fun drainIncomingBytes(maxBytes: Int = 1024): ByteArray

    fun shutdown()
}

interface CollectorExportManager {
    suspend fun export(
        session: Session,
        records: List<MeasurementRecord>,
        format: ExportFormat,
    ): File
}
