package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class BluetoothConnectionManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionChecker: BluetoothPermissionChecker,
) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: BluetoothDevice,
        currentSessionDeviceAddress: String? = null,
        timeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    ): Result<Unit> {
        val newSocket = runCatching {
            val permissionDecision = BluetoothSessionPolicy.bondedConnectAvailability(permissionChecker.currentState())
            if (!permissionDecision.allowed) {
                throw IllegalStateException(permissionDecision.reason ?: "connect_permission_denied")
            }
            val deviceSnapshot = BluetoothDeviceSnapshot(
                address = device.address,
                isBonded = isBonded(device),
            )
            val canConnectDecision = BluetoothSessionPolicy.canConnect(deviceSnapshot)
            if (!canConnectDecision.allowed) {
                throw IllegalStateException(canConnectDecision.reason ?: "connect_not_allowed")
            }
            if (currentSessionDeviceAddress != null) {
                val reconnectDecision = BluetoothSessionPolicy.canReconnectSameSession(
                    currentSessionDeviceAddress = currentSessionDeviceAddress,
                    targetDevice = deviceSnapshot,
                )
                if (!reconnectDecision.allowed) {
                    throw IllegalStateException(reconnectDecision.reason ?: "same_session_reconnect_not_allowed")
                }
            }
            ensureDiscoveryStoppedBeforeConnect()
            disconnect()
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }
        return suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            val timeoutExecutor = Executors.newSingleThreadScheduledExecutor()
            val connectThread = Thread {
                try {
                    newSocket.connect()
                    val newInputStream = newSocket.inputStream
                    if (completed.compareAndSet(false, true)) {
                        socket = newSocket
                        inputStream = newInputStream
                        if (continuation.isActive) {
                            continuation.resume(Result.success(Unit))
                        }
                    } else {
                        runCatching { newInputStream.close() }
                        runCatching { newSocket.close() }
                    }
                } catch (throwable: Throwable) {
                    if (completed.compareAndSet(false, true) && continuation.isActive) {
                        socket = null
                        inputStream = null
                        runCatching { newSocket.close() }
                        continuation.resume(Result.failure(throwable))
                    } else {
                        runCatching { newSocket.close() }
                    }
                } finally {
                    timeoutExecutor.shutdownNow()
                }
            }
            connectThread.start()
            timeoutExecutor.schedule({
                if (completed.compareAndSet(false, true)) {
                    socket = null
                    inputStream = null
                    runCatching { newSocket.close() }
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException("bluetooth_connect_timeout")))
                    }
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS)
            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) {
                    socket = null
                    inputStream = null
                    runCatching { newSocket.close() }
                }
                timeoutExecutor.shutdownNow()
            }
        }
    }

    suspend fun drainIncomingBytes(maxBytes: Int = 1024): ByteArray = withContext(Dispatchers.IO) {
        val stream = inputStream ?: return@withContext ByteArray(0)
        val available = stream.available().coerceAtMost(maxBytes)
        if (available <= 0) return@withContext ByteArray(0)
        val buffer = ByteArray(available)
        val read = stream.read(buffer)
        if (read <= 0) ByteArray(0) else buffer.copyOf(read)
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    fun handleAdapterStateChanged(state: Int): Boolean {
        return if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
            disconnect()
            true
        } else {
            false
        }
    }

    fun disconnect() {
        runCatching { inputStream?.close() }
        runCatching { socket?.close() }
        inputStream = null
        socket = null
    }

    @SuppressLint("MissingPermission")
    private fun ensureDiscoveryStoppedBeforeConnect() {
        val adapter = bluetoothAdapter ?: return
        if (!permissionChecker.currentState().canDiscover) {
            // In the split-permission case, connecting must still be possible even when
            // discovery control is unavailable. Try to stop discovery if the platform
            // allows it, but do not block the connect path on missing scan permission.
            runCatching { adapter.cancelDiscovery() }
            return
        }
        val isDiscovering = runCatching { adapter.isDiscovering }
            .getOrElse { throw IllegalStateException("cannot_verify_discovery_state") }
        if (!isDiscovering) return
        val cancelled = runCatching {
            adapter.cancelDiscovery()
        }.getOrDefault(false)
        if (!cancelled) {
            throw IllegalStateException("discovery_must_be_stopped_before_connect")
        }
    }

    @SuppressLint("MissingPermission")
    private fun isBonded(device: BluetoothDevice): Boolean {
        return runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 10_000L
    }
}
