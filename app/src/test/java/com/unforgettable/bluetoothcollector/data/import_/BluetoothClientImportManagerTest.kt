package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothClientImportManagerTest {

    @Test
    fun client_import_completes_after_silence_without_waiting_for_second_export() = runTest {
        val transport = FakeBluetoothClientImportTransport(
            firstChunk = "FIRST_EXPORT".toByteArray(),
        )
        val manager = BluetoothClientImportManager(
            waitForFirstChunk = transport::waitForFirstChunk,
            drainAvailableBytes = transport::drainAvailableBytes,
            currentTimeMs = { testScheduler.currentTime },
            pollIntervalMs = 100L,
        )
        val importDirectory = Files.createTempDirectory("client-import-test").toFile()

        val deferred = backgroundScope.async {
            manager.receiveImportedFile(
                importDirectory = importDirectory,
                modelCharset = Charset.forName("GBK"),
                timeProvider = { "2026-04-14T00:00:00+08:00" },
                silenceTimeoutMs = 3_000L,
            )
        }

        advanceTimeBy(3_100L)
        advanceUntilIdle()

        val result = deferred.await()
        val imported = (result as BluetoothClientImportResult.Success).info
        assertArrayEquals("FIRST_EXPORT".toByteArray(), imported.file.readBytes())
        assertEquals(1, transport.firstReadCalls)
        assertTrue(transport.drainCalls > 0)
    }

    @Test
    fun client_import_appends_follow_up_chunks_before_silence_completion() = runTest {
        val transport = FakeBluetoothClientImportTransport(
            firstChunk = "HELLO".toByteArray(),
            drainedChunks = ArrayDeque(listOf(" WORLD".toByteArray())),
        )
        val manager = BluetoothClientImportManager(
            waitForFirstChunk = transport::waitForFirstChunk,
            drainAvailableBytes = transport::drainAvailableBytes,
            currentTimeMs = { testScheduler.currentTime },
            pollIntervalMs = 100L,
        )
        val importDirectory = Files.createTempDirectory("client-import-test").toFile()

        val deferred = backgroundScope.async {
            manager.receiveImportedFile(
                importDirectory = importDirectory,
                modelCharset = Charset.forName("GBK"),
                timeProvider = { "2026-04-14T00:00:00+08:00" },
                silenceTimeoutMs = 3_000L,
            )
        }

        advanceTimeBy(3_100L)
        advanceUntilIdle()

        val result = deferred.await()
        val imported = (result as BluetoothClientImportResult.Success).info
        assertArrayEquals("HELLO WORLD".toByteArray(), imported.file.readBytes())
    }

    @Test
    fun client_import_returns_no_data_when_first_read_is_empty() = runTest {
        val transport = FakeBluetoothClientImportTransport(
            firstChunk = ByteArray(0),
        )
        val manager = BluetoothClientImportManager(
            waitForFirstChunk = transport::waitForFirstChunk,
            drainAvailableBytes = transport::drainAvailableBytes,
            currentTimeMs = { testScheduler.currentTime },
            pollIntervalMs = 100L,
        )

        val result = manager.receiveImportedFile(
            importDirectory = Files.createTempDirectory("client-import-test").toFile(),
            modelCharset = Charset.forName("GBK"),
            timeProvider = { "2026-04-14T00:00:00+08:00" },
            silenceTimeoutMs = 3_000L,
        )

        assertEquals(BluetoothClientImportResult.NoData, result)
    }

    @Test
    fun client_import_throws_when_link_drops_after_first_chunk() = runTest {
        val transport = FakeBluetoothClientImportTransport(
            firstChunk = "PARTIAL".toByteArray(),
            failOnDrain = IOException("bluetooth_link_lost"),
        )
        val manager = BluetoothClientImportManager(
            waitForFirstChunk = transport::waitForFirstChunk,
            drainAvailableBytes = transport::drainAvailableBytes,
            currentTimeMs = { testScheduler.currentTime },
            pollIntervalMs = 100L,
        )

        val failure = runCatching {
            manager.receiveImportedFile(
                importDirectory = Files.createTempDirectory("client-import-test").toFile(),
                modelCharset = Charset.forName("GBK"),
                timeProvider = { "2026-04-14T00:00:00+08:00" },
                silenceTimeoutMs = 3_000L,
            )
        }

        assertTrue(failure.isFailure)
        assertEquals("bluetooth_link_lost", failure.exceptionOrNull()?.message)
    }
}

private class FakeBluetoothClientImportTransport(
    private val firstChunk: ByteArray,
    private val drainedChunks: ArrayDeque<ByteArray> = ArrayDeque(),
    private val failOnDrain: IOException? = null,
) {
    var firstReadCalls: Int = 0
    var drainCalls: Int = 0

    suspend fun waitForFirstChunk(): ByteArray {
        firstReadCalls += 1
        return firstChunk
    }

    suspend fun drainAvailableBytes(maxBytes: Int): ByteArray {
        drainCalls += 1
        failOnDrain?.let { throw it }
        val next = drainedChunks.removeFirstOrNull() ?: return ByteArray(0)
        return if (next.size <= maxBytes) next else next.copyOf(maxBytes)
    }
}
