package com.unforgettable.bluetoothcollector.ui.collector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothReceiverController
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.instrument.InstrumentCatalog
import com.unforgettable.bluetoothcollector.data.import_.BluetoothClientImportController
import com.unforgettable.bluetoothcollector.data.import_.BluetoothClientImportResult
import com.unforgettable.bluetoothcollector.data.import_.ImportedArtifactStoreContract
import com.unforgettable.bluetoothcollector.data.import_.ImportExecutionMode
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class CollectorViewModel(
    private val repository: CollectorDataRepository,
    private val bluetoothController: CollectorBluetoothController,
    private val exportManager: CollectorExportManager,
    private val protocolHandlerFactory: ProtocolHandlerFactory,
    private val timeProvider: CollectorTimeProvider,
    private val importDirectory: java.io.File,
    private val importedArtifactStore: ImportedArtifactStoreContract? = null,
    private val clientImportManager: BluetoothClientImportController? = null,
    private val receiverManager: BluetoothReceiverController? = null,
    private val downloadsSaver: com.unforgettable.bluetoothcollector.data.share.DownloadsSaver? = null,
    private val appContext: android.content.Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val receiveDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutableUiState = MutableStateFlow(CollectorUiState())
    private val mutableEvents = MutableSharedFlow<CollectorUiEvent>(extraBufferCapacity = 1)
    private var receiveJob: Job? = null
    private var receiverJob: Job? = null
    private var idleDrainJob: Job? = null
    private var activeProtocolHandler: ProtocolHandler? = null

    val uiState: StateFlow<CollectorUiState> = mutableUiState.asStateFlow()
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
                        appendReceiverDiagnostic("蓝牙已关闭，已退出接收模式")
                        mutableUiState.update {
                            it.copy(
                                connectionState = BluetoothConnectionState.DISCONNECTED,
                                statusMessage = "bluetooth_disabled",
                                isReceiverDiscoverable = false,
                            )
                        }
                    }
                    CollectorBluetoothControllerEvent.LinkLost -> {
                        if (uiState.value.usesReceiverImportMode() &&
                            (receiverJob?.isActive == true || uiState.value.receiverState != ReceiverState.Idle)
                        ) {
                            appendReceiverDiagnostic("已忽略接收模式下的链路断开广播")
                            return@collectLatest
                        }
                        importedArtifactStore?.preserveLastSuccessfulOnFailure()
                        stopReceivingInternal(resumeIdleDrain = false)
                        appendReceiverDiagnostic("蓝牙链路断开")
                        mutableUiState.update {
                            it.copy(
                                connectionState = BluetoothConnectionState.DISCONNECTED,
                                statusMessage = "bluetooth_link_lost",
                            )
                        }
                    }
                    is CollectorBluetoothControllerEvent.DiscoverabilityChanged -> {
                        if (event.isDiscoverable) {
                            appendReceiverDiagnostic("系统蓝牙已进入可发现状态")
                            mutableUiState.update {
                                it.copy(
                                    isReceiverDiscoverable = true,
                                )
                            }
                        } else {
                            appendReceiverDiagnostic("系统蓝牙已退出可发现状态")
                            mutableUiState.update {
                                it.copy(
                                    isReceiverDiscoverable = false,
                                    statusMessage = "receiver_discoverable_expired",
                                )
                            }
                        }
                    }
                }
            }
        }
        receiverManager?.let { manager ->
            scope.launch {
                manager.receiverState.collectLatest { state ->
                    mutableUiState.update { current ->
                        val nextState = if (
                            current.receiverState is ReceiverState.RequestingDiscoverability &&
                            state is ReceiverState.Idle
                        ) {
                            current.receiverState
                        } else {
                            state
                        }
                        current.copy(receiverState = nextState)
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
            bluetoothController.disconnect()
            stopReceivingInternal(resumeIdleDrain = false)
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
            receiveJob = scope.launch(receiveDispatcher) {
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
            when (importProfile.executionMode) {
                ImportExecutionMode.RECEIVER_STREAM -> {
                    mutableUiState.update { it.copy(statusMessage = "receiver_mode_required_for_selected_model") }
                    return@launch
                }

                ImportExecutionMode.GUIDANCE_ONLY -> {
                    mutableUiState.update { it.copy(statusMessage = importProfile.guidanceMessage) }
                    return@launch
                }

                ImportExecutionMode.CLIENT_STREAM -> Unit
            }
            if (importProfile.verdict != ImportProfileVerdict.SUPPORTED) {
                mutableUiState.update { it.copy(statusMessage = importProfile.guidanceMessage) }
                return@launch
            }
            val importManager = clientImportManager ?: return@launch

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
            receiveJob = scope.launch(receiveDispatcher) {
                runClientImportLoop(importManager)
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

    private suspend fun runClientImportLoop(
        importManager: BluetoothClientImportController,
    ) {
        val modelCharset = java.nio.charset.Charset.forName(
            currentInstrumentModel()?.dataCharsetName ?: "GBK",
        )
        val result = try {
            importManager.receiveImportedFile(
                importDirectory = importDirectory,
                modelCharset = modelCharset,
                timeProvider = timeProvider::now,
                silenceTimeoutMs = IMPORT_SILENCE_TIMEOUT_MS,
                maxBytes = RAW_FILE_MAX_BYTES,
            )
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

        when (result) {
            BluetoothClientImportResult.NoData -> {
                importedArtifactStore?.preserveLastSuccessfulOnFailure()
                mutableUiState.update {
                    it.copy(isReceiving = false, isImporting = false, statusMessage = "import_no_data_received")
                }
                ensureIdleDrainLoop()
            }

            BluetoothClientImportResult.TooLarge -> {
                importedArtifactStore?.preserveLastSuccessfulOnFailure()
                mutableUiState.update {
                    it.copy(isReceiving = false, isImporting = false, statusMessage = "import_file_too_large")
                }
                ensureIdleDrainLoop()
            }

            is BluetoothClientImportResult.Success -> {
                importedArtifactStore?.save(result.info)
                mutableUiState.update {
                    it.copy(
                        isReceiving = false,
                        isImporting = false,
                        importedFileInfo = result.info,
                        statusMessage = "已导入文件：${result.info.format.displayName}（${formatFileSize(result.info.sizeBytes)}）",
                    )
                }
                ensureIdleDrainLoop()
            }
        }
    }

    // --- Experimental TS60 RFCOMM Receiver Mode ---

    fun onReceiverStartPrerequisiteFailed(reason: String) {
        appendReceiverDiagnostic("监听前置条件失败：$reason")
        mutableUiState.update {
            it.copy(
                statusMessage = reason,
                receiverState = ReceiverState.Idle,
            )
        }
    }

    fun onReceiverDiscoverabilityRequested() {
        appendReceiverDiagnostic("已请求系统开启蓝牙可发现模式")
        mutableUiState.update {
            it.copy(
                receiverState = ReceiverState.RequestingDiscoverability,
                statusMessage = "receiver_discoverable_requested",
            )
        }
    }

    fun onReceiverDiscoverabilityDenied() {
        appendReceiverDiagnostic("用户拒绝或系统未授予蓝牙可发现模式")
        mutableUiState.update {
            it.copy(
                receiverState = ReceiverState.Idle,
                isReceiverDiscoverable = false,
                statusMessage = "receiver_discoverable_denied",
            )
        }
    }

    fun onReceiverDiscoverabilityGranted(durationSeconds: Int) {
        appendReceiverDiagnostic("蓝牙可发现模式已启用：${durationSeconds}s")
        appendReceiverDiagnostic("请在 ${durationSeconds}s 内让 TS60 搜索当前手机")
        appendReceiverDiagnostic("使用标准 SPP UUID：${com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothReceiverManager.SPP_UUID}")
        appendReceiverDiagnostic("若 secure 配对失败，将继续尝试 secure / insecure RFCOMM 监听")
        appendReceiverDiagnostic("如 TS60 需要加密串口连接，请先在系统蓝牙完成配对")
        mutableUiState.update {
            it.copy(
                isReceiverDiscoverable = true,
                statusMessage = "receiver_discoverable_enabled_${durationSeconds}s",
            )
        }
        onStartReceiverRequested()
    }

    fun onStartReceiverRequested() {
        val manager = receiverManager ?: return
        if (uiState.value.currentImportProfile().executionMode != ImportExecutionMode.RECEIVER_STREAM) {
            appendReceiverDiagnostic("当前型号未启用导出接收模式")
            mutableUiState.update { it.copy(statusMessage = "receiver_mode_not_supported_for_selected_model") }
            return
        }
        if (uiState.value.isReceiving || uiState.value.isImporting) {
            appendReceiverDiagnostic("监听启动失败：当前仍有接收/导入任务")
            mutableUiState.update { it.copy(statusMessage = "receiver_conflicts_with_active_operation") }
            return
        }
        if (receiverJob?.isActive == true) return

        receiverJob = scope.launch {
            if (uiState.value.connectionState != BluetoothConnectionState.DISCONNECTED) {
                appendReceiverDiagnostic("启动监听前断开旧的 client 连接")
                stopReceivingInternal(resumeIdleDrain = false)
                bluetoothController.disconnect()
                mutableUiState.update {
                    it.copy(
                        connectionState = BluetoothConnectionState.DISCONNECTED,
                        isReceiving = false,
                        isImporting = false,
                    )
                }
            }
            manager.resetState()
            appendReceiverDiagnostic("按 Bluetooth Classic 串口模式等待 TS60 主动连入")
            appendReceiverDiagnostic("开始监听 RFCOMM 传入连接")
            val file = manager.listenAndReceive(
                importDirectory = importDirectory,
                timeProvider = timeProvider::now,
            )
            if (file != null) {
                appendReceiverDiagnostic("已接收导出文件：${file.name}")
                val header = file.readBytes().copyOf(minOf(file.length().toInt(), 512))
                val format = com.unforgettable.bluetoothcollector.data.import_.ImportedFileFormat.detect(header)
                val info = com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo(
                    file = file,
                    sizeBytes = file.length(),
                    format = format,
                    receivedAt = timeProvider.now(),
                )
                importedArtifactStore?.save(info)
                mutableUiState.update {
                    it.copy(
                        importedFileInfo = info,
                        statusMessage = "实验性接收完成：${format.displayName}（${formatFileSize(file.length())}）",
                    )
                }
            } else {
                appendReceiverDiagnostic("未收到任何传入连接或文件")
            }
        }
    }

    fun onStopReceiverRequested() {
        appendReceiverDiagnostic("已请求停止监听")
        receiverManager?.cancel()
        receiverJob?.cancel()
        receiverJob = null
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

    private fun appendReceiverDiagnostic(message: String) {
        mutableUiState.update { current ->
            val next = (current.receiverDiagnostics + message).takeLast(MAX_RECEIVER_DIAGNOSTICS)
            current.copy(receiverDiagnostics = next)
        }
    }

    private fun ensureIdleDrainLoop() {
        if (idleDrainJob?.isActive == true) return
        if (mutableUiState.value.connectionState != BluetoothConnectionState.CONNECTED || mutableUiState.value.isReceiving) {
            return
        }
        idleDrainJob = scope.launch(receiveDispatcher) {
            while (!mutableUiState.value.isReceiving &&
                mutableUiState.value.connectionState == BluetoothConnectionState.CONNECTED
            ) {
                try {
                    bluetoothController.drainIncomingBytes()
                } catch (_: java.io.IOException) {
                    mutableUiState.update {
                        it.copy(
                            connectionState = BluetoothConnectionState.DISCONNECTED,
                            statusMessage = "bluetooth_link_lost",
                        )
                    }
                    break
                }
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
        val wasImporting = mutableUiState.value.isImporting
        // Cancel the coroutine first (non-blocking signal).
        receiveJob?.cancel()
        // If importing, the receive loop is stuck in a native blocking read that
        // does not respond to coroutine cancellation alone. Disconnect the transport
        // to break the blocking InputStream.read().
        if (wasImporting) {
            bluetoothController.disconnect()
            mutableUiState.update {
                it.copy(connectionState = BluetoothConnectionState.DISCONNECTED)
            }
        }
        // Now that the stream is closed (import) or timeout will fire (live receive),
        // the job will finish promptly.
        receiveJob?.join()
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
        receiverJob?.cancel()
        receiverManager?.cancel()
        idleDrainJob?.cancel()
        bluetoothController.shutdown()
        scope.cancel()
        super.onCleared()
    }

    companion object {
        private const val RECEIVE_IDLE_DELAY_MS = 250L
        private const val IMPORT_SILENCE_TIMEOUT_MS = BluetoothClientImportController.SILENCE_TIMEOUT_MS
        private const val RAW_FILE_MAX_BYTES = BluetoothClientImportController.MAX_RECEIVE_BYTES
        private const val MAX_RECEIVER_DIAGNOSTICS = 24
    }
}
