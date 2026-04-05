package com.unforgettable.bluetoothcollector.data.protocol

interface ProtocolTransport {
    suspend fun sendBytes(data: ByteArray)

    suspend fun blockingReadBytes(): ByteArray

    suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray?
}
