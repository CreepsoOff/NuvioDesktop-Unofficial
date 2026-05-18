package com.nuvio.app.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.desktopContextMenuPointer(onContextMenu: (() -> Unit)?): Modifier {
    if (onContextMenu == null) return this
    return this.onClick(
        matcher = PointerMatcher.mouse(PointerButton.Secondary),
        onClick = onContextMenu,
    )
}
