package com.unforgettable.bluetoothcollector.data.bluetooth

import java.io.ByteArrayOutputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BluetoothConnectionManagerTest {

    @Test
    fun sendBytes_writes_to_output_stream_and_flushes() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val manager = BluetoothConnectionManager(
            bluetoothAdapter = null,
            permissionChecker = BluetoothPermissionChecker(context),
        )
        val outputStream = TrackingOutputStream()
        setPrivateField(manager, "outputStream", outputStream)

        manager.sendBytes("MEASURE\r\n".toByteArray())

        assertArrayEquals("MEASURE\r\n".toByteArray(), outputStream.toByteArray())
        assertEquals(1, outputStream.flushCount)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        target.javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(target, value)
        }
    }
}

private class TrackingOutputStream : ByteArrayOutputStream() {
    var flushCount: Int = 0
        private set

    override fun flush() {
        super.flush()
        flushCount += 1
    }
}
