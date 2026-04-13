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
                liveReceiveLabel = "接收实时GSI数据",
                actionLabel = "启动导出接收",
                guidanceMessage = "TS60 / Captivate：手机进入可搜索/可连接接收模式，等待仪器通过蓝牙导出 ASCII 数据。该路径仍需实机验证。",
                protocolSummary = "Captivate 蓝牙导出到手机",
                executionMode = ImportExecutionMode.RECEIVER_STREAM,
                transportMode = TransportConnectionMode.RECEIVER,
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
            )
        }
    }
}
