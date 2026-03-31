package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

class BluetoothConnectionManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionChecker: BluetoothPermissionChecker,
) {
    private val stateLock = Any()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var pendingSocket: BluetoothSocket? = null
    private var activeAttemptToken: Long = 0L

    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: BluetoothDevice,
        currentSessionDeviceAddress: String? = null,
        currentDiscoveryState: Boolean = false,
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
            ensureDiscoveryStoppedBeforeConnect(currentDiscoveryState = currentDiscoveryState)
            disconnect()
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }
        val attemptToken = beginAttempt(newSocket)
        return suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            val timeoutExecutor = Executors.newSingleThreadScheduledExecutor()
            val connectThread = Thread {
                try {
                    newSocket.connect()
                    val newInputStream = newSocket.inputStream
                    if (completed.compareAndSet(false, true)) {
                        if (publishConnectedState(attemptToken, newSocket, newInputStream)) {
                            continuation.resume(Result.success(Unit)) { _, _, _ ->
                                clearOwnedState(attemptToken)
                                runCatching { newInputStream.close() }
                                runCatching { newSocket.close() }
                            }
                        } else {
                            runCatching { newInputStream.close() }
                            runCatching { newSocket.close() }
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(IllegalStateException("bluetooth_connect_invalidated")))
                            }
                        }
                    } else {
                        runCatching { newInputStream.close() }
                        runCatching { newSocket.close() }
                    }
                } catch (throwable: Throwable) {
                    if (completed.compareAndSet(false, true)) {
                        clearOwnedState(attemptToken)
                        runCatching { newSocket.close() }
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(throwable))
                        }
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
                    clearOwnedState(attemptToken)
                    runCatching { newSocket.close() }
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException("bluetooth_connect_timeout")))
                    }
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS)
            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) {
                    clearOwnedState(attemptToken)
                    runCatching { newSocket.close() }
                }
                timeoutExecutor.shutdownNow()
            }
        }
    }

    suspend fun drainIncomingBytes(maxBytes: Int = 1024): ByteArray = withContext(Dispatchers.IO) {
        val stream = inputStream ?: return@withContext ByteArray(0)
        runCatching {
            val available = stream.available().coerceAtMost(maxBytes)
            if (available <= 0) {
                ByteArray(0)
            } else {
                val buffer = ByteArray(available)
                val read = stream.read(buffer)
                if (read <= 0) ByteArray(0) else buffer.copyOf(read)
            }
        }.getOrDefault(ByteArray(0))
    }

    // Blocks until at least one byte arrives, or throws IOException on disconnect.
    // Use this in the active receive loop instead of drainIncomingBytes().
    suspend fun blockingReadBytes(bufferSize: Int = 4096): ByteArray = withContext(Dispatchers.IO) {
        val stream = inputStream ?: return@withContext ByteArray(0)
        val buffer = ByteArray(bufferSize)
        val bytesRead = stream.read(buffer)
        if (bytesRead <= 0) ByteArray(0) else buffer.copyOf(bytesRead)
    }

    // Same as blockingReadBytes but returns null if no data arrives within timeoutMs.
    // Used in import mode to detect end-of-transmission silence.
    suspend fun blockingReadBytesWithTimeout(timeoutMs: Long, bufferSize: Int = 4096): ByteArray? {
        return try {
            withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    val stream = inputStream ?: return@withContext ByteArray(0)
                    val buffer = ByteArray(bufferSize)
                    val bytesRead = runInterruptible { stream.read(buffer) }
                    if (bytesRead <= 0) ByteArray(0) else buffer.copyOf(bytesRead)
                }
            }
        } catch (_: TimeoutCancellationException) {
            null
        }
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
        synchronized(stateLock) {
            activeAttemptToken += 1
            runCatching { pendingSocket?.close() }
            runCatching { inputStream?.close() }
            runCatching { socket?.close() }
            pendingSocket = null
            inputStream = null
            socket = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun ensureDiscoveryStoppedBeforeConnect(currentDiscoveryState: Boolean) {
        val adapter = bluetoothAdapter ?: return
        if (!permissionChecker.currentState().canDiscover) {
            if (currentDiscoveryState) {
                throw IllegalStateException("cannot_control_active_discovery_state")
            }
            return
        }
        val isDiscovering = runCatching { adapter.isDiscovering }
            .getOrElse { if (currentDiscoveryState) true else throw IllegalStateException("cannot_verify_discovery_state") }
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

    private fun beginAttempt(newSocket: BluetoothSocket): Long {
        synchronized(stateLock) {
            activeAttemptToken += 1
            pendingSocket = newSocket
            return activeAttemptToken
        }
    }

    private fun publishConnectedState(
        attemptToken: Long,
        newSocket: BluetoothSocket,
        newInputStream: InputStream,
    ): Boolean {
        synchronized(stateLock) {
            if (activeAttemptToken != attemptToken) return false
            pendingSocket = null
            socket = newSocket
            inputStream = newInputStream
            return true
        }
    }

    private fun clearOwnedState(attemptToken: Long) {
        synchronized(stateLock) {
            if (activeAttemptToken == attemptToken) {
                pendingSocket = null
                socket = null
                inputStream = null
            }
        }
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 10_000L
    }
}
