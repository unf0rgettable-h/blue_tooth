package com.unforgettable.bluetoothcollector.data.import_

enum class ImportProfileVerdict {
    SUPPORTED,
    GUIDANCE_ONLY,
    UNSUPPORTED,
}

data class ImportProfile(
    val brandId: String?,
    val modelId: String?,
    val verdict: ImportProfileVerdict,
    val actionLabel: String,
    val guidanceMessage: String,
    val protocolSummary: String,
)
