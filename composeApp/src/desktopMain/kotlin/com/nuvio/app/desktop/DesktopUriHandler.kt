package com.nuvio.app.desktop

import androidx.compose.ui.platform.UriHandler
import java.awt.Desktop
import java.net.URI

/**
 * `UriHandler` for Compose Desktop that delegates to the native OS shell so
 * the URL is opened exactly as a browser would handle it.
 *
 * Compose Desktop's default `UriHandler` calls `Desktop.browse(URI(url))`,
 * which uses Java's strict RFC 3986 parser. Real-world URLs commonly contain
 * characters that the parser rejects (e.g. `|` in Stremio/Torrentio addon
 * configuration paths), causing `URI(...)` to throw and the click to silently
 * fail. Browsers and the Windows shell tolerate these characters, so we go
 * through the OS shell on Windows/macOS/Linux and only fall back to
 * `Desktop.browse` when no shell command is available.
 */
internal class DesktopUriHandler : UriHandler {
    override fun openUri(uri: String) {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return

        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        val command: List<String>? = when {
            // rundll32 hands the URL straight to ShellExecute via FileProtocolHandler;
            // no cmd.exe parsing in between, so `|`, `&`, `^`, etc. travel through unchanged.
            osName.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", trimmed)
            osName.contains("mac") || osName.contains("darwin") -> listOf("open", trimmed)
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") ->
                listOf("xdg-open", trimmed)
            else -> null
        }

        if (command != null) {
            val started = runCatching {
                ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
            }.isSuccess
            if (started) return
        }

        // Last-resort fallback. URI(...) here is the strict parser that triggered the
        // original problem, but if we got this far we've exhausted the OS-level paths
        // anyway. Wrapped in runCatching so a malformed URL doesn't crash the UI.
        runCatching {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(trimmed))
                }
            }
        }
    }
}
