package com.unforgettable.bluetoothcollector.ui.collector

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Collector Compose 入口。
 *
 * Route 只负责生命周期、权限 launcher、系统分享事件和 Screen wiring；
 * Android/Room/Bluetooth 具体装配在 CollectorRouteDependencies.kt，避免本文件继续膨胀。
 */
@Composable
fun CollectorRoute() {
    val appContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val dependencies = remember { CollectorAppDependenciesHolder.get(appContext) }
    val bluetoothController = dependencies.bluetoothController
    val permissionChecker = dependencies.permissionChecker
    val shareLauncher = dependencies.shareLauncher
    val factory = remember(dependencies) {
        CollectorViewModelFactory(
            repository = dependencies.dataRepository,
            bluetoothController = bluetoothController,
            exportManager = dependencies.exportManager,
            protocolHandlerFactory = dependencies.protocolHandlerFactory,
            timeProvider = dependencies.timeProvider,
            importDirectory = dependencies.importDirectory,
            importedArtifactStore = dependencies.importedArtifactStore,
            clientImportManager = dependencies.clientImportManager,
            receiverManager = dependencies.receiverManager,
            downloadsSaver = dependencies.downloadsSaver,
            appContext = appContext,
        )
    }
    val viewModel: CollectorViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    var pendingReceiverStart by remember { mutableStateOf(false) }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode > 0) {
            viewModel.onReceiverDiscoverabilityGranted(result.resultCode)
        } else {
            viewModel.onReceiverDiscoverabilityDenied()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        bluetoothController.refreshPermissionState()
        if (bluetoothController.permissionState.value.canConnect) {
            viewModel.onRefreshPairedDevicesRequested()
        }
        if (pendingReceiverStart) {
            pendingReceiverStart = false
            val permissionState = bluetoothController.permissionState.value
            if (permissionState.canConnect && permissionState.canAdvertise) {
                viewModel.onReceiverDiscoverabilityRequested()
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
                discoverableLauncher.launch(discoverableIntent)
            } else {
                viewModel.onReceiverStartPrerequisiteFailed("missing_receiver_permission")
            }
        }
    }

    DisposableEffect(bluetoothController) {
        bluetoothController.register()
        onDispose {
            bluetoothController.unregister()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                is CollectorUiEvent.ShareImportedFile -> {
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        event.file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    appContext.startActivity(
                        Intent.createChooser(shareIntent, null).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                }
                is CollectorUiEvent.SavedToLocal -> {
                    android.widget.Toast.makeText(
                        appContext,
                        "已保存到下载：${event.fileName}",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
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
        onSingleMeasureRequested = viewModel::onSingleMeasureRequested,
        onStartImportRequested = viewModel::onStartImportRequested,
        onStartReceiverRequested = {
            bluetoothController.refreshPermissionState()
            val permissionState = bluetoothController.permissionState.value
            when {
                !permissionState.bluetoothEnabled -> {
                    viewModel.onReceiverStartPrerequisiteFailed("bluetooth_disabled")
                }
                !permissionState.canConnect || !permissionState.canAdvertise -> {
                    pendingReceiverStart = true
                    permissionLauncher.launch(permissionChecker.requiredPermissionsForReceiver().toTypedArray())
                }
                else -> {
                    viewModel.onReceiverDiscoverabilityRequested()
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    discoverableLauncher.launch(discoverableIntent)
                }
            }
        },
        onStopReceiverRequested = viewModel::onStopReceiverRequested,
        onShareImportedFile = viewModel::onShareImportedFile,
        onSaveToLocalRequested = viewModel::onSaveToLocalRequested,
        onClearRequested = viewModel::onClearRequested,
        onExportRequested = viewModel::onExportRequested,
        onExportFormatSelected = viewModel::onExportFormatSelected,
        onDismissExportDialog = viewModel::onExportDialogDismissed,
    )
}
