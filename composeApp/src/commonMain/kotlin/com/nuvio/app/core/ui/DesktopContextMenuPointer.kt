package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

/**
 * Desktop (mouse): invokes [onContextMenu] on secondary (right) click, matching mobile long-press menus.
 * Touch/mobile platforms: no-op; long-press remains unchanged.
 */
expect fun Modifier.desktopContextMenuPointer(onContextMenu: (() -> Unit)?): Modifier
