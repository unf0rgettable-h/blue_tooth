package com.unforgettable.bluetoothcollector.data.import_

import com.unforgettable.bluetoothcollector.data.bluetooth.TransportConnectionMode
import com.unforgettable.bluetoothcollector.data.instrument.InstrumentCatalog

object ImportProfileRegistry {
    fun resolve(
        brandId: String?,
        modelId: String?,
    ): ImportProfile {
        val modelExists = InstrumentCatalog.models.any { it.brandId == brandId && it.modelId == modelId }
        return when {
            brandId == "leica" && modelId == "TS60" -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.EXPERIMENTAL,
                liveReceiveLabel = "GeoCOM实时测量",
                actionLabel = "查看TS60连接方案",
                guidanceMessage = "TS60 / Captivate：推荐优先使用 WLAN、Cable RS232 或 USB 路径；Android 蓝牙 RFCOMM 仅作为实验诊断入口，不能视为稳定批量导出能力。",
                protocolSummary = "Captivate推荐WLAN/线缆；Android蓝牙仅实验诊断",
                executionMode = ImportExecutionMode.RECEIVER_STREAM,
                transportMode = TransportConnectionMode.RECEIVER,
                capability = ts60CaptivateCapability(),
            )

            modelExists -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.SUPPORTED,
                liveReceiveLabel = "开始接收",
                actionLabel = "导入存储数据",
                guidanceMessage = "按当前已验证的导入流程执行。",
                protocolSummary = "标准蓝牙导入流程",
                executionMode = ImportExecutionMode.CLIENT_STREAM,
                transportMode = TransportConnectionMode.CLIENT,
                capability = verifiedBluetoothClientCapability(),
            )

            else -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.UNSUPPORTED,
                liveReceiveLabel = "开始接收",
                actionLabel = "查看限制说明",
                guidanceMessage = "该型号在当前版本未建立批量导入 profile。",
                protocolSummary = "未建模",
                executionMode = ImportExecutionMode.GUIDANCE_ONLY,
                transportMode = TransportConnectionMode.CLIENT,
                capability = unsupportedCapability(),
            )
        }
    }

    /**
     * 已验证的经典蓝牙 client stream 能力。
     *
     * 这是 TS09/FlexLine 当前可用路径，后续 agent 修改 TS60 时不能影响此能力。
     */
    private fun verifiedBluetoothClientCapability(): InstrumentTransferCapability {
        val route = TransferRouteOption(
            route = TransferRoute.CLASSIC_BLUETOOTH_CLIENT_STREAM,
            confidence = TransferConfidence.VERIFIED_CLIENT_STREAM,
            label = "经典蓝牙导入",
            detail = "App 连接已配对仪器并读取导出的字节流。",
        )
        return InstrumentTransferCapability(
            primaryRoute = route,
            recommendedRoutes = listOf(route),
        )
    }

    /**
     * TS60/Captivate 能力边界。
     *
     * Leica 资料更明确的第三方 GeoCOM 路径是 WLAN/线缆；Android RFCOMM server
     * 只保留为诊断路径，避免 UI 或后续 agent 把它误判为已支持批量导出。
     */
    private fun ts60CaptivateCapability(): InstrumentTransferCapability {
        val wlan = TransferRouteOption(
            route = TransferRoute.GEOCOM_WLAN,
            confidence = TransferConfidence.RECOMMENDED_VENDOR_ROUTE,
            label = "GeoCOM WLAN",
            detail = "推荐路径：通过 Captivate 的 GeoCOM WLAN 连接第三方设备。",
        )
        val rs232 = TransferRouteOption(
            route = TransferRoute.CABLE_RS232,
            confidence = TransferConfidence.RECOMMENDED_VENDOR_ROUTE,
            label = "Cable RS232",
            detail = "推荐路径：通过 TS60/MS60/TM60 的 RS232 线缆端口连接。",
        )
        val usb = TransferRouteOption(
            route = TransferRoute.USB_CABLE,
            confidence = TransferConfidence.RECOMMENDED_VENDOR_ROUTE,
            label = "USB Cable",
            detail = "推荐路径：通过 Captivate 支持的 USB 线缆连接。",
        )
        val androidReceiver = TransferRouteOption(
            route = TransferRoute.ANDROID_RFCOMM_RECEIVER,
            confidence = TransferConfidence.EXPERIMENTAL_DIAGNOSTIC,
            label = "Android蓝牙实验监听",
            detail = "仅用于实机验证 TS60 是否会主动连入普通 Android RFCOMM 服务。",
        )
        return InstrumentTransferCapability(
            primaryRoute = androidReceiver,
            recommendedRoutes = listOf(wlan, rs232, usb),
            experimentalRoutes = listOf(androidReceiver),
            limitation = "普通 Android 手机蓝牙未证明可被 TS60 识别为稳定导出目标。",
        )
    }

    /**
     * 未建模仪器的显式能力，避免调用方通过空列表误判为“还有默认路径可试”。
     */
    private fun unsupportedCapability(): InstrumentTransferCapability {
        val route = TransferRouteOption(
            route = TransferRoute.CLASSIC_BLUETOOTH_CLIENT_STREAM,
            confidence = TransferConfidence.UNSUPPORTED,
            label = "未建模",
            detail = "当前版本没有为该型号建立可执行导入路径。",
        )
        return InstrumentTransferCapability(
            primaryRoute = route,
            recommendedRoutes = emptyList(),
            limitation = "该型号需要先建立导入 profile。",
        )
    }
}
