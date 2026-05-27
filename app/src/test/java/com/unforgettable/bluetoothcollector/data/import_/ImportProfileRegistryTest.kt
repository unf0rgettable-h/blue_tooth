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
        assertEquals(TransferConfidence.VERIFIED_CLIENT_STREAM, profile.capability.primaryRoute.confidence)
        assertEquals(TransferRoute.CLASSIC_BLUETOOTH_CLIENT_STREAM, profile.capability.primaryRoute.route)
    }

    @Test
    fun ts60_profile_prefers_documented_captivate_routes_and_keeps_android_bluetooth_diagnostic_only() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "TS60",
        )

        assertEquals(ImportProfileVerdict.SUPPORTED, profile.verdict)
        assertEquals("WLAN项目接收", profile.liveReceiveLabel)
        assertEquals("启动WLAN项目接收", profile.actionLabel)
        assertEquals("Captivate WLAN/FTP项目文件传输；蓝牙仅实验诊断", profile.protocolSummary)
        assertEquals(ImportExecutionMode.FTP_SERVER, profile.executionMode)
        assertEquals(TransportConnectionMode.RECEIVER, profile.transportMode)
        assertEquals(TransferRoute.FTP_WLAN_PROJECT_TRANSFER, profile.capability.primaryRoute.route)
        assertEquals(TransferConfidence.VERIFIED_APP_FTP_RECEIVER, profile.capability.primaryRoute.confidence)
        assertEquals(
            listOf(
                TransferRoute.FTP_WLAN_PROJECT_TRANSFER,
                TransferRoute.CABLE_RS232,
                TransferRoute.USB_CABLE,
            ),
            profile.capability.recommendedRoutes.map { it.route },
        )
        assertEquals(
            TransferConfidence.EXPERIMENTAL_DIAGNOSTIC,
            profile.capability.experimentalRoutes.single().confidence,
        )
        assertEquals(
            TransferRoute.ANDROID_RFCOMM_RECEIVER,
            profile.capability.experimentalRoutes.single().route,
        )
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
