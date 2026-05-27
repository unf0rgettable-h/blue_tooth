package com.unforgettable.bluetoothcollector.data.ftp

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFtpServerManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun ftp_server_accepts_pasv_stor_and_lists_received_file() = runBlocking {
        val root = tempFolder.newFolder("ftp-root")
        val manager = LocalFtpServerManager(
            addressProvider = NetworkAddressProvider { listOf(listOf(java.net.InetAddress.getByName("127.0.0.1"))) },
            passwordGenerator = { "123456" },
        )
        val config = manager.start(rootDirectory = root, preferredPort = 0)!!
        val client = FtpTestClient(config.host, config.port)

        assertTrue(client.read().startsWith("220"))
        assertTrue(client.command("USER ${config.username}").startsWith("331"))
        assertTrue(client.command("PASS ${config.password}").startsWith("230"))
        client.storeViaPasv("DATA/job.dbx", "DBX BYTES")
        val listing = client.listViaPasv("/")

        assertEquals("DBX BYTES", File(root, "DATA/job.dbx").readText())
        assertTrue(listing.contains("DATA"))
        assertTrue(manager.receiveState.value is FtpReceiveState.Running)
        client.command("QUIT")
        manager.stop()
        Unit
    }

    @Test
    fun ftp_server_rejects_path_escape() = runBlocking {
        val root = tempFolder.newFolder("ftp-root")
        val manager = LocalFtpServerManager(
            addressProvider = NetworkAddressProvider { listOf(listOf(java.net.InetAddress.getByName("127.0.0.1"))) },
            passwordGenerator = { "123456" },
        )
        val config = manager.start(rootDirectory = root, preferredPort = 0)!!
        val client = FtpTestClient(config.host, config.port)

        client.read()
        client.command("USER ${config.username}")
        client.command("PASS ${config.password}")
        val response = client.storeViaPasv("../escape.dbx", "bad")

        assertTrue(response.startsWith("550"))
        assertTrue(File(root.parentFile, "escape.dbx").exists().not())
        client.command("QUIT")
        manager.stop()
        Unit
    }

    private class FtpTestClient(host: String, port: Int) {
        private val socket = Socket(host, port)
        private val reader = BufferedReader(InputStreamReader(socket.getInputStream(), UTF_8))
        private val writer = OutputStreamWriter(socket.getOutputStream(), UTF_8)

        fun read(): String = reader.readLine()

        fun command(command: String): String {
            writer.write("$command\r\n")
            writer.flush()
            return reader.readLine()
        }

        fun storeViaPasv(path: String, content: String): String {
            val dataSocket = openPassiveSocket()
            writer.write("STOR $path\r\n")
            writer.flush()
            val opening = reader.readLine()
            if (!opening.startsWith("150")) {
                dataSocket.close()
                return opening
            }
            dataSocket.getOutputStream().use { it.write(content.toByteArray(UTF_8)) }
            return reader.readLine()
        }

        fun listViaPasv(path: String): String {
            val dataSocket = openPassiveSocket()
            writer.write("LIST $path\r\n")
            writer.flush()
            assertTrue(reader.readLine().startsWith("150"))
            val listing = dataSocket.getInputStream().bufferedReader().readText()
            dataSocket.close()
            assertTrue(reader.readLine().startsWith("226"))
            return listing
        }

        private fun openPassiveSocket(): Socket {
            val response = command("PASV")
            val numbers = response.substringAfter('(').substringBefore(')')
                .split(',')
                .map(String::toInt)
            val host = numbers.take(4).joinToString(".")
            val port = numbers[4] * 256 + numbers[5]
            return Socket(host, port)
        }
    }
}
