package com.unforgettable.bluetoothcollector.data.import_

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.delay

sealed interface BluetoothClientImportResult {
    data class Success(val info: ImportedFileInfo) : BluetoothClientImportResult

    data object NoData : BluetoothClientImportResult

    data object TooLarge : BluetoothClientImportResult
}

interface BluetoothClientImportController {
    suspend fun receiveImportedFile(
        importDirectory: File,
        modelCharset: Charset,
        timeProvider: () -> String,
        silenceTimeoutMs: Long = SILENCE_TIMEOUT_MS,
        maxBytes: Int = MAX_RECEIVE_BYTES,
    ): BluetoothClientImportResult

    companion object {
        const val SILENCE_TIMEOUT_MS: Long = 3_000L
        const val MAX_RECEIVE_BYTES: Int = 50 * 1024 * 1024
    }
}

class BluetoothClientImportManager(
    private val waitForFirstChunk: suspend () -> ByteArray,
    private val drainAvailableBytes: suspend (Int) -> ByteArray,
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
    private val pollIntervalMs: Long = POLL_INTERVAL_MS,
) : BluetoothClientImportController {
    override suspend fun receiveImportedFile(
        importDirectory: File,
        modelCharset: Charset,
        timeProvider: () -> String,
        silenceTimeoutMs: Long,
        maxBytes: Int,
    ): BluetoothClientImportResult {
        val buffer = ByteArrayOutputStream()
        val firstChunk = waitForFirstChunk()
        if (firstChunk.isEmpty()) {
            return BluetoothClientImportResult.NoData
        }

        buffer.write(firstChunk)
        var lastDataAt = currentTimeMs()

        while (buffer.size() < maxBytes) {
            delay(pollIntervalMs)
            val remaining = maxBytes - buffer.size()
            val nextChunk = drainAvailableBytes(minOf(remaining, READ_CHUNK_BYTES))
            if (nextChunk.isNotEmpty()) {
                buffer.write(nextChunk)
                lastDataAt = currentTimeMs()
                continue
            }
            if (currentTimeMs() - lastDataAt >= silenceTimeoutMs) {
                break
            }
        }

        if (buffer.size() >= maxBytes) {
            return BluetoothClientImportResult.TooLarge
        }

        val bytes = buffer.toByteArray()
        val header = bytes.copyOf(minOf(bytes.size, HEADER_BYTES))
        val receivedAt = timeProvider()
        val format = ImportedFileFormat.detect(header, modelCharset)
        val fileName = "import-${receivedAt.replace(Regex("[^0-9T]"), "")}.${format.extension}"
        val file = File(importDirectory.also { it.mkdirs() }, fileName)
        file.writeBytes(bytes)

        return BluetoothClientImportResult.Success(
            ImportedFileInfo(
                file = file,
                sizeBytes = bytes.size.toLong(),
                format = format,
                receivedAt = receivedAt,
            ),
        )
    }

    companion object {
        private const val POLL_INTERVAL_MS: Long = 100L
        private const val READ_CHUNK_BYTES: Int = 4096
        private const val HEADER_BYTES: Int = 512
    }
}
