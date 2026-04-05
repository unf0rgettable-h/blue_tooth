package com.unforgettable.bluetoothcollector.ui.collector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.instrument.InstrumentCatalog
import com.unforgettable.bluetoothcollector.data.import_.ImportedArtifactStoreContract
import com.unforgettable.bluetoothcollector.data.import_.ImportProfileVerdict
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolHandler
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolHandlerFactory
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolTransport
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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

class CollectorViewModel(
    private val repository: CollectorDataRepository,
    private val bluetoothController: CollectorBluetoothController,
    private val exportManager: CollectorExportManager,
    private val protocolHandlerFactory: ProtocolHandlerFactory,
    private val timeProvider: CollectorTimeProvider,
    private val importDirectory: java.io.File,
    private val importedArtifactStore: ImportedArtifactStoreContract? = null,
    private val downloadsSaver: com.unforgettable.bluetoothcollector.data.share.DownloadsSaver? = null,
    private val appContext: android.content.Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val receiveDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutableUiState = MutableStateFlow(CollectorUiState())
    private val mutableEvents = MutableSharedFlow<CollectorUiEvent>(extraBufferCapacity = 1)
    private var receiveJob: Job? = null
    private var idleDrainJob: Job? = null
    private var activeProtocolHandler: ProtocolHandler? = null

    val uiState: StateFlow<CollectorUiState> = mutableUiState.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = mutableUiState.value,
    )
    val events: Flow<CollectorUiEvent> = mutableEvents.asSharedFlow()

    init {
        importedArtifactStore?.load()?.let { importedArtifact ->
            mutableUiState.update { it.copy(importedFileInfo = importedArtifact) }
        }
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
                        importedArtifactStore?.preserveLastSuccessfulOnFailure()
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
        if (uiState.value.currentSession != null && uiState.value.selectedBrandId != brandId) {
            scope.launch {
                clearCurrentSessionForSelectionChange()
                val nextModelId = InstrumentCatalog.models.firstOrNull { it.brandId == brandId }?.modelId
                mutableUiState.update {
                    it.copy(
                        selectedBrandId = brandId,
                        selectedModelId = nextModelId,
                        statusMessage = null,
                    )
                }
            }
            return
        }
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
        if (uiState.value.currentSession != null && uiState.value.selectedModelId != modelId) {
            scope.launch {
                clearCurrentSessionForSelectionChange()
                val matchedModel = InstrumentCatalog.models.firstOrNull { it.modelId == modelId }
                mutableUiState.update {
                    it.copy(
                        selectedBrandId = matchedModel?.brandId ?: it.selectedBrandId,
                        selectedModelId = modelId,
                        statusMessage = null,
                    )
                }
            }
            return
        }
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
        if (uiState.value.currentSession != null && uiState.value.selectedTargetDeviceAddress != address) {
            scope.launch {
                clearCurrentSessionForSelectionChange()
                mutableUiState.update {
                    it.copy(
                        selectedTargetDeviceAddress = address,
                        statusMessage = null,
                    )
                }
            }
            return
        }
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

            val existingSession = uiState.value.currentSession
            if (existingSession != null && existingSession.bluetoothDeviceAddress != address) {
                mutableUiState.update { it.copy(statusMessage = "device_change_requires_clear_current_session") }
                return@launch
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
            if (uiState.value.isImporting) {
                mutableUiState.update { it.copy(statusMessage = "receive_conflicts_with_import") }
                return@launch
            }
            if (uiState.value.connectionState != BluetoothConnectionState.CONNECTED) {
                mutableUiState.update { it.copy(statusMessage = "receive_requires_connected_state") }
                return@launch
            }

            val activeSession = ensureActiveSession() ?: return@launch
            val selectedModel = currentInstrumentModel()
            if (selectedModel == null) {
                mutableUiState.update { it.copy(statusMessage = "receive_requires_instrument_and_device") }
                return@launch
            }
            if (receiveJob?.isActive == true) {
                return@launch
            }

            val handler = protocolHandlerFactory.create(
                model = selectedModel,
                session = activeSession,
                startingSequence = mutableUiState.value.previewRecords.size.toLong(),
                timeProvider = timeProvider::now,
                onOverflow = {
                    mutableUiState.update { it.copy(statusMessage = "incoming_buffer_overflow_trimmed") }
                },
            )
            activeProtocolHandler = handler
            mutableUiState.update {
                it.copy(
                    currentSession = activeSession,
                    isReceiving = true,
                    statusMessage = null,
                )
            }
            cancelIdleDrain()
            receiveJob = CoroutineScope(SupervisorJob() + receiveDispatcher).launch {
                collectProtocolSession(activeSession, handler)
            }
        }
    }

    fun onStartImportRequested() {
        scope.launch {
            if (uiState.value.isReceiving && !uiState.value.isImporting) {
                mutableUiState.update { it.copy(statusMessage = "import_conflicts_with_live_receive") }
                return@launch
            }
            if (uiState.value.connectionState != BluetoothConnectionState.CONNECTED) {
                mutableUiState.update { it.copy(statusMessage = "receive_requires_connected_state") }
                return@launch
            }
            val importProfile = uiState.value.currentImportProfile()
            if (importProfile.verdict != ImportProfileVerdict.SUPPORTED) {
                mutableUiState.update { it.copy(statusMessage = importProfile.guidanceMessage) }
                return@launch
            }

            mutableUiState.update {
                it.copy(
                    isReceiving = true,
                    isImporting = true,
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

    fun onSingleMeasureRequested() {
        viewModelScope.launch {
            val session = uiState.value.currentSession ?: return@launch
            val record = activeProtocolHandler?.triggerSingleMeasurement() ?: return@launch
            appendRecordToState(session, record)
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
            importedArtifactStore?.onCurrentSessionCleared()
            mutableUiState.update {
                it.copy(
                    currentSession = null,
                    previewRecords = emptyList(),
                    receivedCount = 0,
                    isExportDialogVisible = false,
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
                isImporting = false,
                statusMessage = null,
            )
        }
    }

    private suspend fun clearCurrentSessionForSelectionChange() {
        repository.clearCurrentSession()
        importedArtifactStore?.onCurrentSessionCleared()
        mutableUiState.update {
            it.copy(
                currentSession = null,
                previewRecords = emptyList(),
                receivedCount = 0,
                isExportDialogVisible = false,
            )
        }
    }

    private suspend fun ensureActiveSession(): Session? {
        uiState.value.currentSession?.let { return it }

        val model = currentInstrumentModel()
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

    private suspend fun collectProtocolSession(
        session: Session,
        handler: ProtocolHandler,
    ) {
        try {
            handler.startSession().collectLatest { record ->
                appendRecordToState(session, record)
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
        } finally {
            handler.stopSession()
            if (activeProtocolHandler === handler) {
                activeProtocolHandler = null
            }
        }
    }

    private suspend fun appendRecordToState(
        session: Session,
        record: MeasurementRecord,
    ) {
        repository.appendRecord(session.sessionId, record)
        mutableUiState.update {
            val nextRecords = it.previewRecords + record
            it.copy(
                currentSession = session.copy(updatedAt = record.receivedAt),
                previewRecords = nextRecords,
                receivedCount = nextRecords.size,
            )
        }
    }

    private fun currentInstrumentModel(): InstrumentModel? {
        return InstrumentCatalog.models.firstOrNull {
            it.modelId == uiState.value.selectedModelId &&
                it.brandId == uiState.value.selectedBrandId
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
                importedArtifactStore?.preserveLastSuccessfulOnFailure()
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
                    importedArtifactStore?.preserveLastSuccessfulOnFailure()
                    mutableUiState.update {
                        it.copy(statusMessage = "import_file_too_large")
                    }
                    break
                }
            }
        }
        val bytes = buffer.toByteArray()
        if (bytes.isEmpty()) {
            importedArtifactStore?.preserveLastSuccessfulOnFailure()
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
        importedArtifactStore?.save(info)
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
        receiveJob?.cancelAndJoin()
        receiveJob = null
        activeProtocolHandler?.stopSession()
        activeProtocolHandler = null
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
