package com.unforgettable.bluetoothcollector.domain.model

data class InstrumentModel(
    val modelId: String,
    val brandId: String,
    val displayName: String,
    val delimiterStrategy: DelimiterStrategy,
    val expectedTransport: String = CLASSIC_BLUETOOTH_SPP,
    val firmwareFamily: String? = null,
    val dataCharsetName: String = "GBK",
) {
    companion object {
        const val CLASSIC_BLUETOOTH_SPP = "CLASSIC_BLUETOOTH_SPP"
    }
}
