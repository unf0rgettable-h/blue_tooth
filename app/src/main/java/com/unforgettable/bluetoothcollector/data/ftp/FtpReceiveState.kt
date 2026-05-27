package com.unforgettable.bluetoothcollector.data.ftp

import java.io.File

/**
 * WLAN/FTP 项目文件接收状态。
 *
 * 该状态只服务 TS60 项目文件 channel，不能复用蓝牙 receiver 状态，避免两个协议路径互相误判。
 */
sealed interface FtpReceiveState {
    data object Idle : FtpReceiveState
    data object Starting : FtpReceiveState

    data class Running(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val rootDirectory: File,
        val receivedFiles: List<FtpReceivedFile>,
        val totalBytes: Long,
    ) : FtpReceiveState {
        val endpointText: String = "ftp://$host:$port"
    }

    data class Failed(val reason: String, val detail: String? = null) : FtpReceiveState
    data class Stopped(val summary: FtpReceiveSummary) : FtpReceiveState
}
