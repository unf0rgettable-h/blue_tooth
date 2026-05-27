package com.unforgettable.bluetoothcollector.data.import_

/**
 * 仪器数据传输路径的稳定标识。
 *
 * 这里用枚举而不是散落字符串，是为了让 coding agents 能通过 `rg TransferRoute`
 * 快速定位所有协议假设和 UI 消费点，避免 TS60 这类特殊能力继续分散在界面文案里。
 */
enum class TransferRoute {
    CLASSIC_BLUETOOTH_CLIENT_STREAM,
    ANDROID_RFCOMM_RECEIVER,
    FTP_WLAN_PROJECT_TRANSFER,
    CABLE_RS232,
    USB_CABLE,
}

/**
 * 传输路径的可信度。
 *
 * 名称刻意包含能力边界，便于 agent 在修改导入逻辑时先判断是否允许作为默认路径。
 */
enum class TransferConfidence {
    VERIFIED_CLIENT_STREAM,
    VERIFIED_APP_FTP_RECEIVER,
    RECOMMENDED_VENDOR_ROUTE,
    EXPERIMENTAL_DIAGNOSTIC,
    UNSUPPORTED,
}

/**
 * 单条可展示、可执行或可诊断的传输路径。
 *
 * @property route 稳定机器可读路径，用于测试和 UI 分支。
 * @property confidence 路径可信度，用于决定主入口还是实验入口。
 * @property label 面向用户的短标签，UI 直接展示。
 * @property detail 路径边界说明，避免把协议假设写死在 Composable 中。
 */
data class TransferRouteOption(
    val route: TransferRoute,
    val confidence: TransferConfidence,
    val label: String,
    val detail: String,
)

/**
 * 某个仪器型号的数据传输能力快照。
 *
 * `primaryRoute` 是当前 app 可以直接执行或应首先解释的入口；
 * `recommendedRoutes` 是厂家文档更明确的连接方式；
 * `experimentalRoutes` 只用于诊断或实机验证，不能被 UI 包装成稳定功能。
 */
data class InstrumentTransferCapability(
    val primaryRoute: TransferRouteOption,
    val recommendedRoutes: List<TransferRouteOption>,
    val experimentalRoutes: List<TransferRouteOption> = emptyList(),
    val limitation: String? = null,
)
