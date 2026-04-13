package com.unforgettable.bluetoothcollector.data.import_

import com.unforgettable.bluetoothcollector.data.bluetooth.TransportConnectionMode

enum class ImportProfileVerdict {
    SUPPORTED,
    EXPERIMENTAL,
    GUIDANCE_ONLY,
    UNSUPPORTED,
}

data class ImportProfile(
    val brandId: String?,
    val modelId: String?,
    val verdict: ImportProfileVerdict,
    val liveReceiveLabel: String,
    val actionLabel: String,
    val guidanceMessage: String,
    val protocolSummary: String,
    val executionMode: ImportExecutionMode = ImportExecutionMode.GUIDANCE_ONLY,
    val transportMode: TransportConnectionMode = TransportConnectionMode.CLIENT,
)
