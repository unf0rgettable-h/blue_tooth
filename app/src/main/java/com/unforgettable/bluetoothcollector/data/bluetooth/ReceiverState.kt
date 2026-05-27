package com.unforgettable.bluetoothcollector.data.bluetooth

/**
 * 实验性 Bluetooth RFCOMM receiver 的可观察状态。
 *
 * 状态只描述传输阶段；失败归因使用 ReceiverDiagnosticCode，避免调用方解析字符串。
 */
sealed interface ReceiverState {
    /** 空闲：未打开 server socket。 */
    data object Idle : ReceiverState

    /** 正在请求系统蓝牙可发现性。 */
    data object RequestingDiscoverability : ReceiverState

    /** server socket 已打开，等待仪器连入。 */
    data object Listening : ReceiverState

    /** 仪器已连入，正在读取字节。 */
    data class Receiving(val bytesReceived: Long = 0) : ReceiverState

    /** 传输成功完成。 */
    data class Completed(val bytesReceived: Long, val fileName: String) : ReceiverState

    /**
     * Receiver 因明确原因失败。
     *
     * reason 保留为稳定字符串，兼容旧 UI/测试；新逻辑应使用 code。
     */
    data class Failed(
        val code: ReceiverDiagnosticCode,
        val detail: String? = null,
    ) : ReceiverState {
        val reason: String = code.stableKey
    }

    /** 用户主动取消。 */
    data object Cancelled : ReceiverState
}
