package com.nuvio.app.features.player.desktop

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.GraphicsEnvironment
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

private object DesktopAppFullscreen {
    private var toggleHandler: ((Window?) -> Unit)? = null

    fun setToggleHandler(handler: ((Window?) -> Unit)?): () -> Unit {
        toggleHandler = handler
        return {
            if (toggleHandler === handler) {
                toggleHandler = null
            }
        }
    }

    fun toggle(window: Window? = null) {
        val handler = toggleHandler ?: return
        if (SwingUtilities.isEventDispatchThread()) {
            handler(window)
        } else {
            SwingUtilities.invokeLater { handler(window) }
        }
    }
}

internal fun registerDesktopAppFullscreenToggle(handler: (Window?) -> Unit): () -> Unit =
    DesktopAppFullscreen.setToggleHandler(handler)

internal fun toggleDesktopAppFullscreen(window: Window? = null) {
    DesktopAppFullscreen.toggle(window)
}

internal class DesktopAppFullscreenController {
    private var restoreWindowPlacement = WindowPlacement.Floating
    private var windowsFullscreenState: WindowsFullscreenState? = null

    fun toggle(window: Window, windowState: WindowState) {
        if (DesktopHostOs.current == DesktopHostOs.WINDOWS) {
            toggleWindowsFullscreen(window)
        } else {
            toggleComposeFullscreen(windowState)
        }
    }

    fun dispose(window: Window) {
        exitWindowsFullscreen(window)
    }

    private fun toggleComposeFullscreen(windowState: WindowState) {
        if (windowState.placement == WindowPlacement.Fullscreen) {
            windowState.placement = restoreWindowPlacement
        } else {
            restoreWindowPlacement = windowState.placement
                .takeUnless { it == WindowPlacement.Fullscreen }
                ?: WindowPlacement.Floating
            windowState.placement = WindowPlacement.Fullscreen
        }
    }

    private fun toggleWindowsFullscreen(window: Window) {
        if (windowsFullscreenState?.window === window) {
            exitWindowsFullscreen(window)
        } else {
            enterWindowsFullscreen(window)
        }
    }

    private fun enterWindowsFullscreen(window: Window) {
        val screenBounds = window.graphicsConfiguration?.bounds
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
        val hwnd = AwtNativeViewResolver.resolveNativeViewPointer(window)
        NativePlayerBridge.setWindowBorderlessFullscreen(
            windowHwnd = hwnd,
            fullscreen = true,
            x = screenBounds.x,
            y = screenBounds.y,
            width = screenBounds.width,
            height = screenBounds.height,
        )
        windowsFullscreenState = WindowsFullscreenState(window = window, windowHwnd = hwnd)
        window.toFront()
        window.requestFocus()
    }

    private fun exitWindowsFullscreen(window: Window) {
        val fullscreenState = windowsFullscreenState?.takeIf { it.window === window } ?: return
        NativePlayerBridge.setWindowBorderlessFullscreen(
            windowHwnd = fullscreenState.windowHwnd,
            fullscreen = false,
            x = 0,
            y = 0,
            width = 0,
            height = 0,
        )
        windowsFullscreenState = null
    }

    private data class WindowsFullscreenState(
        val window: Window,
        val windowHwnd: Long,
    )
}

internal fun installDesktopAppFullscreenShortcuts(window: Window): () -> Unit {
    val dispatcher = KeyEventDispatcher { event ->
        if (!event.isDesktopAppFullscreenShortcut()) return@KeyEventDispatcher false
        toggleDesktopAppFullscreen(window)
        true
    }
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
    return {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
    }
}

private fun KeyEvent.isDesktopAppFullscreenShortcut(): Boolean {
    if (id != KeyEvent.KEY_PRESSED) return false
    if (keyCode == KeyEvent.VK_F11) return true
    if (keyCode != KeyEvent.VK_F) return false
    val modifiers = modifiersEx
    val hasMacFullscreenModifiers =
        modifiers and KeyEvent.META_DOWN_MASK != 0 &&
            modifiers and KeyEvent.CTRL_DOWN_MASK != 0 &&
            modifiers and KeyEvent.ALT_DOWN_MASK == 0
    return hasMacFullscreenModifiers
}
