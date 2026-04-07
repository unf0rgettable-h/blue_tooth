package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.nio.charset.Charset

data class ImportedFileInfo(
    val file: File,
    val sizeBytes: Long,
    val format: ImportedFileFormat,
    val receivedAt: String,
)

enum class ImportedFileFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
) {
    XML("LandXML", "xml", "text/xml"),
    GSI("GSI", "gsi", "text/plain"),
    DXF("DXF", "dxf", "application/dxf"),
    CSV("CSV", "csv", "text/csv"),
    TXT("文本", "txt", "text/plain"),
    UNKNOWN("未知", "dat", "application/octet-stream");

    companion object {
        fun detect(header: ByteArray, charset: Charset = Charset.forName("GBK")): ImportedFileFormat {
            val text = header.toString(charset).trimStart()
            return when {
                text.startsWith("<?xml") || text.startsWith("<LandXML") -> XML
                Regex("^\\*?\\d{6}[+\\-]").containsMatchIn(text) -> GSI
                text.startsWith("0\nSECTION") || text.startsWith("0\r\nSECTION") -> DXF
                text.contains(",") && text.lines().firstOrNull()?.count { it == ',' } ?: 0 >= 2 -> CSV
                text.isNotBlank() -> TXT
                else -> UNKNOWN
            }
        }
    }
}
