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
                verdict = ImportProfileVerdict.GUIDANCE_ONLY,
                liveReceiveLabel = "接收实时GSI数据",
                actionLabel = "查看导出说明",
                guidanceMessage = "TS60 / Captivate：实时测量走 GeoCOM。ASCII 导出与 GSI output 需按独立连接路径配置，本版本不再默认复用 TS09 的批量导入流程。",
                protocolSummary = "Captivate 独立路径",
                executionMode = ImportExecutionMode.GUIDANCE_ONLY,
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
