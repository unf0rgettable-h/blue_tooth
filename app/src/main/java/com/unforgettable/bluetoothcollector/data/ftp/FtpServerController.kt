package com.unforgettable.bluetoothcollector.data.ftp

import java.io.File
import kotlinx.coroutines.flow.StateFlow

interface FtpServerController {
    val receiveState: StateFlow<FtpReceiveState>

    suspend fun start(
        rootDirectory: File,
        preferredPort: Int = DEFAULT_FTP_PORT,
    ): FtpServerConfig?

    suspend fun stop(): FtpReceiveSummary

    companion object {
        const val DEFAULT_FTP_PORT: Int = 2121
    }
}
