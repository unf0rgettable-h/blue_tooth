package com.unforgettable.bluetoothcollector.data.import_

import com.unforgettable.bluetoothcollector.data.bluetooth.TransportConnectionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportProfileRegistryTest {

    @Test
    fun ts09_profile_is_supported_for_batch_import() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "TS09",
        )

        assertEquals(ImportProfileVerdict.SUPPORTED, profile.verdict)
        assertEquals("开始接收", profile.liveReceiveLabel)
        assertEquals("导入存储数据", profile.actionLabel)
        assertEquals(ImportExecutionMode.CLIENT_STREAM, profile.executionMode)
        assertEquals(TransportConnectionMode.CLIENT, profile.transportMode)
    }

    @Test
    fun ts60_profile_uses_dedicated_captivate_import_path() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "TS60",
        )

        assertEquals(ImportProfileVerdict.EXPERIMENTAL, profile.verdict)
        assertEquals("接收实时GSI数据", profile.liveReceiveLabel)
        assertEquals("启动导出接收", profile.actionLabel)
        assertEquals("Captivate 蓝牙导出到手机", profile.protocolSummary)
        assertEquals(ImportExecutionMode.RECEIVER_STREAM, profile.executionMode)
        assertEquals(TransportConnectionMode.RECEIVER, profile.transportMode)
    }

    @Test
    fun unknown_model_falls_back_to_unsupported() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "UNKNOWN_MODEL",
        )

        assertEquals(ImportProfileVerdict.UNSUPPORTED, profile.verdict)
        assertEquals("查看限制说明", profile.actionLabel)
        assertEquals(ImportExecutionMode.GUIDANCE_ONLY, profile.executionMode)
    }
}
