package com.unforgettable.bluetoothcollector.data.ftp

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * 选择手机当前可给 TS60 访问的 IPv4 地址。
 *
 * Android 热点网关地址在不同厂商上不完全一致，所以这里只做自动候选；
 * UI 仍会展示“如连接失败，请以系统热点/网络页面显示的地址为准”的提示。
 */
class NetworkAddressProvider(
    private val interfacesProvider: () -> List<List<InetAddress>> = {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp }
            .map { it.inetAddresses.toList() }
    },
) {
    fun preferredIpv4Address(): String {
        return interfacesProvider()
            .flatten()
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?.hostAddress
            ?: LOOPBACK_HOST
    }

    companion object {
        const val LOOPBACK_HOST: String = "127.0.0.1"
    }
}
