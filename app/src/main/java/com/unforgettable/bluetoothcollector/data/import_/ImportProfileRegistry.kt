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
                verdict = ImportProfileVerdict.GUIDANCE_ONLY,
                actionLabel = "查看导入说明",
                guidanceMessage = "TS60 批量导入需要先按型号兼容说明确认可用导出路径。",
            )

            modelExists -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.SUPPORTED,
                actionLabel = "导入存储数据",
                guidanceMessage = "按当前已验证的导入流程执行。",
            )

            else -> ImportProfile(
                brandId = brandId,
                modelId = modelId,
                verdict = ImportProfileVerdict.UNSUPPORTED,
                actionLabel = "查看限制说明",
                guidanceMessage = "该型号在当前版本未建立批量导入 profile。",
            )
        }
    }
}
