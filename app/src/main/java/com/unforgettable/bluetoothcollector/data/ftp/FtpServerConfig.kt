package com.unforgettable.bluetoothcollector.data.ftp

import java.io.File

data class FtpServerConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val rootDirectory: File,
) {
    val endpointText: String = "ftp://$host:$port"
}

data class FtpReceivedFile(
    val file: File,
    val relativePath: String,
    val sizeBytes: Long,
)

data class FtpReceiveSummary(
    val rootDirectory: File,
    val receivedFiles: List<FtpReceivedFile>,
) {
    val fileCount: Int = receivedFiles.size
    val totalSizeBytes: Long = receivedFiles.sumOf(FtpReceivedFile::sizeBytes)
}
