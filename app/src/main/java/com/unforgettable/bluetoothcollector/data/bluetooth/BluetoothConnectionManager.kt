package com.unforgettable.bluetoothcollector.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.util.UUID

class BluetoothConnectionManager(
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: BluetoothDevice,
        timeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            bluetoothAdapter?.cancelDiscovery()
            disconnect()
            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                withTimeout(timeoutMillis) {
                    newSocket.connect()
                }
                socket = newSocket
                inputStream = newSocket.inputStream
            } catch (throwable: Throwable) {
                runCatching { newSocket.close() }
                throw throwable
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

    fun disconnect() {
        runCatching { inputStream?.close() }
        runCatching { socket?.close() }
        inputStream = null
        socket = null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 10_000L
    }
}
