package com.unforgettable.bluetoothcollector.data.ftp

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkAddressProviderTest {

    @Test
    fun preferred_ipv4_ignores_loopback_and_ipv6_addresses() {
        val provider = NetworkAddressProvider(
            interfacesProvider = {
                listOf(
                    listOf(InetAddress.getByName("127.0.0.1")),
                    listOf(InetAddress.getByName("fe80::1")),
                    listOf(InetAddress.getByName("192.168.43.1")),
                )
            },
        )

        assertEquals("192.168.43.1", provider.preferredIpv4Address())
    }
}
