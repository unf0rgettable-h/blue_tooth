package com.unforgettable.bluetoothcollector.data.ftp

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Android 本地 FTP Server。
 *
 * Captivate 的 FTP data transfer 需要一个标准 FTP server；这里实现项目接收所需的
 * 登录、目录、被动/主动数据连接、上传和列表命令，避免把 TS60 项目传输塞进蓝牙链路。
 */
class LocalFtpServerManager(
    private val addressProvider: NetworkAddressProvider = NetworkAddressProvider(),
    private val passwordGenerator: () -> String = { Random.nextInt(100_000, 1_000_000).toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FtpServerController {
    private val mutableReceiveState = MutableStateFlow<FtpReceiveState>(FtpReceiveState.Idle)
    override val receiveState: StateFlow<FtpReceiveState> = mutableReceiveState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val stateMutex = Mutex()

    @Volatile
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var activeConfig: FtpServerConfig? = null
    private val receivedFiles = linkedMapOf<String, FtpReceivedFile>()

    /**
     * 启动 FTP 控制端口。
     *
     * preferredPort 为 0 时由系统分配空闲端口，单元测试使用该模式避免端口冲突。
     */
    override suspend fun start(
        rootDirectory: File,
        preferredPort: Int,
    ): FtpServerConfig? = withContext(ioDispatcher) {
        stop()
        mutableReceiveState.value = FtpReceiveState.Starting
        val host = addressProvider.preferredIpv4Address()
        val password = passwordGenerator()
        val socket = runCatching {
            ServerSocket(preferredPort).apply { reuseAddress = true }
        }.onFailure { error ->
            mutableReceiveState.value = FtpReceiveState.Failed("ftp_server_start_failed", error.message)
        }.getOrNull() ?: return@withContext null

        rootDirectory.mkdirs()
        val config = FtpServerConfig(
            host = host,
            port = socket.localPort,
            username = DEFAULT_USERNAME,
            password = password,
            rootDirectory = rootDirectory,
        )
        serverSocket = socket
        activeConfig = config
        receivedFiles.clear()
        publishRunning(config)
        acceptJob = scope.launch {
            acceptLoop(socket, config)
        }
        config
    }

    /**
     * 停止 FTP 服务并返回本轮接收摘要。
     */
    override suspend fun stop(): FtpReceiveSummary {
        val config = activeConfig
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancelAndJoin()
        acceptJob = null
        val summary = FtpReceiveSummary(
            rootDirectory = config?.rootDirectory ?: File("."),
            receivedFiles = stateMutex.withLock { receivedFiles.values.toList() },
        )
        activeConfig = null
        mutableReceiveState.value = if (summary.receivedFiles.isEmpty()) {
            FtpReceiveState.Idle
        } else {
            FtpReceiveState.Stopped(summary)
        }
        return summary
    }

    private suspend fun acceptLoop(socket: ServerSocket, config: FtpServerConfig) {
        while (!socket.isClosed) {
            val client = runCatching {
                runInterruptible(ioDispatcher) { socket.accept() }
            }.getOrNull() ?: break
            scope.launch {
                handleClient(client, config)
            }
        }
    }

    private suspend fun handleClient(socket: Socket, config: FtpServerConfig) = withContext(ioDispatcher) {
        socket.use { client ->
            val session = ClientSession(config = config)
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), UTF_8))
            val writer = OutputStreamWriter(client.getOutputStream(), UTF_8)
            writer.reply("220 SurvLink FTP ready")
            while (!client.isClosed) {
                val line = runCatching { reader.readLine() }.getOrNull() ?: break
                val command = line.substringBefore(' ').uppercase()
                val argument = line.substringAfter(' ', missingDelimiterValue = "")
                val shouldQuit = handleCommand(command, argument, session, writer)
                if (shouldQuit) break
            }
        }
    }

    private suspend fun handleCommand(
        command: String,
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
    ): Boolean {
        return when (command) {
            "USER" -> {
                session.username = argument
                writer.reply("331 Password required")
                false
            }
            "PASS" -> {
                session.loggedIn = session.username == session.config.username && argument == session.config.password
                writer.reply(if (session.loggedIn) "230 Login successful" else "530 Login incorrect")
                false
            }
            "SYST" -> {
                writer.reply("215 UNIX Type: L8")
                false
            }
            "FEAT" -> {
                writer.write("211-Features\r\n PASV\r\n EPSV\r\n UTF8\r\n211 End\r\n")
                writer.flush()
                false
            }
            "PWD", "XPWD" -> {
                writer.reply("257 \"${session.currentDirectory}\"")
                false
            }
            "TYPE" -> {
                writer.reply("200 Type set to $argument")
                false
            }
            "NOOP" -> {
                writer.reply("200 NOOP ok")
                false
            }
            "CWD" -> handleChangeDirectory(argument, session, writer, parent = false)
            "CDUP" -> handleChangeDirectory("..", session, writer, parent = true)
            "MKD", "XMKD" -> handleMakeDirectory(argument, session, writer)
            "PASV" -> handlePassive(session, writer, extended = false)
            "EPSV" -> handlePassive(session, writer, extended = true)
            "PORT" -> handlePort(argument, session, writer)
            "LIST" -> handleList(argument, session, writer, namesOnly = false)
            "NLST" -> handleList(argument, session, writer, namesOnly = true)
            "STOR" -> handleStore(argument, session, writer, append = false)
            "APPE" -> handleStore(argument, session, writer, append = true)
            "SIZE" -> handleSize(argument, session, writer)
            "MDTM" -> handleModifiedTime(argument, session, writer)
            "QUIT" -> {
                writer.reply("221 Goodbye")
                true
            }
            else -> {
                writer.reply("502 Command not implemented")
                false
            }
        }
    }

    private fun handleChangeDirectory(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
        parent: Boolean,
    ): Boolean {
        val target = session.resolve(argument.ifBlank { "/" })
        if (target == null || !target.exists() || !target.isDirectory) {
            writer.reply("550 Directory unavailable")
            return false
        }
        val relative = session.root.toPath().relativize(target.canonicalFile.toPath()).toString()
            .replace(File.separatorChar, '/')
        session.currentDirectory = if (relative.isBlank() || parent && target.canonicalFile == session.root) {
            "/"
        } else {
            "/$relative"
        }
        writer.reply("250 Directory changed")
        return false
    }

    private fun handleMakeDirectory(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
    ): Boolean {
        val target = session.resolve(argument)
        if (target == null) {
            writer.reply("550 Invalid path")
            return false
        }
        target.mkdirs()
        writer.reply("257 \"${target.name}\" created")
        return false
    }

    private fun handlePassive(
        session: ClientSession,
        writer: OutputStreamWriter,
        extended: Boolean,
    ): Boolean {
        session.closePassiveSocket()
        val passiveSocket = ServerSocket(0)
        session.passiveSocket = passiveSocket
        val port = passiveSocket.localPort
        if (extended) {
            writer.reply("229 Entering Extended Passive Mode (|||$port|)")
        } else {
            val hostParts = session.config.host.split('.').mapNotNull(String::toIntOrNull)
            val host = if (hostParts.size == 4) hostParts else listOf(127, 0, 0, 1)
            writer.reply("227 Entering Passive Mode (${host.joinToString(",")},${port / 256},${port % 256})")
        }
        return false
    }

    private fun handlePort(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
    ): Boolean {
        val parts = argument.split(',').mapNotNull(String::toIntOrNull)
        if (parts.size != 6) {
            writer.reply("501 Invalid PORT")
            return false
        }
        val host = parts.take(4).joinToString(".")
        val port = parts[4] * 256 + parts[5]
        session.activeAddress = InetAddress.getByName(host) to port
        writer.reply("200 PORT command successful")
        return false
    }

    private suspend fun handleList(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
        namesOnly: Boolean,
    ): Boolean {
        val target = session.resolve(argument.ifBlank { session.currentDirectory })
        if (target == null || !target.exists()) {
            writer.reply("550 Path unavailable")
            return false
        }
        if (!session.hasPendingDataConnection()) {
            writer.reply("425 Use PASV/EPSV/PORT first")
            return false
        }
        writer.reply("150 Opening data connection")
        val dataSocket = session.openDataSocket()
        if (dataSocket == null) {
            writer.reply("425 Data connection failed")
            return false
        }
        dataSocket.use { socket ->
            val output = OutputStreamWriter(socket.getOutputStream(), UTF_8)
            val files = if (target.isDirectory) target.listFiles().orEmpty().toList() else listOf(target)
            files.forEach { file ->
                val line = if (namesOnly) file.name else file.toUnixListLine()
                output.write("$line\r\n")
            }
            output.flush()
        }
        writer.reply("226 Transfer complete")
        return false
    }

    private suspend fun handleStore(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
        append: Boolean,
    ): Boolean {
        val target = session.resolve(argument)
        if (target == null) {
            writer.reply("550 Invalid path")
            return false
        }
        if (!session.hasPendingDataConnection()) {
            writer.reply("425 Use PASV/EPSV/PORT first")
            return false
        }
        writer.reply("150 Opening data connection")
        val dataSocket = session.openDataSocket()
        if (dataSocket == null) {
            writer.reply("425 Data connection failed")
            return false
        }
        dataSocket.use { socket ->
            target.parentFile?.mkdirs()
            FileOutputStream(target, append).use { output ->
                socket.getInputStream().copyTo(output)
            }
        }
        registerReceivedFile(session.config, target)
        writer.reply("226 Transfer complete")
        return false
    }

    private fun handleSize(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
    ): Boolean {
        val target = session.resolve(argument)
        if (target == null || !target.isFile) {
            writer.reply("550 File unavailable")
        } else {
            writer.reply("213 ${target.length()}")
        }
        return false
    }

    private fun handleModifiedTime(
        argument: String,
        session: ClientSession,
        writer: OutputStreamWriter,
    ): Boolean {
        val target = session.resolve(argument)
        if (target == null || !target.exists()) {
            writer.reply("550 File unavailable")
        } else {
            val formatted = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(target.lastModified()))
            writer.reply("213 $formatted")
        }
        return false
    }

    private suspend fun registerReceivedFile(config: FtpServerConfig, file: File) {
        val root = config.rootDirectory.canonicalFile
        val canonical = file.canonicalFile
        val relativePath = root.toPath().relativize(canonical.toPath()).toString()
            .replace(File.separatorChar, '/')
        stateMutex.withLock {
            receivedFiles[relativePath] = FtpReceivedFile(
                file = canonical,
                relativePath = relativePath,
                sizeBytes = canonical.length(),
            )
            publishRunning(config)
        }
    }

    private fun publishRunning(config: FtpServerConfig) {
        val files = receivedFiles.values.toList()
        mutableReceiveState.value = FtpReceiveState.Running(
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password,
            rootDirectory = config.rootDirectory,
            receivedFiles = files,
            totalBytes = files.sumOf(FtpReceivedFile::sizeBytes),
        )
    }

    private data class ClientSession(
        val config: FtpServerConfig,
        var username: String? = null,
        var loggedIn: Boolean = false,
        var currentDirectory: String = "/",
        var passiveSocket: ServerSocket? = null,
        var activeAddress: Pair<InetAddress, Int>? = null,
    ) {
        val root: File = config.rootDirectory.canonicalFile

        /**
         * 将 FTP 路径解析到根目录内。
         *
         * 返回 null 表示客户端试图越界访问，必须拒绝写入。
         */
        fun resolve(path: String): File? {
            val normalizedPath = path.ifBlank { currentDirectory }
            val relative = if (normalizedPath.startsWith("/")) {
                normalizedPath.trimStart('/')
            } else {
                listOf(currentDirectory.trim('/'), normalizedPath)
                    .filter(String::isNotBlank)
                    .joinToString("/")
            }
            val target = File(root, relative).canonicalFile
            val parent = (if (target.exists()) target else target.parentFile ?: root).canonicalFile
            return if (target.toPath().startsWith(root.toPath()) && parent.toPath().startsWith(root.toPath())) {
                target
            } else {
                null
            }
        }

        suspend fun openDataSocket(): Socket? {
            passiveSocket?.let { server ->
                passiveSocket = null
                return runCatching { runInterruptible { server.accept() } }
                    .also { runCatching { server.close() } }
                    .getOrNull()
            }
            activeAddress?.let { (address, port) ->
                activeAddress = null
                return runCatching { Socket(address, port) }.getOrNull()
            }
            return null
        }

        fun hasPendingDataConnection(): Boolean {
            return passiveSocket != null || activeAddress != null
        }

        fun closePassiveSocket() {
            runCatching { passiveSocket?.close() }
            passiveSocket = null
        }
    }

    private fun File.toUnixListLine(): String {
        val type = if (isDirectory) "d" else "-"
        return "$type rw-r--r-- 1 owner group ${length()} Jan 01 00:00 $name"
    }

    private fun OutputStreamWriter.reply(line: String) {
        write("$line\r\n")
        flush()
    }

    companion object {
        private const val DEFAULT_USERNAME = "survlink"
    }
}
