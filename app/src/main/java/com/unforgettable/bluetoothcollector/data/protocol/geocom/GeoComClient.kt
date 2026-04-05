package com.unforgettable.bluetoothcollector.data.protocol.geocom

import com.unforgettable.bluetoothcollector.data.protocol.ProtocolTransport
import java.nio.charset.StandardCharsets.US_ASCII
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface GeoComCommandClient {
    suspend fun sendCommand(command: GeoComCommand): GeoComResponse?
}

class GeoComClient(
    private val transport: ProtocolTransport,
    private val maxReadAttempts: Int = DEFAULT_MAX_READ_ATTEMPTS,
    private val readTimeoutMillis: Long = DEFAULT_READ_TIMEOUT_MS,
) : GeoComCommandClient {
    private val commandMutex = Mutex()
    private val responseBuffer = StringBuilder()

    override suspend fun sendCommand(command: GeoComCommand): GeoComResponse? = commandMutex.withLock {
        transport.sendBytes(command.toRequest().toByteArray(US_ASCII))
        readResponseLine()?.let(GeoComResponse::parse)
    }

    private suspend fun readResponseLine(): String? {
        repeat(maxReadAttempts) {
            extractLine()?.let { line ->
                return line
            }

            val chunk = transport.blockingReadBytesWithTimeout(readTimeoutMillis)
            if (chunk == null || chunk.isEmpty()) return@repeat

            responseBuffer.append(chunk.toString(US_ASCII))
            extractLine()?.let { line ->
                return line
            }
        }
        return extractLine()
    }

    private fun extractLine(): String? {
        val newlineIndex = responseBuffer.indexOf("\n")
        if (newlineIndex == -1) return null

        val line = responseBuffer.substring(0, newlineIndex + 1)
        val remainder = responseBuffer.substring(newlineIndex + 1)
        responseBuffer.clear()
        responseBuffer.append(remainder)
        return line
    }

    companion object {
        private const val DEFAULT_MAX_READ_ATTEMPTS: Int = 5
        private const val DEFAULT_READ_TIMEOUT_MS: Long = 1_000L
    }
}
