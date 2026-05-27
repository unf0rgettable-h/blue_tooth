package com.unforgettable.bluetoothcollector.data.bluetooth

/**
 * Receiver 诊断的稳定原因码。
 *
 * 这些 code 是给 coding agents 搜索和维护状态机用的，不依赖 UI 文案；
 * 修改 TS60 蓝牙实验路径时，应优先扩展这里而不是新增散落字符串。
 */
enum class ReceiverDiagnosticCode(val stableKey: String) {
    BLUETOOTH_ADAPTER_UNAVAILABLE("bluetooth_adapter_not_available"),
    BLUETOOTH_DISABLED("bluetooth_disabled"),
    MISSING_BLUETOOTH_CONNECT_PERMISSION("missing_bluetooth_connect_permission"),
    MISSING_RECEIVER_PERMISSION("missing_receiver_permission"),
    DISCOVERABILITY_REQUESTED("discoverability_requested"),
    DISCOVERABILITY_DENIED("discoverability_denied"),
    DISCOVERABILITY_ENABLED("discoverability_enabled"),
    DISCOVERABILITY_EXPIRED("discoverability_expired"),
    SPP_UUID_DECLARED("spp_uuid_declared"),
    RFCOMM_SECURE_INSECURE_ATTEMPT("rfcomm_secure_insecure_attempt"),
    PAIRING_REQUIRED_HINT("pairing_required_hint"),
    RECEIVER_MODE_NOT_SUPPORTED("receiver_mode_not_supported"),
    RECEIVER_CONFLICTS_ACTIVE_OPERATION("receiver_conflicts_active_operation"),
    DISCONNECT_CLIENT_BEFORE_LISTEN("disconnect_client_before_listen"),
    WAITING_FOR_TS60_CONNECTION("waiting_for_ts60_connection"),
    RFCOMM_LISTENING("rfcomm_listening"),
    RFCOMM_SERVER_OPEN_FAILED("rfcomm_server_open_failed"),
    NO_INCOMING_CONNECTION("no_incoming_connection"),
    NO_DATA_RECEIVED("no_data_received"),
    RECEIVER_COMPLETED("receiver_completed"),
    RECEIVER_CANCELLED("receiver_cancelled"),
    RECEIVER_LINK_LOST_IGNORED("receiver_link_lost_ignored"),
    BLUETOOTH_LINK_LOST("bluetooth_link_lost"),
    ADAPTER_POWERED_OFF("adapter_powered_off"),
}

/**
 * 诊断严重级别。
 *
 * UI 可以用它决定颜色；测试和 agent 可以用它判断失败是否需要改变流程。
 */
enum class ReceiverDiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
}

/**
 * Receiver 诊断条目。
 *
 * message 是当前中文 UI 文案；code/severity 才是稳定维护契约。
 */
data class ReceiverDiagnosticEntry(
    val code: ReceiverDiagnosticCode,
    val severity: ReceiverDiagnosticSeverity,
    val message: String,
)
