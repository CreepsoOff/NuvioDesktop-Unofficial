package com.nuvio.app.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement

/**
 * Persists main window size and maximized state between sessions (same store as [DesktopPreferences]).
 */
internal object DesktopWindowStateStore {
    private const val namespace = "nuvio_desktop_window"
    private const val keyWidthDp = "width_dp"
    private const val keyHeightDp = "height_dp"
    private const val keyMaximized = "maximized"

    data class Saved(val widthDp: Int, val heightDp: Int, val maximized: Boolean)

    fun load(): Saved? {
        val w = DesktopPreferences.getInt(namespace, keyWidthDp) ?: return null
        val h = DesktopPreferences.getInt(namespace, keyHeightDp) ?: return null
        if (w < MinWidthDp || h < MinHeightDp) return null
        val maximized = DesktopPreferences.getBoolean(namespace, keyMaximized) ?: false
        return Saved(w, h, maximized)
    }

    /**
     * Skips fullscreen so we do not persist fullscreen bounds as the next floating size.
     */
    fun save(size: DpSize, placement: WindowPlacement) {
        if (placement == WindowPlacement.Fullscreen) return

        val maximized = placement == WindowPlacement.Maximized
        val w = size.width.value.toInt().coerceAtLeast(MinWidthDp)
        val h = size.height.value.toInt().coerceAtLeast(MinHeightDp)
        DesktopPreferences.putInt(namespace, keyWidthDp, w)
        DesktopPreferences.putInt(namespace, keyHeightDp, h)
        DesktopPreferences.putBoolean(namespace, keyMaximized, maximized)
    }

    private const val MinWidthDp = 400
    private const val MinHeightDp = 300
}
