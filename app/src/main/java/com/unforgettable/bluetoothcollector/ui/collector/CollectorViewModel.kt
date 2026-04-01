package com.unforgettable.bluetoothcollector.ui.collector

import androidx.lifecycle.ViewModel
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.TextStreamRecordParser
import com.unforgettable.bluetoothcollector.data.instrument.InstrumentCatalog
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoredCollectorSession(
    val session: Session,
    val records: List<MeasurementRecord>,
)

sealed interface CollectorBluetoothControllerEvent {
    data object AdapterPoweredOff : CollectorBluetoothControllerEvent
    data object LinkLost : CollectorBluetoothControllerEvent
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

interface CollectorBluetoothController {
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

    suspend fun blockingReadBytes(): ByteArray

    suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray?

    fun shutdown()
}

interface CollectorExportManager {
    suspend fun export(
        session: Session,
        records: List<MeasurementRecord>,
        format: ExportFormat,
    ): File
}

class CollectorViewModel(
    private val repository: CollectorDataRepository,
    private val bluetoothController: CollectorBluetoothController,
    private val exportManager: CollectorExportManager,
    private val timeProvider: CollectorTimeProvider,
    private val importDirectory: java.io.File,
    private val downloadsSaver: com.unforgettable.bluetoothcollector.data.share.DownloadsSaver? = null,
    private val appContext: android.content.Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val receiveDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val parser = TextStreamRecordParser()
    private val mutableUiState = MutableStateFlow(CollectorUiState())
    private val mutableEvents = MutableSharedFlow<CollectorUiEvent>(extraBufferCapacity = 1)
    private var receiveJob: Job? = null
    private var idleDrainJob: Job? = null

    val uiState: StateFlow<CollectorUiState> = mutableUiState.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = mutableUiState.value,
    )
    val events: Flow<CollectorUiEvent> = mutableEvents.asSharedFlow()

    init {
        bluetoothController.refreshPermissionState()
        scope.launch {
            bluetoothController.refreshPairedDevices()
        }
        scope.launch {
            bluetoothController.nearbyDevices.collectLatest { devices ->
                mutableUiState.update { it.copy(nearbyDevices = devices) }
            }
        }
        scope.launch {
            bluetoothController.pairedDevices.collectLatest { devices ->
                mutableUiState.update { current ->
                    current.copy(
                        pairedDevices = devices,
                        selectedTargetDeviceAddress = current.selectedTargetDeviceAddress
                            ?: devices.firstOrNull()?.address,
                    )
                }
            }
        }
        scope.launch {
            bluetoothController.isDiscovering.collectLatest { isDiscovering ->
                mutableUiState.update { it.copy(isDiscovering = isDiscovering) }
            }
        }
        scope.launch {
            bluetoothController.permissionState.collectLatest { permissionState ->
                mutableUiState.update { it.copy(permissionState = permissionState) }
            }
        }
        scope.launch {
            bluetoothController.controllerEvents.collectLatest { event ->
                when (event) {
                    CollectorBluetoothControllerEvent.AdapterPoweredOff -> {
                        stopReceivingInternal(resumeIdleDrain = false)
                        mutableUiState.update {
                            it.copy(
                                connectionState = BluetoothConnectionState.DISCONNECTED,
                                statusMessage = "bluetooth_disabled",
                            )
                        }
                    }
                    CollectorBluetoothControllerEvent.LinkLost -> {
                        stopReceivingInternal(resumeIdleDrain = false)
                        mutableUiState.update {
                            it.copy(
                                connectionState = BluetoothConnectionState.DISCONNECTED,
                                statusMessage = "bluetooth_link_lost",
                            )
                        }
                    }
                }
            }
        }
        scope.launch {
            restoreCurrentSession()
        }
    }

    fun onInstrumentBrandSelected(brandId: String) {
        if (uiState.value.isSelectionLocked() && uiState.value.selectedBrandId != brandId) return
        val nextModelId = InstrumentCatalog.models.firstOrNull { it.brandId == brandId }?.modelId
        mutableUiState.update {
            it.copy(
                selectedBrandId = brandId,
                selectedModelId = nextModelId,
                statusMessage = null,
            )
        }
    }

    fun onInstrumentModelSelected(modelId: String) {
        if (uiState.value.isSelectionLocked() && uiState.value.selectedModelId != modelId) return
        val matchedModel = InstrumentCatalog.models.firstOrNull { it.modelId == modelId }
        mutableUiState.update {
            it.copy(
                selectedBrandId = matchedModel?.brandId ?: it.selectedBrandId,
                selectedModelId = modelId,
                statusMessage = null,
            )
        }
    }

    fun onTargetDeviceSelected(address: String) {
        if (uiState.value.isSelectionLocked() && uiState.value.selectedTargetDeviceAddress != address) return
        mutableUiState.update {
            it.copy(
                selectedTargetDeviceAddress = address,
                statusMessage = null,
            )
        }
    }

    fun onDiscoveryRequested() {
        scope.launch {
            bluetoothController.refreshPermissionState()
            val permissionState = bluetoothController.permissionState.value
            when {
                !permissionState.bluetoothEnabled -> {
                    mutableUiState.update { it.copy(statusMessage = "bluetooth_disabled") }
                    return@launch
                }

                !permissionState.canDiscover -> {
                    mutableUiState.update { it.copy(statusMessage = "missing_discovery_permission") }
                    return@launch
                }
            }
            val started = bluetoothController.startDiscovery(uiState.value.connectionState)
            if (!started) {
                mutableUiState.update {
                    it.copy(
                        statusMessage = if (uiState.value.connectionState == BluetoothConnectionState.DISCONNECTED) {
                            "discovery_start_failed"
                        } else {
                            "discovery_requires_disconnected_state"
                        },
                    )
                }
            }
        }
    }

    fun onRefreshPairedDevicesRequested() {
        scope.launch {
            bluetoothController.refreshPairedDevices()
        }
    }

    fun onAppForegrounded() {
        bluetoothController.refreshPermissionState()
        onRefreshPairedDevicesRequested()
    }

    fun onAppBackgrounded() {
        if (!uiState.value.isReceiving) return
        scope.launch {
            stopReceivingInternal(resumeIdleDrain = true)
            mutableUiState.update {
                it.copy(statusMessage = "receiving_paused_backgrounded")
            }
        }
    }

    fun onStopDiscoveryRequested() {
        scope.launch {
            bluetoothController.cancelDiscovery()
        }
    }

    fun onConnectRequested(address: String) {
        scope.launch {
            if (bluetoothController.isDiscovering.value) {
                bluetoothController.cancelDiscovery()
            }
            val pairedDevice = bluetoothController.pairedDevices.value.firstOrNull { it.address == address }
            if (pairedDevice == null) {
                mutableUiState.update {
                    it.copy(
                        connectionState = BluetoothConnectionState.DISCONNECTED,
                        statusMessage = "connect_requires_bonded_device",
                    )
                }
                return@launch
            }

            // If switching to a different device, clear old session first.
            val existingSession = uiState.value.currentSession
            if (existingSession != null && existingSession.bluetoothDeviceAddress != address) {
                repository.clearCurrentSession()
                parser.dropIncompleteFragment()
                mutableUiState.update {
                    it.copy(
                        currentSession = null,
                        previewRecords = emptyList(),
                        receivedCount = 0,
                        importedFileInfo = null,
                    )
                }
            }

            mutableUiState.update {
                it.copy(
                    selectedTargetDeviceAddress = address,
                    connectionState = BluetoothConnectionState.CONNECTING,
                    statusMessage = null,
                )
            }

            val result = bluetoothController.connect(
                address = address,
                currentSessionDeviceAddress = uiState.value.currentSession?.bluetoothDeviceAddress,
            )
            result.onSuccess { connectedDevice ->
                mutableUiState.update {
                    it.copy(
                        selectedTargetDeviceAddress = connectedDevice.address,
                        connectionState = BluetoothConnectionState.CONNECTED,
                        statusMessage = null,
                    )
                }
                ensureIdleDrainLoop()
            }.onFailure { failure ->
                mutableUiState.update {
                    it.copy(
                        connectionState = BluetoothConnectionState.DISCONNECTED,
                        statusMessage = failure.message ?: "bluetooth_connect_failed",
                    )
                }
            }
        }
    }

    fun onPairDeviceRequested(address: String) {
        scope.launch {
            val paired = bluetoothController.requestBond(
                address = address,
                currentSessionDeviceAddress = uiState.value.currentSession?.bluetoothDeviceAddress,
            )
            if (!paired) {
                mutableUiState.update { it.copy(statusMessage = "bond_request_failed") }
                return@launch
            }
            bluetoothController.refreshPairedDevices()
            mutableUiState.update {
                it.copy(
                    selectedTargetDeviceAddress = address,
                    statusMessage = "bond_request_submitted",
                )
            }
        }
    }

    fun onDisconnectRequested() {
        scope.launch {
            stopReceivingInternal(resumeIdleDrain = false)
            bluetoothController.disconnect()
            mutableUiState.update {
                it.copy(
                    connectionState = BluetoothConnectionState.DISCONNECTED,
                    statusMessage = null,
                )
            }
        }
    }

    fun onStartReceivingRequested() {
        scope.launch {
            if (uiState.value.connectionState != BluetoothConnectionState.CONNECTED) {
                mutableUiState.update { it.copy(statusMessage = "receive_requires_connected_state") }
                return@launch
            }

            val activeSession = ensureActiveSession() ?: return@launch
            mutableUiState.update {
                it.copy(
                    currentSession = activeSession,
                    isReceiving = true,
                    statusMessage = null,
                )
            }
            cancelIdleDrain()
            if (receiveJob?.isActive == true) {
                return@launch
            }
            receiveJob = CoroutineScope(SupervisorJob() + receiveDispatcher).launch {
                receiveLoop(activeSession, importMode = false)
            }
        }
    }

    fun onStartImportRequested() {
        scope.launch {
            if (uiState.value.connectionState != BluetoothConnectionState.CONNECTED) {
                mutableUiState.update { it.copy(statusMessage = "receive_requires_connected_state") }
                return@launch
            }

            mutableUiState.update {
                it.copy(
                    isReceiving = true,
                    isImporting = true,
                    importedFileInfo = null,
                    statusMessage = null,
                )
            }
            cancelIdleDrain()
            if (receiveJob?.isActive == true) {
                return@launch
            }
            receiveJob = CoroutineScope(SupervisorJob() + receiveDispatcher).launch {
                rawFileReceiveLoop()
            }
        }
    }

    fun onStopReceivingRequested() {
        scope.launch {
            stopReceivingInternal(resumeIdleDrain = true)
        }
    }

    fun onClearRequested() {
        scope.launch {
            if (uiState.value.connectionState != BluetoothConnectionState.DISCONNECTED) {
                mutableUiState.update { it.copy(statusMessage = "clear_requires_disconnected_state") }
                return@launch
            }
            stopReceivingInternal(resumeIdleDrain = false)
            repository.clearCurrentSession()
            parser.dropIncompleteFragment()
            mutableUiState.update {
                it.copy(
                    currentSession = null,
                    previewRecords = emptyList(),
                    receivedCount = 0,
                    isExportDialogVisible = false,
                    importedFileInfo = null,
                    statusMessage = null,
                )
            }
        }
    }

    fun onExportRequested() {
        if (uiState.value.currentSession == null || uiState.value.previewRecords.isEmpty()) {
            mutableUiState.update { it.copy(statusMessage = "export_requires_current_session") }
            return
        }
        mutableUiState.update {
            it.copy(
                isExportDialogVisible = true,
                statusMessage = null,
            )
        }
    }

    fun onExportFormatSelected(format: ExportFormat) {
        scope.launch {
            val session = uiState.value.currentSession ?: return@launch
            val records = uiState.value.previewRecords
            mutableUiState.update { it.copy(isExportDialogVisible = false) }
            val exportedFile = exportManager.export(
                session = session,
                records = records,
                format = format,
            )
            mutableEvents.emit(CollectorUiEvent.ShareExport(file = exportedFile, format = format))
        }
    }

    fun onExportDialogDismissed() {
        mutableUiState.update { it.copy(isExportDialogVisible = false) }
    }

    private suspend fun restoreCurrentSession() {
        val restored = repository.restoreCurrentSession() ?: return
        mutableUiState.update {
            it.copy(
                selectedBrandId = restored.session.instrumentBrand,
                selectedModelId = restored.session.instrumentModel,
                selectedTargetDeviceAddress = restored.session.bluetoothDeviceAddress,
                currentSession = restored.session,
                previewRecords = restored.records.sortedBy(MeasurementRecord::sequence),
                receivedCount = restored.records.size,
                connectionState = BluetoothConnectionState.DISCONNECTED,
                isReceiving = false,
                statusMessage = null,
            )
        }
    }

    private suspend fun ensureActiveSession(): Session? {
        uiState.value.currentSession?.let { return it }

        val model = InstrumentCatalog.models.firstOrNull {
            it.modelId == uiState.value.selectedModelId &&
                it.brandId == uiState.value.selectedBrandId
        }
        val selectedDevice = bluetoothController.pairedDevices.value.firstOrNull {
            it.address == uiState.value.selectedTargetDeviceAddress
        }
        if (model == null || selectedDevice == null) {
            mutableUiState.update { it.copy(statusMessage = "receive_requires_instrument_and_device") }
            return null
        }

        return runCatching {
            repository.ensureCurrentSession(
                startedAt = timeProvider.now(),
                instrumentBrand = model.brandId,
                instrumentModel = model.modelId,
                bluetoothDeviceName = selectedDevice.name.orEmpty(),
                bluetoothDeviceAddress = selectedDevice.address,
                delimiterStrategy = model.delimiterStrategy,
            )
        }.onFailure { failure ->
            mutableUiState.update {
                it.copy(statusMessage = failure.message ?: "current_session_create_failed")
            }
        }.getOrNull()
    }

    private suspend fun receiveLoop(session: Session, importMode: Boolean = false) {
        val startCount = mutableUiState.value.receivedCount
        while (mutableUiState.value.isReceiving &&
            mutableUiState.value.connectionState == BluetoothConnectionState.CONNECTED
        ) {
            val hasReceivedRecords = mutableUiState.value.receivedCount > startCount
            val incoming = try {
                if (importMode && hasReceivedRecords) {
                    // Already received records in this import — use silence timeout to detect end.
                    bluetoothController.blockingReadBytesWithTimeout(IMPORT_SILENCE_TIMEOUT_MS)
                        ?: run {
                            val totalImported = mutableUiState.value.receivedCount - startCount
                            stopReceivingInternal(resumeIdleDrain = true)
                            mutableUiState.update {
                                it.copy(
                                    isImporting = false,
                                    statusMessage = "已导入 $totalImported 条记录",
                                )
                            }
                            return
                        }
                } else {
                    bluetoothController.blockingReadBytes()
                }
            } catch (e: java.io.IOException) {
                // Socket threw on read — physical link dropped.
                parser.dropIncompleteFragment()
                cancelIdleDrain()
                mutableUiState.update {
                    it.copy(
                        isReceiving = false,
                        isImporting = false,
                        connectionState = BluetoothConnectionState.DISCONNECTED,
                        statusMessage = "bluetooth_link_lost",
                    )
                }
                return
            }
            if (incoming.isEmpty()) continue
            val parseResult = parser.accept(
                chunk = incoming.toString(Charsets.UTF_8),
                delimiterStrategy = session.delimiterStrategy,
            )
            if (parseResult.completed.isEmpty() && parseResult.overflowed) {
                mutableUiState.update {
                    it.copy(statusMessage = "incoming_buffer_overflow_trimmed")
                }
            }
            parseResult.completed.forEach { parsedRecord ->
                val nextSequence = mutableUiState.value.previewRecords.size.toLong() + 1L
                val receivedAt = timeProvider.now()
                val record = MeasurementRecord(
                    id = "${session.sessionId}-${UUID.randomUUID()}",
                    sequence = nextSequence,
                    receivedAt = receivedAt,
                    instrumentBrand = session.instrumentBrand,
                    instrumentModel = session.instrumentModel,
                    bluetoothDeviceName = session.bluetoothDeviceName,
                    bluetoothDeviceAddress = session.bluetoothDeviceAddress,
                    rawPayload = parsedRecord.rawPayload,
                    parsedCode = parsedRecord.parsedCode,
                    parsedValue = parsedRecord.parsedValue,
                )
                repository.appendRecord(session.sessionId, record)
                mutableUiState.update {
                    val nextRecords = it.previewRecords + record
                    it.copy(
                        currentSession = session.copy(updatedAt = receivedAt),
                        previewRecords = nextRecords,
                        receivedCount = nextRecords.size,
                        statusMessage = if (parseResult.overflowed) {
                            "incoming_buffer_overflow_trimmed"
                        } else {
                            it.statusMessage
                        },
                    )
                }
            }
        }
    }

    private suspend fun rawFileReceiveLoop() {
        val buffer = java.io.ByteArrayOutputStream()
        val maxSize = RAW_FILE_MAX_BYTES
        while (mutableUiState.value.isReceiving &&
            mutableUiState.value.connectionState == BluetoothConnectionState.CONNECTED
        ) {
            val incoming = try {
                if (buffer.size() > 0) {
                    bluetoothController.blockingReadBytesWithTimeout(IMPORT_SILENCE_TIMEOUT_MS)
                        ?: break // silence → transmission complete
                } else {
                    bluetoothController.blockingReadBytes() // wait for first byte
                }
            } catch (e: java.io.IOException) {
                cancelIdleDrain()
                mutableUiState.update {
                    it.copy(
                        isReceiving = false,
                        isImporting = false,
                        connectionState = BluetoothConnectionState.DISCONNECTED,
                        statusMessage = "bluetooth_link_lost",
                    )
                }
                return
            }
            if (incoming.isNotEmpty()) {
                buffer.write(incoming)
                if (buffer.size() > maxSize) {
                    mutableUiState.update {
                        it.copy(statusMessage = "import_file_too_large")
                    }
                    break
                }
            }
        }
        val bytes = buffer.toByteArray()
        if (bytes.isEmpty()) {
            mutableUiState.update {
                it.copy(isReceiving = false, isImporting = false, statusMessage = "import_no_data_received")
            }
            ensureIdleDrainLoop()
            return
        }
        val header = bytes.copyOf(minOf(bytes.size, 512))
        val format = com.unforgettable.bluetoothcollector.data.import_.ImportedFileFormat.detect(header)
        val receivedAt = timeProvider.now()
        val fileName = "import-${receivedAt.replace(Regex("[^0-9T]"), "")}.${format.extension}"
        val dir = importDirectory.also { it.mkdirs() }
        val file = java.io.File(dir, fileName)
        file.writeBytes(bytes)
        val info = com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo(
            file = file,
            sizeBytes = bytes.size.toLong(),
            format = format,
            receivedAt = receivedAt,
        )
        mutableUiState.update {
            it.copy(
                isReceiving = false,
                isImporting = false,
                importedFileInfo = info,
                statusMessage = "已导入文件：${format.displayName}（${formatFileSize(bytes.size.toLong())}）",
            )
        }
        ensureIdleDrainLoop()
    }

    fun onShareImportedFile() {
        val info = uiState.value.importedFileInfo ?: return
        mutableEvents.tryEmit(CollectorUiEvent.ShareImportedFile(file = info.file, mimeType = info.format.mimeType))
    }

    fun onSaveToLocalRequested() {
        scope.launch {
            val session = uiState.value.currentSession
            val records = uiState.value.previewRecords
            val importedFile = uiState.value.importedFileInfo
            val ctx = appContext ?: return@launch
            val saver = downloadsSaver ?: return@launch
            if (importedFile != null) {
                saver.saveToDownloads(ctx, importedFile.file, importedFile.format.mimeType)
                mutableEvents.emit(CollectorUiEvent.SavedToLocal(fileName = importedFile.file.name))
            } else if (session != null && records.isNotEmpty()) {
                val exportedFile = exportManager.export(session, records, ExportFormat.CSV)
                saver.saveToDownloads(ctx, exportedFile, "text/csv")
                mutableEvents.emit(CollectorUiEvent.SavedToLocal(fileName = exportedFile.name))
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

    private fun ensureIdleDrainLoop() {
        if (idleDrainJob?.isActive == true) return
        if (mutableUiState.value.connectionState != BluetoothConnectionState.CONNECTED || mutableUiState.value.isReceiving) {
            return
        }
        idleDrainJob = CoroutineScope(SupervisorJob() + receiveDispatcher).launch {
            while (!mutableUiState.value.isReceiving &&
                mutableUiState.value.connectionState == BluetoothConnectionState.CONNECTED
            ) {
                bluetoothController.drainIncomingBytes()
                delay(RECEIVE_IDLE_DELAY_MS)
            }
        }
    }

    private fun cancelIdleDrain() {
        idleDrainJob?.cancel()
        idleDrainJob = null
    }

    private suspend fun stopReceivingInternal(
        resumeIdleDrain: Boolean,
    ) {
        receiveJob?.cancel()
        receiveJob = null
        parser.dropIncompleteFragment()
        mutableUiState.update { it.copy(isReceiving = false, isImporting = false) }
        if (resumeIdleDrain && mutableUiState.value.connectionState == BluetoothConnectionState.CONNECTED) {
            ensureIdleDrainLoop()
        } else {
            cancelIdleDrain()
        }
    }

    override fun onCleared() {
        receiveJob?.cancel()
        idleDrainJob?.cancel()
        bluetoothController.shutdown()
        scope.cancel()
        super.onCleared()
    }

    companion object {
        private const val RECEIVE_IDLE_DELAY_MS = 250L
        private const val IMPORT_SILENCE_TIMEOUT_MS = 3_000L
        private const val RAW_FILE_MAX_BYTES = 50 * 1024 * 1024 // 50MB
    }
}
