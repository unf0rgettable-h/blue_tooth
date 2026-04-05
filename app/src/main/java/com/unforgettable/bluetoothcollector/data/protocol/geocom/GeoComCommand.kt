package com.unforgettable.bluetoothcollector.data.protocol.geocom

sealed class GeoComCommand(
    val rpcId: Int,
) {
    abstract fun toRequest(): String

    data object GetSimpleMeasurement : GeoComCommand(2108) {
        override fun toRequest(): String = "%R1Q,2108:1,1\r\n"
    }

    data object GetCoordinate : GeoComCommand(2082) {
        override fun toRequest(): String = "%R1Q,2082:1,1\r\n"
    }
}
