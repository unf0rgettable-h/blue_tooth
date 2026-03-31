package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @Test
    fun launch_restores_current_session_on_init() = runTest(mainDispatcherRule.dispatcher) {
        val restored = RestoredCollectorSession(
            session = sampleSession(),
            records = listOf(
                sampleRecord(sequence = 1, rawPayload = "01123.456"),
                sampleRecord(sequence = 2, rawPayload = "02124.456"),
            ),
        )
        val repository = FakeCollectorDataRepository(restoredSession = restored)

        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        assertEquals(1, repository.restoreCalls)
        assertEquals(restored.session.sessionId, viewModel.uiState.value.currentSession?.sessionId)
        assertEquals(restored.session.instrumentModel, viewModel.uiState.value.selectedModelId)
        assertEquals(
            restored.records.map { it.rawPayload },
            viewModel.uiState.value.previewRecords.map { it.rawPayload },
        )
    }

    @Test
    fun discovery_and_connect_are_mutually_exclusive() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
            isDiscoveringInitially = true,
        )
        val viewModel = createViewModel(bluetooth = bluetooth)

        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()

        assertEquals(1, bluetooth.cancelDiscoveryCalls)
        assertEquals(listOf(sampleBondedDevice().address), bluetooth.connectRequests)
    }

    @Test
    fun connect_requires_bonded_device() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(pairedDevices = emptyList())
        val viewModel = createViewModel(bluetooth = bluetooth)

        viewModel.onConnectRequested("AA:BB:CC:DD:EE:FF")
        advanceUntilIdle()

        assertTrue(bluetooth.connectRequests.isEmpty())
        assertEquals(BluetoothConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
        assertEquals("connect_requires_bonded_device", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun start_receive_creates_session_when_absent() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCollectorDataRepository()
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = repository,
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()

        viewModel.onStartReceivingRequested()
        advanceUntilIdle()

        assertEquals(1, repository.ensureRequests.size)
        assertEquals("leica", repository.ensureRequests.single().instrumentBrand)
        assertEquals("TS02", repository.ensureRequests.single().instrumentModel)
        assertEquals(sampleBondedDevice().address, repository.ensureRequests.single().bluetoothDeviceAddress)
        assertNotNull(viewModel.uiState.value.currentSession)
        assertTrue(viewModel.uiState.value.isReceiving)
    }

    @Test
    fun clear_is_only_allowed_while_disconnected() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCollectorDataRepository(
            restoredSession = RestoredCollectorSession(
                session = sampleSession(),
                records = listOf(sampleRecord(sequence = 1, rawPayload = "01123.456")),
            ),
        )
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = repository,
            bluetooth = bluetooth,
        )
        advanceUntilIdle()

        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        viewModel.onClearRequested()
        advanceUntilIdle()

        assertEquals(0, repository.clearCalls)

        viewModel.onDisconnectRequested()
        advanceUntilIdle()
        viewModel.onClearRequested()
        advanceUntilIdle()

        assertEquals(1, repository.clearCalls)
    }

    @Test
    fun app_restart_restores_current_session_data_but_not_connection_or_receiving_state() =
        runTest(mainDispatcherRule.dispatcher) {
            val restored = RestoredCollectorSession(
                session = sampleSession(),
                records = listOf(
                    sampleRecord(sequence = 1, rawPayload = "01123.456"),
                    sampleRecord(sequence = 2, rawPayload = "02124.456"),
                ),
            )
            val viewModel = createViewModel(
                repository = FakeCollectorDataRepository(restoredSession = restored),
            )
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.currentSession)
            assertEquals(2, viewModel.uiState.value.previewRecords.size)
            assertEquals(BluetoothConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
            assertFalse(viewModel.uiState.value.isReceiving)
        }

    @Test
    fun export_dialog_opens_with_csv_and_txt_options() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(
                restoredSession = RestoredCollectorSession(
                    session = sampleSession(),
                    records = listOf(sampleRecord(sequence = 1, rawPayload = "01123.456")),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onExportRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isExportDialogVisible)
        assertEquals(
            listOf(ExportFormat.CSV, ExportFormat.TXT),
            viewModel.uiState.value.exportFormatOptions,
        )
    }

    @Test
    fun export_preparation_emits_share_event() = runTest(mainDispatcherRule.dispatcher) {
        val exporter = FakeCollectorExportManager(
            exportedFile = File("/tmp/collector-session.csv"),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(
                restoredSession = RestoredCollectorSession(
                    session = sampleSession(),
                    records = listOf(sampleRecord(sequence = 1, rawPayload = "01123.456")),
                ),
            ),
            exporter = exporter,
        )
        advanceUntilIdle()

        val pendingEvent = backgroundScope.async {
            viewModel.events.first()
        }

        viewModel.onExportRequested()
        advanceUntilIdle()
        viewModel.onExportFormatSelected(ExportFormat.CSV)
        advanceUntilIdle()

        val event = pendingEvent.await() as CollectorUiEvent.ShareExport
        assertEquals(ExportFormat.CSV, exporter.requests.single().format)
        assertEquals(exporter.exportedFile, event.file)
        assertEquals(ExportFormat.CSV, event.format)
    }

    private fun createViewModel(
        repository: FakeCollectorDataRepository = FakeCollectorDataRepository(),
        bluetooth: FakeCollectorBluetoothController = FakeCollectorBluetoothController(),
        exporter: FakeCollectorExportManager = FakeCollectorExportManager(),
    ): CollectorViewModel {
        return CollectorViewModel(
            repository = repository,
            bluetoothController = bluetooth,
            exportManager = exporter,
            timeProvider = FakeCollectorTimeProvider(),
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    private fun sampleSession(): Session {
        return Session(
            sessionId = "session-1",
            startedAt = "2026-03-31T10:00:00+08:00",
            updatedAt = "2026-03-31T10:01:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = sampleBondedDevice().address,
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            isCurrent = true,
        )
    }

    private fun sampleRecord(sequence: Long, rawPayload: String): MeasurementRecord {
        return MeasurementRecord(
            id = "record-$sequence",
            sequence = sequence,
            receivedAt = "2026-03-31T10:00:0${sequence}+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = sampleBondedDevice().address,
            rawPayload = rawPayload,
            parsedCode = rawPayload.take(2),
            parsedValue = rawPayload.drop(2),
        )
    }

    private fun sampleBondedDevice(): BondedBluetoothDeviceItem {
        return BondedBluetoothDeviceItem(
            name = "Leica TS02",
            address = "00:11:22:33:44:55",
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

private class FakeCollectorDataRepository(
    restoredSession: RestoredCollectorSession? = null,
) : CollectorDataRepository {
    var restoreCalls: Int = 0
    var clearCalls: Int = 0
    val ensureRequests = mutableListOf<EnsureSessionRequest>()
    private var current: RestoredCollectorSession? = restoredSession

    override suspend fun restoreCurrentSession(): RestoredCollectorSession? {
        restoreCalls += 1
        return current
    }

    override suspend fun ensureCurrentSession(
        startedAt: String,
        instrumentBrand: String,
        instrumentModel: String,
        bluetoothDeviceName: String,
        bluetoothDeviceAddress: String,
        delimiterStrategy: DelimiterStrategy,
    ): Session {
        ensureRequests += EnsureSessionRequest(
            startedAt = startedAt,
            instrumentBrand = instrumentBrand,
            instrumentModel = instrumentModel,
            bluetoothDeviceName = bluetoothDeviceName,
            bluetoothDeviceAddress = bluetoothDeviceAddress,
            delimiterStrategy = delimiterStrategy,
        )
        val session = Session(
            sessionId = "generated-session-${ensureRequests.size}",
            startedAt = startedAt,
            updatedAt = startedAt,
            instrumentBrand = instrumentBrand,
            instrumentModel = instrumentModel,
            bluetoothDeviceName = bluetoothDeviceName,
            bluetoothDeviceAddress = bluetoothDeviceAddress,
            delimiterStrategy = delimiterStrategy,
            isCurrent = true,
        )
        current = RestoredCollectorSession(
            session = session,
            records = current?.records.orEmpty(),
        )
        return session
    }

    override suspend fun appendRecord(sessionId: String, record: MeasurementRecord) {
        current = current?.copy(records = current!!.records + record)
    }

    override suspend fun clearCurrentSession() {
        clearCalls += 1
        current = null
    }
}

private class FakeCollectorBluetoothController(
    nearbyDevices: List<DiscoveredBluetoothDeviceItem> = emptyList(),
    pairedDevices: List<BondedBluetoothDeviceItem> = emptyList(),
    isDiscoveringInitially: Boolean = false,
) : CollectorBluetoothController {
    private val mutableNearbyDevices = MutableStateFlow(nearbyDevices)
    private val mutablePairedDevices = MutableStateFlow(pairedDevices)
    private val mutableIsDiscovering = MutableStateFlow(isDiscoveringInitially)
    private val mutablePermissionState = MutableStateFlow(CollectorPermissionUiState())

    override val nearbyDevices: StateFlow<List<DiscoveredBluetoothDeviceItem>> = mutableNearbyDevices
    override val pairedDevices: StateFlow<List<BondedBluetoothDeviceItem>> = mutablePairedDevices
    override val isDiscovering: StateFlow<Boolean> = mutableIsDiscovering
    override val permissionState: StateFlow<CollectorPermissionUiState> = mutablePermissionState

    val connectRequests = mutableListOf<String>()
    var cancelDiscoveryCalls: Int = 0

    override fun refreshPermissionState() = Unit

    override suspend fun refreshPairedDevices() = Unit

    override suspend fun startDiscovery(connectionState: BluetoothConnectionState): Boolean {
        return if (connectionState == BluetoothConnectionState.DISCONNECTED) {
            mutableIsDiscovering.value = true
            true
        } else {
            false
        }
    }

    override suspend fun cancelDiscovery(): Boolean {
        cancelDiscoveryCalls += 1
        mutableIsDiscovering.value = false
        return true
    }

    override suspend fun connect(
        address: String,
        currentSessionDeviceAddress: String?,
    ): Result<BondedBluetoothDeviceItem> {
        connectRequests += address
        val matched = pairedDevices.value.firstOrNull { it.address == address }
        return if (matched != null) {
            Result.success(matched)
        } else {
            Result.failure(IllegalStateException("connect_requires_bonded_device"))
        }
    }

    override suspend fun requestBond(
        address: String,
        currentSessionDeviceAddress: String?,
    ): Boolean {
        mutablePairedDevices.value = mutablePairedDevices.value +
            BondedBluetoothDeviceItem(name = "Paired $address", address = address)
        return true
    }

    override suspend fun disconnect() = Unit

    override suspend fun drainIncomingBytes(maxBytes: Int): ByteArray = ByteArray(0)

    override fun shutdown() = Unit
}

private class FakeCollectorExportManager(
    val exportedFile: File = File("/tmp/collector-session.txt"),
) : CollectorExportManager {
    val requests = mutableListOf<ExportRequest>()

    override suspend fun export(
        session: Session,
        records: List<MeasurementRecord>,
        format: ExportFormat,
    ): File {
        requests += ExportRequest(
            session = session,
            records = records,
            format = format,
        )
        return exportedFile
    }
}

private class FakeCollectorTimeProvider : CollectorTimeProvider {
    override fun now(): String = "2026-03-31T10:15:00+08:00"
}

private data class EnsureSessionRequest(
    val startedAt: String,
    val instrumentBrand: String,
    val instrumentModel: String,
    val bluetoothDeviceName: String,
    val bluetoothDeviceAddress: String,
    val delimiterStrategy: DelimiterStrategy,
)

private data class ExportRequest(
    val session: Session,
    val records: List<MeasurementRecord>,
    val format: ExportFormat,
)
