package com.unforgettable.bluetoothcollector.ui.collector

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionManager
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothDiscoveryManager
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothPermissionChecker
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothReceiverManager
import com.unforgettable.bluetoothcollector.data.bluetooth.BondedDeviceManager
import com.unforgettable.bluetoothcollector.data.bluetooth.PairingRequestCoordinator
import com.unforgettable.bluetoothcollector.data.export.CsvExportWriter
import com.unforgettable.bluetoothcollector.data.export.GeoComCsvExportWriter
import com.unforgettable.bluetoothcollector.data.export.TxtExportWriter
import com.unforgettable.bluetoothcollector.data.import_.BluetoothClientImportManager
import com.unforgettable.bluetoothcollector.data.import_.ImportedArtifactStore
import com.unforgettable.bluetoothcollector.data.protocol.DefaultProtocolHandlerFactory
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolHandlerFactory
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Route 层依赖装配。
 *
 * 这些类型集中承接 Android/Room/Bluetooth 具体实现，CollectorRoute 只负责 Compose wiring，
 * ViewModel 只依赖 CollectorContracts 中的接口，便于 agents 分层修改。
 */
internal class CollectorViewModelFactory(
    private val repository: CollectorDataRepository,
    private val bluetoothController: CollectorBluetoothController,
    private val exportManager: CollectorExportManager,
    private val protocolHandlerFactory: ProtocolHandlerFactory,
    private val timeProvider: CollectorTimeProvider,
    private val importDirectory: File,
    private val importedArtifactStore: ImportedArtifactStore,
    private val clientImportManager: BluetoothClientImportManager,
    private val receiverManager: BluetoothReceiverManager,
    private val downloadsSaver: com.unforgettable.bluetoothcollector.data.share.DownloadsSaver,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectorViewModel::class.java)) {
            return CollectorViewModel(
                repository = repository,
                bluetoothController = bluetoothController,
                exportManager = exportManager,
                protocolHandlerFactory = protocolHandlerFactory,
                timeProvider = timeProvider,
                importDirectory = importDirectory,
                importedArtifactStore = importedArtifactStore,
                clientImportManager = clientImportManager,
                receiverManager = receiverManager,
                downloadsSaver = downloadsSaver,
                appContext = appContext,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

internal class AndroidCollectorBluetoothController(
    private val context: Context,
    private val permissionChecker: BluetoothPermissionChecker,
    private val discoveryManager: BluetoothDiscoveryManager,
    private val bondedDeviceManager: BondedDeviceManager,
    private val connectionManager: BluetoothConnectionManager,
    private val pairingCoordinator: PairingRequestCoordinator,
) : CollectorBluetoothController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutablePermissionState = MutableStateFlow(permissionChecker.currentState().toUiState())
    private val mutableControllerEvents = MutableSharedFlow<CollectorBluetoothControllerEvent>(extraBufferCapacity = 1)
    private var bondedAddressesJob: Job? = null
    private val registrationLock = Any()
    private var registrationCount: Int = 0
    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val adapterWasHandled = connectionManager.handleAdapterStateChanged(state)
                    discoveryManager.handleAdapterStateChanged(state)
                    refreshPermissionState()
                    if (adapterWasHandled) {
                        mutableControllerEvents.tryEmit(CollectorBluetoothControllerEvent.AdapterPoweredOff)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    mutableControllerEvents.tryEmit(CollectorBluetoothControllerEvent.LinkLost)
                }
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                    mutableControllerEvents.tryEmit(
                        CollectorBluetoothControllerEvent.DiscoverabilityChanged(
                            isDiscoverable = scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                        ),
                    )
                }
            }
        }
    }

    override val nearbyDevices: StateFlow<List<com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem>> =
        discoveryManager.discoveredDevices
    override val pairedDevices: StateFlow<List<BondedBluetoothDeviceItem>> =
        bondedDeviceManager.bondedDevices
    override val isDiscovering: StateFlow<Boolean> =
        discoveryManager.isDiscovering
    override val permissionState: StateFlow<CollectorPermissionUiState> =
        mutablePermissionState
    override val controllerEvents: Flow<CollectorBluetoothControllerEvent> =
        mutableControllerEvents.asSharedFlow()

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
        context.registerReceiver(
            adapterStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            },
        )
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
        runCatching { context.unregisterReceiver(adapterStateReceiver) }
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

    @SuppressLint("MissingPermission")
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

    override suspend fun sendBytes(data: ByteArray) {
        connectionManager.sendBytes(data)
    }

    override suspend fun blockingReadBytes(): ByteArray {
        return connectionManager.blockingReadBytes()
    }

    override suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray? {
        return connectionManager.blockingReadBytesWithTimeout(timeoutMs)
    }

    override fun shutdown() {
        connectionManager.disconnect()
    }
}

internal data class CollectorAppDependencies(
    val permissionChecker: BluetoothPermissionChecker,
    val bluetoothController: AndroidCollectorBluetoothController,
    val dataRepository: CollectorDataRepository,
    val exportManager: CollectorExportManager,
    val protocolHandlerFactory: ProtocolHandlerFactory,
    val shareLauncher: ShareLauncher,
    val timeProvider: CollectorTimeProvider,
    val importDirectory: File,
    val importedArtifactStore: ImportedArtifactStore,
    val clientImportManager: BluetoothClientImportManager,
    val receiverManager: BluetoothReceiverManager,
    val downloadsSaver: com.unforgettable.bluetoothcollector.data.share.DownloadsSaver,
)

internal object CollectorAppDependenciesHolder {
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
            context = context,
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
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
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
                geoComCsvExportWriter = GeoComCsvExportWriter(),
                txtExportWriter = TxtExportWriter(),
                timeProvider = timeProvider,
            ),
            protocolHandlerFactory = DefaultProtocolHandlerFactory(bluetoothController),
            shareLauncher = ShareLauncher(),
            timeProvider = timeProvider,
            importDirectory = File(context.filesDir, "imports"),
            importedArtifactStore = ImportedArtifactStore(File(context.filesDir, "imports")),
            clientImportManager = BluetoothClientImportManager(
                waitForFirstChunk = bluetoothController::blockingReadBytes,
                drainAvailableBytes = bluetoothController::drainIncomingBytes,
            ),
            receiverManager = BluetoothReceiverManager(
                bluetoothAdapter = bluetoothAdapter,
                permissionChecker = permissionChecker,
            ),
            downloadsSaver = com.unforgettable.bluetoothcollector.data.share.DownloadsSaver(),
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
    private val geoComCsvExportWriter: GeoComCsvExportWriter,
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
            ExportFormat.CSV -> if (records.any { it.protocolType == "GEOCOM" || it.hzAngleRad != null }) {
                geoComCsvExportWriter.write(
                    directory = exportDirectory,
                    session = session,
                    records = records,
                    exportedAt = exportedAt,
                )
            } else {
                csvExportWriter.write(
                    directory = exportDirectory,
                    session = session,
                    records = records,
                    exportedAt = exportedAt,
                )
            }

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
        canAdvertise = canAdvertise,
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
        protocolType = protocolType,
        hzAngleRad = hzAngleRad,
        vAngleRad = vAngleRad,
        slopeDistanceM = slopeDistanceM,
        coordinateE = coordinateE,
        coordinateN = coordinateN,
        coordinateH = coordinateH,
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
        protocolType = protocolType,
        hzAngleRad = hzAngleRad,
        vAngleRad = vAngleRad,
        slopeDistanceM = slopeDistanceM,
        coordinateE = coordinateE,
        coordinateN = coordinateN,
        coordinateH = coordinateH,
    )
}
