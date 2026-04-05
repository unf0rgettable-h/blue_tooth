package com.unforgettable.bluetoothcollector.data.protocol.geocom

import com.unforgettable.bluetoothcollector.data.protocol.ProtocolTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeoComClientTest {

    @Test
    fun sendCommand_serializes_concurrent_requests() = runTest {
        val transport = FakeProtocolTransport()
        val client = GeoComClient(
            transport = transport,
            maxReadAttempts = 5,
            readTimeoutMillis = 10,
        )

        val first = async { client.sendCommand(GeoComCommand.GetSimpleMeasurement) }
        transport.awaitSendCount(1)

        val second = async { client.sendCommand(GeoComCommand.GetCoordinate) }
        runCurrent()

        assertEquals(listOf("%R1Q,2108:1,1\r\n"), transport.sentRequests)

        transport.enqueueRead("%R1P,0,0:0,1.0,2.0,3.0\r\n".toByteArray())
        assertEquals(listOf("1.0", "2.0", "3.0"), first.await()?.parameters)

        transport.awaitSendCount(2)
        assertEquals(
            listOf("%R1Q,2108:1,1\r\n", "%R1Q,2082:1,1\r\n"),
            transport.sentRequests,
        )

        transport.enqueueRead("%R1P,0,0:0,4.0,5.0,6.0\r\n".toByteArray())
        assertEquals(listOf("4.0", "5.0", "6.0"), second.await()?.parameters)
    }

    @Test
    fun sendCommand_returns_null_after_bounded_empty_reads() = runTest {
        val transport = FakeProtocolTransport()
        repeat(3) { transport.enqueueRead(ByteArray(0)) }
        val client = GeoComClient(
            transport = transport,
            maxReadAttempts = 3,
            readTimeoutMillis = 10,
        )

        val response = client.sendCommand(GeoComCommand.GetSimpleMeasurement)

        assertNull(response)
        assertEquals(3, transport.readAttempts)
    }
}

private class FakeProtocolTransport : ProtocolTransport {
    val sentRequests = mutableListOf<String>()
    private val sendCount = CompletableDeferred<Unit>()
    private val reads = ArrayDeque<ByteArray?>()
    var readAttempts: Int = 0
        private set

    override suspend fun sendBytes(data: ByteArray) {
        sentRequests += data.toString(Charsets.UTF_8)
        if (!sendCount.isCompleted) {
            sendCount.complete(Unit)
        }
    }

    override suspend fun blockingReadBytes(): ByteArray {
        return blockingReadBytesWithTimeout(0) ?: ByteArray(0)
    }

    override suspend fun blockingReadBytesWithTimeout(timeoutMs: Long): ByteArray? {
        readAttempts += 1
        while (reads.isEmpty()) {
            delay(1)
        }
        return reads.removeFirst()
    }

    suspend fun awaitSendCount(expected: Int) {
        withTimeout(1_000L) {
            while (sentRequests.size < expected) {
                delay(1)
            }
        }
    }

    fun enqueueRead(value: ByteArray?) {
        reads += value
    }
}
