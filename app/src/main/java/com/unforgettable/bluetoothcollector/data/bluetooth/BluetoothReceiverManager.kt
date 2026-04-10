package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
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
) {
    private val mutableReceiverState = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    val receiverState: StateFlow<ReceiverState> = mutableReceiverState.asStateFlow()

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
    suspend fun listenAndReceive(
        importDirectory: File,
        timeProvider: () -> String,
        silenceTimeoutMs: Long = SILENCE_TIMEOUT_MS,
        maxBytes: Int = MAX_RECEIVE_BYTES,
    ): File? = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            mutableReceiverState.value = ReceiverState.Failed("bluetooth_adapter_not_available")
            return@withContext null
        }
        if (!permissionChecker.currentState().canConnect) {
            mutableReceiverState.value = ReceiverState.Failed("missing_bluetooth_connect_permission")
            return@withContext null
        }

        // Open server socket
        val server = try {
            adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
        } catch (e: IOException) {
            mutableReceiverState.value = ReceiverState.Failed("rfcomm_server_open_failed: ${e.message}")
            return@withContext null
        }
        serverSocket = server
        mutableReceiverState.value = ReceiverState.Listening

        // Accept incoming connection (blocking)
        val client: BluetoothSocket = try {
            runInterruptible { server.accept() }
        } catch (e: IOException) {
            closeServerSocket()
            if (!currentCoroutineContext().isActive) {
                mutableReceiverState.value = ReceiverState.Cancelled
            } else {
                mutableReceiverState.value = ReceiverState.Failed("accept_failed: ${e.message}")
            }
            return@withContext null
        } finally {
            // Close server socket after accepting one connection
            closeServerSocket()
        }
        acceptedSocket = client

        // Read incoming data
        val buffer = ByteArrayOutputStream()
        mutableReceiverState.value = ReceiverState.Receiving(bytesReceived = 0)
        try {
            val inputStream = client.inputStream
            val readBuffer = ByteArray(4096)

            // Wait for first byte (indefinite)
            val firstRead = runInterruptible { inputStream.read(readBuffer) }
            if (firstRead <= 0) {
                mutableReceiverState.value = ReceiverState.Failed("no_data_received")
                closeAcceptedSocket()
                return@withContext null
            }
            buffer.write(readBuffer, 0, firstRead)
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
                mutableReceiverState.value = ReceiverState.Receiving(bytesReceived = buffer.size().toLong())
            }
        } catch (_: IOException) {
            // Connection lost during read
        } finally {
            closeAcceptedSocket()
        }

        val bytes = buffer.toByteArray()
        if (bytes.isEmpty()) {
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

        mutableReceiverState.value = ReceiverState.Completed(
            bytesReceived = bytes.size.toLong(),
            fileName = fileName,
        )
        file
    }

    fun cancel() {
        mutableReceiverState.value = ReceiverState.Cancelled
        closeServerSocket()
        closeAcceptedSocket()
    }

    fun resetState() {
        mutableReceiverState.value = ReceiverState.Idle
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
        private const val SILENCE_TIMEOUT_MS = 3_000L
        private const val MAX_RECEIVE_BYTES = 50 * 1024 * 1024 // 50MB
    }
}
