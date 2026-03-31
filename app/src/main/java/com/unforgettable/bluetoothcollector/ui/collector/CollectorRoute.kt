package com.unforgettable.bluetoothcollector.ui.collector

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionManager
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothDiscoveryManager
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothPermissionChecker
import com.unforgettable.bluetoothcollector.data.bluetooth.BondedDeviceManager
import com.unforgettable.bluetoothcollector.data.bluetooth.PairingRequestCoordinator
import com.unforgettable.bluetoothcollector.data.export.CsvExportWriter
import com.unforgettable.bluetoothcollector.data.export.TxtExportWriter
import com.unforgettable.bluetoothcollector.data.share.ShareLauncher
import com.unforgettable.bluetoothcollector.data.storage.AppDatabase
import com.unforgettable.bluetoothcollector.data.storage.CollectorRepository
import com.unforgettable.bluetoothcollector.data.storage.MeasurementRecordDao
import com.unforgettable.bluetoothcollector.data.storage.MeasurementRecordEntity
import com.unforgettable.bluetoothcollector.data.storage.SessionEntity
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun CollectorRoute() {
    val appContext = LocalContext.current.applicationContext
    val dependencies = remember { CollectorAppDependenciesHolder.get(appContext) }
    val bluetoothController = dependencies.bluetoothController
    val permissionChecker = dependencies.permissionChecker
    val shareLauncher = dependencies.shareLauncher
    val factory = remember(dependencies) {
        CollectorViewModelFactory(
            repository = dependencies.dataRepository,
            bluetoothController = bluetoothController,
            exportManager = dependencies.exportManager,
            timeProvider = dependencies.timeProvider,
        )
    }
    val viewModel: CollectorViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        bluetoothController.refreshPermissionState()
        if (bluetoothController.permissionState.value.canConnect) {
            viewModel.onRefreshPairedDevicesRequested()
        }
    }

    DisposableEffect(bluetoothController) {
        bluetoothController.register()
        onDispose {
            bluetoothController.unregister()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CollectorUiEvent.ShareExport -> {
                    shareLauncher.share(
                        context = appContext,
                        exportedFile = event.file,
                        format = event.format,
                    )
                }
            }
        }
    }

    CollectorScreen(
        uiState = uiState,
        onInstrumentBrandSelected = viewModel::onInstrumentBrandSelected,
        onInstrumentModelSelected = viewModel::onInstrumentModelSelected,
        onTargetDeviceSelected = viewModel::onTargetDeviceSelected,
        onDiscoveryRequested = {
            if (uiState.permissionState.canDiscover) {
                viewModel.onDiscoveryRequested()
            } else {
                permissionLauncher.launch(permissionChecker.requiredPermissionsForDiscovery().toTypedArray())
            }
        },
        onStopDiscoveryRequested = viewModel::onStopDiscoveryRequested,
        onPairDeviceRequested = { address ->
            if (uiState.permissionState.canConnect) {
                viewModel.onPairDeviceRequested(address)
            } else {
                permissionLauncher.launch(permissionChecker.requiredPermissionsForConnect().toTypedArray())
            }
        },
        onConnectRequested = {
            if (uiState.permissionState.canConnect) {
                uiState.selectedTargetDeviceAddress?.let(viewModel::onConnectRequested)
            } else {
                permissionLauncher.launch(permissionChecker.requiredPermissionsForConnect().toTypedArray())
            }
        },
        onDisconnectRequested = viewModel::onDisconnectRequested,
        onStartReceivingRequested = viewModel::onStartReceivingRequested,
        onStopReceivingRequested = viewModel::onStopReceivingRequested,
        onClearRequested = viewModel::onClearRequested,
        onExportRequested = viewModel::onExportRequested,
        onExportFormatSelected = viewModel::onExportFormatSelected,
        onDismissExportDialog = viewModel::onExportDialogDismissed,
    )
}

private class CollectorViewModelFactory(
    private val repository: CollectorDataRepository,
    private val bluetoothController: CollectorBluetoothController,
    private val exportManager: CollectorExportManager,
    private val timeProvider: CollectorTimeProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectorViewModel::class.java)) {
            return CollectorViewModel(
                repository = repository,
                bluetoothController = bluetoothController,
                exportManager = exportManager,
                timeProvider = timeProvider,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private class AndroidCollectorBluetoothController(
    private val permissionChecker: BluetoothPermissionChecker,
    private val discoveryManager: BluetoothDiscoveryManager,
    private val bondedDeviceManager: BondedDeviceManager,
    private val connectionManager: BluetoothConnectionManager,
    private val pairingCoordinator: PairingRequestCoordinator,
) : CollectorBluetoothController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutablePermissionState = MutableStateFlow(permissionChecker.currentState().toUiState())
    private var bondedAddressesJob: Job? = null
    private val registrationLock = Any()
    private var registrationCount: Int = 0

    override val nearbyDevices: StateFlow<List<com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem>> =
        discoveryManager.discoveredDevices
    override val pairedDevices: StateFlow<List<BondedBluetoothDeviceItem>> =
        bondedDeviceManager.bondedDevices
    override val isDiscovering: StateFlow<Boolean> =
        discoveryManager.isDiscovering
    override val permissionState: StateFlow<CollectorPermissionUiState> =
        mutablePermissionState

    fun register() {
        val shouldRegister = synchronized(registrationLock) {
            val previousCount = registrationCount
            registrationCount += 1
            previousCount == 0
        }
        if (!shouldRegister) return
        refreshPermissionState()
        discoveryManager.register()
        pairingCoordinator.register()
        bondedDeviceManager.refreshBondedDevices()
        if (bondedAddressesJob?.isActive != true) {
            bondedAddressesJob = scope.launch {
                pairingCoordinator.bondedAddresses.collectLatest {
                    bondedDeviceManager.refreshBondedDevices()
                    refreshPermissionState()
                }
            }
        }
    }

    fun unregister() {
        val shouldUnregister = synchronized(registrationLock) {
            if (registrationCount == 0) {
                false
            } else {
                registrationCount -= 1
                registrationCount == 0
            }
        }
        if (!shouldUnregister) return
        discoveryManager.unregister()
        pairingCoordinator.unregister()
    }

    override fun refreshPermissionState() {
        mutablePermissionState.value = permissionChecker.currentState().toUiState()
    }

    override suspend fun refreshPairedDevices() {
        bondedDeviceManager.refreshBondedDevices()
    }

    override suspend fun startDiscovery(connectionState: com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState): Boolean {
        refreshPermissionState()
        return discoveryManager.startDiscovery(connectionState)
    }

    override suspend fun cancelDiscovery(): Boolean = discoveryManager.cancelDiscovery()

    override suspend fun connect(
        address: String,
        currentSessionDeviceAddress: String?,
    ): Result<BondedBluetoothDeviceItem> {
        refreshPermissionState()
        val targetDevice = bondedDeviceManager.resolveDevice(address)
            ?: return Result.failure(IllegalStateException("connect_requires_bonded_device"))
        return connectionManager.connect(
            device = targetDevice,
            currentSessionDeviceAddress = currentSessionDeviceAddress,
            currentDiscoveryState = isDiscovering.value,
        ).map {
            bondedDeviceManager.findSavedTarget(address)
                ?: BondedBluetoothDeviceItem(
                    name = targetDevice.name,
                    address = targetDevice.address,
                )
        }
    }

    override suspend fun requestBond(
        address: String,
        currentSessionDeviceAddress: String?,
    ): Boolean {
        refreshPermissionState()
        return pairingCoordinator.requestBondByAddress(
            targetDeviceAddress = address,
            currentSessionDeviceAddress = currentSessionDeviceAddress,
        )
    }

    override suspend fun disconnect() {
        connectionManager.disconnect()
    }

    override suspend fun drainIncomingBytes(maxBytes: Int): ByteArray {
        return connectionManager.drainIncomingBytes(maxBytes)
    }

    override fun shutdown() {
        connectionManager.disconnect()
    }
}

private data class CollectorAppDependencies(
    val permissionChecker: BluetoothPermissionChecker,
    val bluetoothController: AndroidCollectorBluetoothController,
    val dataRepository: CollectorDataRepository,
    val exportManager: CollectorExportManager,
    val shareLauncher: ShareLauncher,
    val timeProvider: CollectorTimeProvider,
)

private object CollectorAppDependenciesHolder {
    @Volatile
    private var instance: CollectorAppDependencies? = null

    fun get(context: Context): CollectorAppDependencies {
        return instance ?: synchronized(this) {
            instance ?: buildDependencies(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDependencies(context: Context): CollectorAppDependencies {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val permissionChecker = BluetoothPermissionChecker(context)
        val discoveryManager = BluetoothDiscoveryManager(
            context = context,
            bluetoothAdapter = bluetoothAdapter,
            permissionChecker = permissionChecker,
        )
        val bondedDeviceManager = BondedDeviceManager(
            bluetoothAdapter = bluetoothAdapter,
            permissionChecker = permissionChecker,
        )
        val connectionManager = BluetoothConnectionManager(
            bluetoothAdapter = bluetoothAdapter,
            permissionChecker = permissionChecker,
        )
        val pairingCoordinator = PairingRequestCoordinator(
            context = context,
            bondedDeviceManager = bondedDeviceManager,
            permissionChecker = permissionChecker,
        )
        val bluetoothController = AndroidCollectorBluetoothController(
            permissionChecker = permissionChecker,
            discoveryManager = discoveryManager,
            bondedDeviceManager = bondedDeviceManager,
            connectionManager = connectionManager,
            pairingCoordinator = pairingCoordinator,
        )
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "collector.db",
        ).build()
        val collectorRepository = CollectorRepository.fromDatabase(database)
        val timeProvider = SystemCollectorTimeProvider()

        return CollectorAppDependencies(
            permissionChecker = permissionChecker,
            bluetoothController = bluetoothController,
            dataRepository = RoomCollectorDataRepository(
                collectorRepository = collectorRepository,
                measurementRecordDao = database.measurementRecordDao(),
            ),
            exportManager = DefaultCollectorExportManager(
                exportDirectory = File(context.filesDir, "exports"),
                csvExportWriter = CsvExportWriter(),
                txtExportWriter = TxtExportWriter(),
                timeProvider = timeProvider,
            ),
            shareLauncher = ShareLauncher(),
            timeProvider = timeProvider,
        )
    }
}

private class RoomCollectorDataRepository(
    private val collectorRepository: CollectorRepository,
    private val measurementRecordDao: MeasurementRecordDao,
) : CollectorDataRepository {
    override suspend fun restoreCurrentSession(): RestoredCollectorSession? {
        val currentSession = collectorRepository.restoreCurrentSession() ?: return null
        return RestoredCollectorSession(
            session = currentSession.toDomain(),
            records = measurementRecordDao.getBySessionIdOrdered(currentSession.sessionId).map(MeasurementRecordEntity::toDomain),
        )
    }

    override suspend fun ensureCurrentSession(
        startedAt: String,
        instrumentBrand: String,
        instrumentModel: String,
        bluetoothDeviceName: String,
        bluetoothDeviceAddress: String,
        delimiterStrategy: DelimiterStrategy,
    ): Session {
        return collectorRepository.ensureCurrentSession(
            startedAt = startedAt,
            instrumentBrand = instrumentBrand,
            instrumentModel = instrumentModel,
            bluetoothDeviceName = bluetoothDeviceName,
            bluetoothDeviceAddress = bluetoothDeviceAddress,
            delimiterStrategy = delimiterStrategy,
        ).toDomain()
    }

    override suspend fun appendRecord(sessionId: String, record: MeasurementRecord) {
        collectorRepository.appendRecord(
            sessionId = sessionId,
            record = record.toEntity(sessionId),
        )
    }

    override suspend fun clearCurrentSession() {
        collectorRepository.clearCurrentSession()
    }
}

private class DefaultCollectorExportManager(
    private val exportDirectory: File,
    private val csvExportWriter: CsvExportWriter,
    private val txtExportWriter: TxtExportWriter,
    private val timeProvider: CollectorTimeProvider,
) : CollectorExportManager {
    override suspend fun export(
        session: Session,
        records: List<MeasurementRecord>,
        format: ExportFormat,
    ): File {
        val exportedAt = OffsetDateTime.parse(timeProvider.now())
        return when (format) {
            ExportFormat.CSV -> csvExportWriter.write(
                directory = exportDirectory,
                session = session,
                records = records,
                exportedAt = exportedAt,
            )

            ExportFormat.TXT -> txtExportWriter.write(
                directory = exportDirectory,
                session = session,
                records = records,
                exportedAt = exportedAt,
            )
        }
    }
}

private class SystemCollectorTimeProvider : CollectorTimeProvider {
    override fun now(): String {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}

private fun com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothPermissionState.toUiState(): CollectorPermissionUiState {
    return CollectorPermissionUiState(
        canDiscover = canDiscover,
        canConnect = canConnect,
        bluetoothEnabled = bluetoothEnabled,
    )
}

private fun SessionEntity.toDomain(): Session {
    return Session(
        sessionId = sessionId,
        startedAt = startedAt,
        updatedAt = updatedAt,
        instrumentBrand = instrumentBrand,
        instrumentModel = instrumentModel,
        bluetoothDeviceName = bluetoothDeviceName,
        bluetoothDeviceAddress = bluetoothDeviceAddress,
        delimiterStrategy = delimiterStrategy,
        isCurrent = isCurrent,
    )
}

private fun MeasurementRecordEntity.toDomain(): MeasurementRecord {
    return MeasurementRecord(
        id = id,
        sequence = sequence,
        receivedAt = receivedAt,
        instrumentBrand = instrumentBrand,
        instrumentModel = instrumentModel,
        bluetoothDeviceName = bluetoothDeviceName,
        bluetoothDeviceAddress = bluetoothDeviceAddress,
        rawPayload = rawPayload,
        parsedCode = parsedCode,
        parsedValue = parsedValue,
    )
}

private fun MeasurementRecord.toEntity(sessionId: String): MeasurementRecordEntity {
    return MeasurementRecordEntity(
        id = id,
        sessionId = sessionId,
        sequence = sequence,
        receivedAt = receivedAt,
        instrumentBrand = instrumentBrand,
        instrumentModel = instrumentModel,
        bluetoothDeviceName = bluetoothDeviceName,
        bluetoothDeviceAddress = bluetoothDeviceAddress,
        rawPayload = rawPayload,
        parsedCode = parsedCode,
        parsedValue = parsedValue,
    )
}
