package com.unforgettable.bluetoothcollector.ui.collector

import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothConnectionState
import com.unforgettable.bluetoothcollector.data.bluetooth.BluetoothReceiverController
import com.unforgettable.bluetoothcollector.data.bluetooth.ReceiverState
import com.unforgettable.bluetoothcollector.data.import_.BluetoothClientImportController
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolHandler
import com.unforgettable.bluetoothcollector.data.protocol.ProtocolHandlerFactory
import com.unforgettable.bluetoothcollector.data.import_.ImportedArtifactStoreContract
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileFormat
import com.unforgettable.bluetoothcollector.data.import_.ImportedFileInfo
import com.unforgettable.bluetoothcollector.data.import_.ImportProfileVerdict
import com.unforgettable.bluetoothcollector.domain.model.BondedBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.DiscoveredBluetoothDeviceItem
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
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

        try {
            viewModel.onStartReceivingRequested()
            advanceUntilIdle()

            assertEquals(1, repository.ensureRequests.size)
            assertEquals("leica", repository.ensureRequests.single().instrumentBrand)
            assertEquals("TS02", repository.ensureRequests.single().instrumentModel)
            assertEquals(sampleBondedDevice().address, repository.ensureRequests.single().bluetoothDeviceAddress)
            assertNotNull(viewModel.uiState.value.currentSession)
            assertTrue(viewModel.uiState.value.isReceiving)
        } finally {
            viewModel.onDisconnectRequested()
            advanceUntilIdle()
            disposeViewModel(viewModel)
        }
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

    @Test
    fun app_backgrounded_stops_receiving_and_keeps_connection_state() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        try {
            viewModel.onStartReceivingRequested()
            advanceUntilIdle()

            viewModel.onAppBackgrounded()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isReceiving)
            assertEquals(BluetoothConnectionState.CONNECTED, viewModel.uiState.value.connectionState)
            assertEquals("receiving_paused_backgrounded", viewModel.uiState.value.statusMessage)
        } finally {
            viewModel.onDisconnectRequested()
            advanceUntilIdle()
        }
    }

    @Test
    fun live_receive_is_refused_while_import_is_active() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        forceUiState(viewModel) {
            it.copy(
                isReceiving = true,
                isImporting = true,
                connectionState = BluetoothConnectionState.CONNECTED,
            )
        }

        viewModel.onStartReceivingRequested()
        advanceUntilIdle()

        assertEquals("receive_conflicts_with_import", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun import_is_refused_while_live_receive_is_active() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        try {
            viewModel.onStartReceivingRequested()
            advanceUntilIdle()

            viewModel.onStartImportRequested()
            advanceUntilIdle()

            assertEquals("import_conflicts_with_live_receive", viewModel.uiState.value.statusMessage)
        } finally {
            viewModel.onDisconnectRequested()
            advanceUntilIdle()
        }
    }

    @Test
    fun reconnect_to_different_device_is_refused_while_current_session_exists() = runTest(mainDispatcherRule.dispatcher) {
        val otherDevice = BondedBluetoothDeviceItem(
            name = "Leica TS16",
            address = "66:77:88:99:AA:BB",
        )
        val repository = FakeCollectorDataRepository(
            restoredSession = RestoredCollectorSession(
                session = sampleSession(),
                records = listOf(sampleRecord(sequence = 1, rawPayload = "01123.456")),
            ),
        )
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice(), otherDevice),
        )
        val viewModel = createViewModel(
            repository = repository,
            bluetooth = bluetooth,
        )
        advanceUntilIdle()

        viewModel.onConnectRequested(otherDevice.address)
        advanceUntilIdle()

        assertEquals(0, repository.clearCalls)
        assertTrue(bluetooth.connectRequests.isEmpty())
        assertEquals("device_change_requires_clear_current_session", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun disconnect_during_import_preserves_last_successful_imported_artifact() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val importedArtifactStore = FakeImportedArtifactStore(
            loadedArtifact = sampleImportedArtifact(),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
            importedArtifactStore = importedArtifactStore,
        )
        advanceUntilIdle()

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        forceUiState(viewModel) {
            it.copy(
                isReceiving = true,
                isImporting = true,
                connectionState = BluetoothConnectionState.CONNECTED,
            )
        }

        viewModel.onDisconnectRequested()
        repeat(3) { runCurrent() }

        assertEquals(sampleImportedArtifact().file.absolutePath, viewModel.uiState.value.importedFileInfo?.file?.absolutePath)
    }

    @Test
    fun ts60_profile_exposes_experimental_receiver_import_path() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()

        val importProfile = viewModel.uiState.value.currentImportProfile()

        assertEquals(ImportProfileVerdict.EXPERIMENTAL, importProfile.verdict)
        assertEquals("接收实时GSI数据", importProfile.liveReceiveLabel)
        assertEquals("启动导出接收", importProfile.actionLabel)
        assertEquals("Captivate 蓝牙导出到手机", importProfile.protocolSummary)
    }

    @Test
    fun stop_import_disconnects_transport_before_waiting_for_receive_job() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS09")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        forceUiState(viewModel) {
            it.copy(
                isReceiving = true,
                isImporting = true,
                connectionState = BluetoothConnectionState.CONNECTED,
            )
        }

        viewModel.onStopReceivingRequested()
        runCurrent()

        assertEquals(1, bluetooth.disconnectCalls)
    }

    @Ignore("Needs a more deterministic transport fake to avoid hanging the unit-test harness")
    @Test
    fun live_receive_appends_preview_record_from_blocking_read_stream() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
            blockingReadSequence = ArrayDeque(
                listOf(
                    "01123.456\n".toByteArray(),
                ),
            ),
            blockingReadFailureAfterSequence = IOException("socket closed"),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS02")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()

        viewModel.onStartReceivingRequested()
        repeat(3) { runCurrent() }

        assertEquals(1, viewModel.uiState.value.previewRecords.size)
        assertEquals("01123.456", viewModel.uiState.value.previewRecords.single().rawPayload)
    }

    @Test
    fun disconnected_restored_session_allows_selection_change_and_clears_current_session() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(
                restoredSession = RestoredCollectorSession(
                    session = sampleSession(),
                    records = listOf(sampleRecord(sequence = 1, rawPayload = "01123.456")),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onInstrumentBrandSelected("sokkia")
        viewModel.onInstrumentModelSelected("SX-103")
        viewModel.onTargetDeviceSelected("66:77:88:99:AA:BB")
        advanceUntilIdle()

        assertEquals("sokkia", viewModel.uiState.value.selectedBrandId)
        assertEquals("SX-103", viewModel.uiState.value.selectedModelId)
        assertEquals("66:77:88:99:AA:BB", viewModel.uiState.value.selectedTargetDeviceAddress)
        assertEquals(null, viewModel.uiState.value.currentSession)
        assertEquals(0, viewModel.uiState.value.previewRecords.size)
    }

    @Test
    fun start_receive_uses_protocol_handler_factory_and_appends_emitted_record() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(
                BondedBluetoothDeviceItem(name = "Leica TS60", address = sampleBondedDevice().address),
            ),
        )
        val protocolFactory = FakeProtocolHandlerFactory(
            handler = FakeProtocolHandler(
                emittedRecords = listOf(
                    MeasurementRecord(
                        id = "record-1",
                        sequence = 1,
                        receivedAt = "2026-03-31T10:15:00+08:00",
                        instrumentBrand = "leica",
                        instrumentModel = "TS60",
                        bluetoothDeviceName = "Leica TS60",
                        bluetoothDeviceAddress = sampleBondedDevice().address,
                        rawPayload = "%R1P,0,0:0,1.0,2.0,3.0",
                        parsedCode = "GEOCOM",
                        parsedValue = "1.0,2.0,3.0",
                        protocolType = "GEOCOM",
                        hzAngleRad = 1.0,
                        vAngleRad = 2.0,
                        slopeDistanceM = 3.0,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel(
            repository = FakeCollectorDataRepository(),
            bluetooth = bluetooth,
            protocolHandlerFactory = protocolFactory,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        repeat(3) { runCurrent() }

        viewModel.onStartReceivingRequested()
        repeat(5) { runCurrent() }

        assertEquals("TS60", protocolFactory.createdModel?.modelId)
        assertEquals(1, viewModel.uiState.value.previewRecords.size)
        assertEquals("GEOCOM", viewModel.uiState.value.previewRecords.single().protocolType)

        viewModel.onDisconnectRequested()
        repeat(3) { runCurrent() }
    }

    @Test
    fun single_measure_uses_active_protocol_handler_and_appends_record() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(
                BondedBluetoothDeviceItem(name = "Leica TS60", address = sampleBondedDevice().address),
            ),
        )
        val protocolHandler = FakeProtocolHandler(
            keepAlive = true,
            singleMeasurement = MeasurementRecord(
                id = "record-1",
                sequence = 1,
                receivedAt = "2026-03-31T10:15:00+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS60",
                bluetoothDeviceName = "Leica TS60",
                bluetoothDeviceAddress = sampleBondedDevice().address,
                rawPayload = "%R1P,0,0:0,1.0,2.0,3.0",
                parsedCode = "GEOCOM",
                parsedValue = "1.0,2.0,3.0",
                protocolType = "GEOCOM",
                hzAngleRad = 1.0,
                vAngleRad = 2.0,
                slopeDistanceM = 3.0,
            ),
        )
        val protocolFactory = FakeProtocolHandlerFactory(handler = protocolHandler)
        val repository = FakeCollectorDataRepository()
        val viewModel = createViewModel(
            repository = repository,
            bluetooth = bluetooth,
            protocolHandlerFactory = protocolFactory,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        repeat(3) { runCurrent() }

        viewModel.onStartReceivingRequested()
        repeat(3) { runCurrent() }

        viewModel.onSingleMeasureRequested()
        repeat(3) { runCurrent() }

        assertEquals(1, protocolHandler.singleMeasurementCalls)
        assertEquals(1, viewModel.uiState.value.previewRecords.size)
        assertEquals("GEOCOM", viewModel.uiState.value.previewRecords.single().protocolType)
        assertEquals(1, repository.currentRecords().size)

        viewModel.onDisconnectRequested()
        repeat(3) { runCurrent() }
    }

    @Test
    fun receiver_discoverability_request_and_denial_are_reflected_in_ui_state() = runTest(mainDispatcherRule.dispatcher) {
        val receiverManager = FakeBluetoothReceiverController()
        val viewModel = createViewModel(receiverManager = receiverManager)

        viewModel.onReceiverDiscoverabilityRequested()
        advanceUntilIdle()

        assertEquals(ReceiverState.RequestingDiscoverability, viewModel.uiState.value.receiverState)
        assertEquals("receiver_discoverable_requested", viewModel.uiState.value.statusMessage)

        viewModel.onReceiverDiscoverabilityDenied()
        advanceUntilIdle()

        assertEquals(ReceiverState.Idle, viewModel.uiState.value.receiverState)
        assertFalse(viewModel.uiState.value.isReceiverDiscoverable)
        assertEquals("receiver_discoverable_denied", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun receiver_discoverability_granted_disconnects_existing_client_before_listening() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController(
            pairedDevices = listOf(sampleBondedDevice()),
        )
        val receiverManager = FakeBluetoothReceiverController()
        val viewModel = createViewModel(
            bluetooth = bluetooth,
            receiverManager = receiverManager,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        viewModel.onConnectRequested(sampleBondedDevice().address)
        advanceUntilIdle()
        assertEquals(BluetoothConnectionState.CONNECTED, viewModel.uiState.value.connectionState)

        viewModel.onReceiverDiscoverabilityGranted(120)
        advanceUntilIdle()

        assertEquals(1, bluetooth.disconnectCalls)
        assertEquals(1, receiverManager.listenCalls)
        assertEquals(BluetoothConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
        assertTrue(viewModel.uiState.value.isReceiverDiscoverable)
        assertEquals("receiver_discoverable_enabled_120s", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun receiver_discoverability_granted_adds_ts60_pairing_and_spp_diagnostics() = runTest(mainDispatcherRule.dispatcher) {
        val receiverManager = FakeBluetoothReceiverController()
        val viewModel = createViewModel(receiverManager = receiverManager)

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        advanceUntilIdle()

        viewModel.onReceiverDiscoverabilityGranted(300)
        advanceUntilIdle()

        val diagnostics = viewModel.uiState.value.receiverDiagnostics.joinToString("\n")
        assertTrue(diagnostics.contains("300s"))
        assertTrue(diagnostics.contains("SPP UUID"))
        assertTrue(diagnostics.contains("secure / insecure"))
        assertTrue(diagnostics.contains("配对"))
    }

    @Test
    fun receiver_link_disconnect_broadcast_does_not_override_success_status() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController()
        val receiverManager = FakeBluetoothReceiverController()
        val completedFile = File.createTempFile("receiver-success", ".txt").apply {
            writeText("TS60 EXPORT")
            deleteOnExit()
        }
        receiverManager.nextFile = completedFile
        val viewModel = createViewModel(
            bluetooth = bluetooth,
            receiverManager = receiverManager,
        )

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS60")
        advanceUntilIdle()

        viewModel.onReceiverDiscoverabilityGranted(120)
        advanceUntilIdle()
        val completedStatus = viewModel.uiState.value.statusMessage

        bluetooth.emitControllerEvent(CollectorBluetoothControllerEvent.LinkLost)
        advanceUntilIdle()

        assertEquals(completedStatus, viewModel.uiState.value.statusMessage)
        assertEquals(completedFile.absolutePath, viewModel.uiState.value.importedFileInfo?.file?.absolutePath)
    }

    @Test
    fun receiver_mode_is_rejected_for_non_ts60_profiles() = runTest(mainDispatcherRule.dispatcher) {
        val receiverManager = FakeBluetoothReceiverController()
        val viewModel = createViewModel(receiverManager = receiverManager)

        viewModel.onInstrumentBrandSelected("leica")
        viewModel.onInstrumentModelSelected("TS09")
        advanceUntilIdle()

        viewModel.onStartReceiverRequested()
        advanceUntilIdle()

        assertEquals(0, receiverManager.listenCalls)
        assertEquals("receiver_mode_not_supported_for_selected_model", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun discoverability_lifecycle_updates_ui_when_scan_mode_changes() = runTest(mainDispatcherRule.dispatcher) {
        val bluetooth = FakeCollectorBluetoothController()
        val viewModel = createViewModel(bluetooth = bluetooth)
        advanceUntilIdle()

        bluetooth.emitControllerEvent(
            CollectorBluetoothControllerEvent.DiscoverabilityChanged(isDiscoverable = true),
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isReceiverDiscoverable)

        bluetooth.emitControllerEvent(
            CollectorBluetoothControllerEvent.DiscoverabilityChanged(isDiscoverable = false),
        )
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReceiverDiscoverable)
        assertEquals("receiver_discoverable_expired", viewModel.uiState.value.statusMessage)
    }

    private fun createViewModel(
        repository: FakeCollectorDataRepository = FakeCollectorDataRepository(),
        bluetooth: FakeCollectorBluetoothController = FakeCollectorBluetoothController(),
        exporter: FakeCollectorExportManager = FakeCollectorExportManager(),
        importedArtifactStore: FakeImportedArtifactStore? = null,
        protocolHandlerFactory: FakeProtocolHandlerFactory = FakeProtocolHandlerFactory(),
        clientImportManager: BluetoothClientImportController? = null,
        receiverManager: FakeBluetoothReceiverController? = null,
    ): CollectorViewModel {
        return CollectorViewModel(
            repository = repository,
            bluetoothController = bluetooth,
            exportManager = exporter,
            protocolHandlerFactory = protocolHandlerFactory,
            timeProvider = FakeCollectorTimeProvider(),
            importDirectory = java.io.File(System.getProperty("java.io.tmpdir"), "test-imports"),
            importedArtifactStore = importedArtifactStore,
            clientImportManager = clientImportManager,
            receiverManager = receiverManager,
            ioDispatcher = mainDispatcherRule.dispatcher,
            receiveDispatcher = mainDispatcherRule.dispatcher,
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

    private fun sampleImportedArtifact(): ImportedFileInfo {
        val file = File(System.getProperty("java.io.tmpdir"), "existing-import.gsi").apply {
            writeText("*110001+000000000")
        }
        return ImportedFileInfo(
            file = file,
            sizeBytes = file.length(),
            format = ImportedFileFormat.GSI,
            receivedAt = "2026-04-03T21:00:00+08:00",
        )
    }

    private fun disposeViewModel(viewModel: CollectorViewModel) {
        val onCleared = CollectorViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)
    }

    @Suppress("UNCHECKED_CAST")
    private fun forceUiState(
        viewModel: CollectorViewModel,
        transform: (CollectorUiState) -> CollectorUiState,
    ) {
        val stateField = CollectorViewModel::class.java.getDeclaredField("mutableUiState")
        stateField.isAccessible = true
        val stateFlow = stateField.get(viewModel) as MutableStateFlow<CollectorUiState>
        stateFlow.value = transform(stateFlow.value)
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

    fun currentRecords(): List<MeasurementRecord> = current?.records.orEmpty()
}

private class FakeCollectorBluetoothController(
    nearbyDevices: List<DiscoveredBluetoothDeviceItem> = emptyList(),
    pairedDevices: List<BondedBluetoothDeviceItem> = emptyList(),
    isDiscoveringInitially: Boolean = false,
    blockingReadSequence: ArrayDeque<ByteArray> = ArrayDeque(),
    blockingReadFailureAfterSequence: IOException? = null,
) : CollectorBluetoothController {
    private val mutableNearbyDevices = MutableStateFlow(nearbyDevices)
    private val mutablePairedDevices = MutableStateFlow(pairedDevices)
    private val mutableIsDiscovering = MutableStateFlow(isDiscoveringInitially)
    private val mutablePermissionState = MutableStateFlow(CollectorPermissionUiState())
    private val mutableControllerEvents = MutableSharedFlow<CollectorBluetoothControllerEvent>(extraBufferCapacity = 1)
    private val queuedBlockingReads = blockingReadSequence
    private val blockingReadFailure = blockingReadFailureAfterSequence
    private val disconnectSignal = CompletableDeferred<Unit>()

    override val nearbyDevices: StateFlow<List<DiscoveredBluetoothDeviceItem>> = mutableNearbyDevices
    override val pairedDevices: StateFlow<List<BondedBluetoothDeviceItem>> = mutablePairedDevices
    override val isDiscovering: StateFlow<Boolean> = mutableIsDiscovering
    override val permissionState: StateFlow<CollectorPermissionUiState> = mutablePermissionState
    override val controllerEvents = mutableControllerEvents

    val connectRequests = mutableListOf<String>()
    var disconnectCalls: Int = 0
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

    override suspend fun disconnect() {
        disconnectCalls += 1
        disconnectSignal.complete(Unit)
    }

    override suspend fun drainIncomingBytes(maxBytes: Int): ByteArray {
        kotlinx.coroutines.awaitCancellation()
    }

    override suspend fun sendBytes(data: ByteArray) = Unit

    override suspend fun blockingReadBytes(): ByteArray {
        queuedBlockingReads.removeFirstOrNull()?.let { return it }
        blockingReadFailure?.let { throw it }
        disconnectSignal.await()
        throw IOException("bluetooth_not_connected")
    }

    override suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray? {
        kotlinx.coroutines.delay(timeoutMs)
        return null
    }

    override fun shutdown() = Unit

    fun emitControllerEvent(event: CollectorBluetoothControllerEvent) {
        mutableControllerEvents.tryEmit(event)
    }
}

private class FakeBluetoothReceiverController : BluetoothReceiverController {
    private val mutableReceiverState = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    override val receiverState: StateFlow<ReceiverState> = mutableReceiverState
    var listenCalls: Int = 0
    var nextFile: File? = null

    override suspend fun listenAndReceive(
        importDirectory: File,
        timeProvider: () -> String,
        silenceTimeoutMs: Long,
        maxBytes: Int,
    ): File? {
        listenCalls += 1
        mutableReceiverState.value = ReceiverState.Listening
        val result = nextFile
        if (result != null) {
            mutableReceiverState.value = ReceiverState.Completed(
                bytesReceived = result.length(),
                fileName = result.name,
            )
        }
        return result
    }

    override fun cancel() {
        mutableReceiverState.value = ReceiverState.Cancelled
    }

    override fun resetState() {
        mutableReceiverState.value = ReceiverState.Idle
    }
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

private class FakeProtocolHandlerFactory(
    private val handler: ProtocolHandler = FakeProtocolHandler(),
) : ProtocolHandlerFactory {
    var createdModel: com.unforgettable.bluetoothcollector.domain.model.InstrumentModel? = null

    override fun create(
        model: com.unforgettable.bluetoothcollector.domain.model.InstrumentModel,
        session: Session,
        startingSequence: Long,
        timeProvider: () -> String,
        onOverflow: () -> Unit,
    ): ProtocolHandler {
        createdModel = model
        return handler
    }
}

private class FakeProtocolHandler(
    private val emittedRecords: List<MeasurementRecord> = emptyList(),
    private val keepAlive: Boolean = false,
    private val singleMeasurement: MeasurementRecord? = null,
) : ProtocolHandler {
    var singleMeasurementCalls: Int = 0
        private set

    override fun startSession() = flow {
        emittedRecords.forEach { emit(it) }
        if (keepAlive) {
            awaitCancellation()
        }
    }

    override suspend fun triggerSingleMeasurement(): MeasurementRecord? {
        singleMeasurementCalls += 1
        return singleMeasurement
    }

    override fun stopSession() = Unit
}

private class FakeCollectorTimeProvider : CollectorTimeProvider {
    override fun now(): String = "2026-03-31T10:15:00+08:00"
}

private class FakeImportedArtifactStore(
    loadedArtifact: ImportedFileInfo? = null,
) : ImportedArtifactStoreContract {
    private var artifact: ImportedFileInfo? = loadedArtifact

    override fun save(artifact: ImportedFileInfo) {
        this.artifact = artifact
    }

    override fun load(): ImportedFileInfo? = artifact

    override fun preserveLastSuccessfulOnFailure() = Unit

    override fun onCurrentSessionCleared() = Unit
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
