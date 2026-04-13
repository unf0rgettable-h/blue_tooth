package com.unforgettable.bluetoothcollector.data.export

import java.nio.charset.StandardCharsets.UTF_8

internal object ExportEncoding {
    val UTF8_BOM: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun utf8WithBom(content: String): ByteArray {
        return UTF8_BOM + content.toByteArray(UTF_8)
    }
}
