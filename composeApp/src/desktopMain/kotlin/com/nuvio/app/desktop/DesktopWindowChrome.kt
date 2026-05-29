package com.nuvio.app.desktop

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

/**
 * Darkens the **native** Windows title bar via DWM (immersive dark mode) so it matches Nuvio's
 * near-black UI instead of the default white caption.
 *
 * Deliberately minimal: it sets ONLY the dark-mode caption attribute and keeps the window
 * *decorated*. It does NOT set rounded-corner or border-color attributes — those persist into the
 * native borderless-fullscreen window state and black out fullscreen video (DWM can no longer do
 * direct fullscreen presentation). Window shape is left to the OS default (Win11 rounds decorated
 * windows natively). Dark mode is a non-client/caption attribute, so it can't affect the video
 * surface. Aero Snap / resize / maximize all keep working because the native frame is untouched.
 *
 * Best-effort: guarded and HRESULT-logged; no-ops on non-Windows / older OS.
 */
object WindowsChromePolish {
    private const val S_OK = 0
    // Dark caption attribute: index 20 on Win10 2004+/Win11, 19 on older Win10 (1809–1909).
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1 = 19

    private interface Dwmapi : StdCallLibrary {
        fun DwmSetWindowAttribute(
            hwnd: Pointer,
            dwAttribute: Int,
            pvAttribute: IntByReference,
            cbAttribute: Int,
        ): Int
    }

    fun apply(window: ComposeWindow) {
        if (System.getProperty("os.name")?.contains("Windows", ignoreCase = true) != true) return
        runCatching {
            val hwnd = Native.getWindowPointer(window) ?: run {
                DesktopRuntimeLog.warn("window chrome polish skipped: null HWND")
                return
            }
            val dwm = Native.load("dwmapi", Dwmapi::class.java)

            // ONLY the dark-caption hint — NOT corner-preference or border-color, which persist into
            // the borderless-fullscreen popup window and black out fullscreen video.
            var darkHr = dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, IntByReference(1), 4)
            if (darkHr != S_OK) {
                darkHr = dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1, IntByReference(1), 4)
            }
            DesktopRuntimeLog.info("window chrome polish hr: darkMode=$darkHr (0=S_OK)")
        }.onFailure {
            DesktopRuntimeLog.warn("window chrome polish failed ${it::class.simpleName}:${it.message}")
        }
    }
}
