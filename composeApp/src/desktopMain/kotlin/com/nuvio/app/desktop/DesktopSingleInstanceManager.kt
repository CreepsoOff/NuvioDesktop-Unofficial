package com.nuvio.app.desktop

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Single-instance IPC on loopback only: [PORT_FIRST]..[PORT_LAST], handshake [PING_LINE]/[PONG_LINE].
 * Persists chosen port under `%LOCALAPPDATA%\Nuvio\single-instance-port.txt`.
 */
internal object DesktopSingleInstanceManager {
    private const val Host = "127.0.0.1"
    private const val PORT_FIRST = 45822
    private const val PORT_LAST = 45832
    private const val PING_LINE = "PING"
    private const val PONG_LINE = "NUVIO"

    private const val CONNECT_TIMEOUT_MS = 200
    private const val READ_TIMEOUT_MS = 300
    private const val ACCEPT_SOCKET_READ_TIMEOUT_MS = 30_000

    sealed interface StartResult {
        /** Bound and listening; call [close] on shutdown to release port and port file. */
        data class Primary(val close: () -> Unit) : StartResult

        /** Another Nuvio instance received the payload; this process should exit. */
        data object ForwardedToPrimary : StartResult

        /** Could not bind any port and no Nuvio primary answered; launch normally without IPC. */
        data object NoPrimaryAvailable : StartResult
    }

    /**
     * 1) Probe persisted port + range with PING; if Nuvio responds, forward URLs and return [ForwardedToPrimary].
     * 2) Else try bind [PORT_FIRST]..[PORT_LAST]; on success write port file and return [Primary].
     * 3) Else return [NoPrimaryAvailable] (never treat "port busy" alone as "be secondary").
     */
    fun resolveStartup(
        startupUrls: List<String>,
        onUrlReceived: (String) -> Unit,
        onFocusRequested: () -> Unit,
    ): StartResult {
        val orderedProbePorts = buildOrderedProbePorts()

        for (port in orderedProbePorts) {
            if (!pingNuvioPrimary(port)) continue
            DesktopRuntimeLog.info("single-instance: PING OK on port=$port, attempting forward")
            if (forwardPayload(port, startupUrls)) {
                DesktopRuntimeLog.info("single-instance: forwarded to primary on port=$port, this instance will exit")
                return StartResult.ForwardedToPrimary
            }
            DesktopRuntimeLog.warn("single-instance: forward failed to port=$port despite PING OK; continuing resolution")
        }

        for (port in PORT_FIRST..PORT_LAST) {
            val serverSocket = tryBindLoopback(port) ?: continue
            DesktopRuntimeLog.info("single-instance: bound primary IPC on loopback port=$port")
            writePersistedPort(port)

            val running = AtomicBoolean(true)
            val worker = thread(
                isDaemon = true,
                name = "nuvio-single-instance-ipc",
            ) {
                while (running.get()) {
                    val socket = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    runCatching { socket.soTimeout = ACCEPT_SOCKET_READ_TIMEOUT_MS }
                    socket.use { acceptedSocket ->
                        handleIncomingConnection(
                            socket = acceptedSocket,
                            onUrlReceived = onUrlReceived,
                            onFocusRequested = onFocusRequested,
                        )
                    }
                }
            }

            val closer = {
                running.set(false)
                runCatching { serverSocket.close() }
                worker.interrupt()
                runCatching { deletePersistedPort() }
                Unit
            }
            return StartResult.Primary(close = closer)
        }

        DesktopRuntimeLog.warn(
            "single-instance IPC unavailable: could not bind any port in $PORT_FIRST..$PORT_LAST " +
                "and no Nuvio primary responded to PING; continuing without IPC",
        )
        return StartResult.NoPrimaryAvailable
    }

    private fun buildOrderedProbePorts(): List<Int> {
        val fromFile = readPersistedPort()
        return buildList {
            fromFile?.let { add(it) }
            for (p in PORT_FIRST..PORT_LAST) add(p)
        }.distinct()
    }

    private fun portFilePath(): Path? {
        val local = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: return null
        return Path.of(local, "Nuvio", "single-instance-port.txt")
    }

    private fun readPersistedPort(): Int? {
        val path = portFilePath() ?: return null
        return runCatching {
            val text = Files.readString(path).trim()
            text.toIntOrNull()?.takeIf { it in PORT_FIRST..PORT_LAST }
        }.getOrNull()
    }

    private fun writePersistedPort(port: Int) {
        val path = portFilePath() ?: return
        runCatching {
            Files.createDirectories(path.parent)
            Files.writeString(
                path,
                port.toString() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            DesktopRuntimeLog.info("single-instance: wrote port file ${path.fileName} -> $port")
        }.onFailure {
            DesktopRuntimeLog.warn("single-instance: failed to write port file: ${it.message}")
        }
    }

    private fun deletePersistedPort() {
        val path = portFilePath() ?: return
        runCatching {
            Files.deleteIfExists(path)
        }.onFailure {
            DesktopRuntimeLog.warn("single-instance: failed to delete port file: ${it.message}")
        }
    }

    private fun tryBindLoopback(port: Int): ServerSocket? =
        runCatching {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(InetAddress.getByName(Host), port))
            }
        }.getOrElse {
            if (it is BindException) {
                null
            } else {
                DesktopRuntimeLog.warn("single-instance: unexpected bind error port=$port: ${it.message}")
                null
            }
        }

    private fun pingNuvioPrimary(port: Int): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getByName(Host), port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                writer.write(PING_LINE)
                writer.write("\n")
                writer.flush()
                val line = reader.readLine()
                line == PONG_LINE
            }
        }.getOrElse {
            when (it) {
                is SocketTimeoutException -> false
                else -> false
            }
        }

    private fun forwardPayload(port: Int, urlArgs: List<String>): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getByName(Host), port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                writer.write(PING_LINE)
                writer.write("\n")
                writer.flush()
                val pong = reader.readLine()
                if (pong != PONG_LINE) return@runCatching false

                val payload =
                    if (urlArgs.isEmpty()) listOf("FOCUS") else urlArgs.map { "URL|$it" } + "FOCUS"
                payload.forEach { line ->
                    writer.write(line)
                    writer.write("\n")
                }
                writer.flush()
                true
            }
        }.getOrDefault(false)

    private fun handleIncomingConnection(
        socket: Socket,
        onUrlReceived: (String) -> Unit,
        onFocusRequested: () -> Unit,
    ) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
        while (true) {
            val line = reader.readLine() ?: return
            when {
                line == PING_LINE -> {
                    writer.write(PONG_LINE)
                    writer.write("\n")
                    writer.flush()
                }
                line == "FOCUS" -> onFocusRequested()
                line.startsWith("URL|") -> onUrlReceivedSafe(line.removePrefix("URL|"), onUrlReceived)
            }
        }
    }

    private fun onUrlReceivedSafe(
        url: String,
        onUrlReceived: (String) -> Unit,
    ) {
        if (!url.startsWith("nuvio://", ignoreCase = true)) return
        onUrlReceived(url)
    }
}
