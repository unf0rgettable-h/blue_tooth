package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.nio.charset.Charset

data class ImportedFileInfo(
    val file: File,
    val sizeBytes: Long,
    val format: ImportedFileFormat,
    val receivedAt: String,
    val sourceChannel: ImportedSourceChannel = ImportedSourceChannel.BLUETOOTH_STREAM,
    val fileCount: Int = 1,
    val totalSizeBytes: Long = sizeBytes,
)

/**
 * 导入来源 channel。
 *
 * 这里区分 Bluetooth 和 FTP，是为了让 TS09 的串口导入与 TS60 的项目文件接收互不污染。
 */
enum class ImportedSourceChannel {
    BLUETOOTH_STREAM,
    BLUETOOTH_RFCOMM_RECEIVER,
    FTP_WLAN_PROJECT,
}

enum class ImportedFileFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
) {
    ZIP("ZIP项目包", "zip", "application/zip"),
    DBX("Leica DBX", "dbx", "application/octet-stream"),
    XML("LandXML", "xml", "text/xml"),
    GSI("GSI", "gsi", "text/plain"),
    DXF("DXF", "dxf", "application/dxf"),
    CSV("CSV", "csv", "text/csv"),
    TXT("文本", "txt", "text/plain"),
    UNKNOWN("未知", "dat", "application/octet-stream");

    companion object {
        /**
         * 根据文件扩展名和头部内容识别导入格式。
         *
         * 扩展名优先用于 ZIP/DBX 这类二进制或专有格式；开放文本格式再读取头部判断。
         */
        fun detect(
            header: ByteArray,
            charset: Charset = Charset.forName("GBK"),
            fileName: String? = null,
        ): ImportedFileFormat {
            when (fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()) {
                "zip" -> return ZIP
                "dbx" -> return DBX
                "xml" -> return XML
                "dxf" -> return DXF
                "csv" -> return CSV
                "txt" -> return TXT
                "gsi" -> return GSI
            }
            val text = header.toString(charset).trimStart()
            return when {
                header.size >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() -> ZIP
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
