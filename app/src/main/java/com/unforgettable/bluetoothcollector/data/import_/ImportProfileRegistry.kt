package com.unforgettable.bluetoothcollector.data.import_

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
                verdict = ImportProfileVerdict.SUPPORTED,
                liveReceiveLabel = "接收实时GSI数据",
                actionLabel = "接收导出数据",
                guidanceMessage = "TS60 / Captivate：整项目导出使用 ASCII format file -> RS232 interface；实时点存输出对应 GSI output 连接。",
                protocolSummary = "Captivate 导出 / GSI output",
            )

            modelExists -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.SUPPORTED,
                liveReceiveLabel = "开始接收",
                actionLabel = "导入存储数据",
                guidanceMessage = "按当前已验证的导入流程执行。",
                protocolSummary = "标准蓝牙导入流程",
            )

            else -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.UNSUPPORTED,
                liveReceiveLabel = "开始接收",
                actionLabel = "查看限制说明",
                guidanceMessage = "该型号在当前版本未建立批量导入 profile。",
                protocolSummary = "未建模",
            )
        }
    }
}
