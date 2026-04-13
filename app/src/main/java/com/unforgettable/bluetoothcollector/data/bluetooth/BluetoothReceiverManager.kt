package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

interface BluetoothReceiverController {
    val receiverState: StateFlow<ReceiverState>

    suspend fun listenAndReceive(
        importDirectory: File,
        timeProvider: () -> String,
        silenceTimeoutMs: Long = BluetoothReceiverManager.SILENCE_TIMEOUT_MS,
        maxBytes: Int = BluetoothReceiverManager.MAX_RECEIVE_BYTES,
    ): File?

    fun cancel()

    fun resetState()
}

/**
 * Experimental RFCOMM server socket manager for TS60 receiver mode.
 *
 * Opens a Bluetooth Classic SPP server socket and waits for an incoming connection
 * from the instrument. Once connected, reads incoming bytes and saves the result
 * to a file.
 *
 * This is NOT field-proven — the TS60 may or may not initiate RFCOMM to the phone.
 * Treat this entire path as experimental.
 */
class BluetoothReceiverManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionChecker: BluetoothPermissionChecker,
) : BluetoothReceiverController {
    private val mutableReceiverState = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    override val receiverState: StateFlow<ReceiverState> = mutableReceiverState.asStateFlow()

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null
    @Volatile
    private var acceptedSocket: BluetoothSocket? = null

    /**
     * Opens an RFCOMM server socket and blocks (suspending) until:
     * - an instrument connects and sends data, OR
     * - the coroutine is cancelled, OR
     * - an error occurs.
     *
     * @param importDirectory where to save the received file
     * @param timeProvider returns a timestamp string for file naming
     * @param silenceTimeoutMs how long to wait (after first byte) before treating silence as end-of-transmission
     * @param maxBytes guard against oversized transfers
     * @return the received file, or null if cancelled/no data
     */
    @SuppressLint("MissingPermission")
    override suspend fun listenAndReceive(
        importDirectory: File,
        timeProvider: () -> String,
        silenceTimeoutMs: Long,
        maxBytes: Int,
    ): File? = withContext(Dispatchers.IO) {
        Log.i(TAG, "receiver start requested")
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Log.w(TAG, "receiver start failed: bluetooth adapter unavailable")
            mutableReceiverState.value = ReceiverState.Failed("bluetooth_adapter_not_available")
            return@withContext null
        }
        if (!permissionChecker.currentState().canConnect) {
            Log.w(TAG, "receiver start failed: missing BLUETOOTH_CONNECT permission")
            mutableReceiverState.value = ReceiverState.Failed("missing_bluetooth_connect_permission")
            return@withContext null
        }
        Log.i(
            TAG,
            "receiver compatibility profile: adapterName=${safeAdapterName(adapter)}, service=$SERVICE_NAME, sppUuid=$SPP_UUID, modes=secure+insecure",
        )
        Log.i(TAG, "receiver note: secure RFCOMM may require prior pairing; insecure fallback will be attempted")

        val client = acceptIncomingClient(adapter) ?: return@withContext null
        acceptedSocket = client
        Log.i(TAG, "receiver accepted remote device: ${describeRemoteDevice(client)}")

        // Read incoming data
        val buffer = ByteArrayOutputStream()
        mutableReceiverState.value = ReceiverState.Receiving(bytesReceived = 0)
        try {
            val inputStream = client.inputStream
            val readBuffer = ByteArray(4096)

            // Wait for first byte (indefinite)
            val firstRead = runInterruptible { inputStream.read(readBuffer) }
            if (firstRead <= 0) {
                Log.w(TAG, "receiver connected but no data was received")
                mutableReceiverState.value = ReceiverState.Failed("no_data_received")
                closeAcceptedSocket()
                return@withContext null
            }
            buffer.write(readBuffer, 0, firstRead)
            Log.i(TAG, "receiver first byte chunk received: $firstRead bytes")
            mutableReceiverState.value = ReceiverState.Receiving(bytesReceived = buffer.size().toLong())

            // Continue reading with silence-based completion
            while (currentCoroutineContext().isActive && buffer.size() < maxBytes) {
                val available = try {
                    runInterruptible {
                        // Poll with timeout: check available bytes, sleep briefly if none
                        val startWait = System.currentTimeMillis()
                        while (inputStream.available() == 0) {
                            if (System.currentTimeMillis() - startWait > silenceTimeoutMs) {
                                return@runInterruptible -1 // silence timeout
                            }
                            Thread.sleep(100)
                        }
                        val bytesRead = inputStream.read(readBuffer)
                        bytesRead
                    }
                } catch (_: IOException) {
                    break // connection closed
                }
                if (available == -1) break // silence → transmission complete
                if (available <= 0) break
                buffer.write(readBuffer, 0, available)
                Log.i(TAG, "receiver total bytes received: ${buffer.size()}")
                mutableReceiverState.value = ReceiverState.Receiving(bytesReceived = buffer.size().toLong())
            }
        } catch (error: IOException) {
            Log.w(TAG, "receiver read interrupted: ${error.message}")
        } finally {
            closeAcceptedSocket()
        }

        val bytes = buffer.toByteArray()
        if (bytes.isEmpty()) {
            Log.w(TAG, "receiver finished without any payload")
            mutableReceiverState.value = ReceiverState.Failed("no_data_received")
            return@withContext null
        }

        // Save to file
        val header = bytes.copyOf(minOf(bytes.size, 512))
        val format = com.unforgettable.bluetoothcollector.data.import_.ImportedFileFormat.detect(header)
        val receivedAt = timeProvider()
        val fileName = "receiver-${receivedAt.replace(Regex("[^0-9T]"), "")}.${format.extension}"
        val dir = importDirectory.also { it.mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        Log.i(TAG, "receiver completed: wrote ${bytes.size} bytes to ${file.absolutePath}")

        mutableReceiverState.value = ReceiverState.Completed(
            bytesReceived = bytes.size.toLong(),
            fileName = fileName,
        )
        file
    }

    override fun cancel() {
        Log.i(TAG, "receiver cancelled by user")
        mutableReceiverState.value = ReceiverState.Cancelled
        closeServerSocket()
        closeAcceptedSocket()
    }

    override fun resetState() {
        mutableReceiverState.value = ReceiverState.Idle
    }

    @SuppressLint("MissingPermission")
    private suspend fun acceptIncomingClient(adapter: BluetoothAdapter): BluetoothSocket? {
        val attempts = listOf(
            ListenerMode(
                label = "secure",
                serviceName = SERVICE_NAME,
                open = { adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID) },
            ),
            ListenerMode(
                label = "insecure",
                serviceName = "$SERVICE_NAME Insecure",
                open = { adapter.listenUsingInsecureRfcommWithServiceRecord("$SERVICE_NAME Insecure", SPP_UUID) },
            ),
        )

        attempts.forEachIndexed { index, mode ->
            val server = try {
                Log.i(TAG, "opening ${mode.label} RFCOMM server socket (${mode.serviceName})")
                mode.open()
            } catch (error: IOException) {
                Log.w(TAG, "failed to open ${mode.label} RFCOMM server socket: ${error.message}")
                if (index == attempts.lastIndex) {
                    mutableReceiverState.value = ReceiverState.Failed("rfcomm_server_open_failed: ${error.message}")
                }
                return@forEachIndexed
            }

            serverSocket = server
            mutableReceiverState.value = ReceiverState.Listening
            val client = try {
                Log.i(TAG, "waiting for ${mode.label} accept (timeout=${ACCEPT_TIMEOUT_MS}ms)")
                runInterruptible { server.accept(ACCEPT_TIMEOUT_MS.toInt()) }
            } catch (error: IOException) {
                if (!currentCoroutineContext().isActive) {
                    Log.i(TAG, "receiver accept cancelled while waiting on ${mode.label} listener")
                    mutableReceiverState.value = ReceiverState.Cancelled
                    return null
                }
                Log.w(TAG, "${mode.label} accept timed out or failed: ${error.message}")
                if (index == attempts.lastIndex) {
                    Log.w(TAG, "no incoming connection observed on secure/insecure listener")
                    mutableReceiverState.value = ReceiverState.Failed("no_incoming_connection")
                }
                null
            } finally {
                closeServerSocket()
            }

            if (client != null) {
                return client
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun safeRemoteAddress(socket: BluetoothSocket): String {
        return runCatching { socket.remoteDevice?.address ?: "unknown" }.getOrDefault("unknown")
    }

    @SuppressLint("MissingPermission")
    private fun describeRemoteDevice(socket: BluetoothSocket): String {
        val remote = socket.remoteDevice
        val address = runCatching { remote?.address ?: "unknown" }.getOrDefault("unknown")
        val bondState = runCatching { remote?.bondState ?: BluetoothPermissionChecker.LEGACY_DISCOVERY_BOND_STATE_UNKNOWN }
            .getOrDefault(BluetoothPermissionChecker.LEGACY_DISCOVERY_BOND_STATE_UNKNOWN)
        return "$address (bondState=${bondStateLabel(bondState)})"
    }

    @SuppressLint("MissingPermission")
    private fun safeAdapterName(adapter: BluetoothAdapter): String {
        return runCatching { adapter.name ?: "unknown" }.getOrDefault("unknown")
    }

    private fun bondStateLabel(state: Int): String {
        return when (state) {
            android.bluetooth.BluetoothDevice.BOND_BONDED -> "bonded"
            android.bluetooth.BluetoothDevice.BOND_BONDING -> "bonding"
            android.bluetooth.BluetoothDevice.BOND_NONE -> "not_bonded"
            else -> "unknown"
        }
    }

    private fun closeServerSocket() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun closeAcceptedSocket() {
        runCatching { acceptedSocket?.inputStream?.close() }
        runCatching { acceptedSocket?.close() }
        acceptedSocket = null
    }

    companion object {
        private const val SERVICE_NAME = "SurvLink Receiver"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SILENCE_TIMEOUT_MS = 3_000L
        const val MAX_RECEIVE_BYTES = 50 * 1024 * 1024 // 50MB
        const val ACCEPT_TIMEOUT_MS = 30_000L
        private const val TAG = "BluetoothReceiver"
    }
}

private data class ListenerMode(
    val label: String,
    val serviceName: String,
    val open: () -> BluetoothServerSocket,
)
