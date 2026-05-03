package com.nuvio.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.core.network.SupabaseConfig
import com.nuvio.app.desktop.DesktopPlayerRegistry
import com.nuvio.app.desktop.DesktopRuntimeLog
import com.nuvio.app.desktop.WindowsNativeBootstrap
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.nuvio_window_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Color as AwtColor
import kotlin.system.exitProcess

private val DesktopWindowBackground = AwtColor(0x0D, 0x0D, 0x0D)

private fun configureMacOsNativeAppearance() {
    val osName = System.getProperty("os.name")?.lowercase() ?: return
    if (!osName.contains("mac")) return
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
}

fun main() {
    DesktopRuntimeLog.initialize()
    DesktopRuntimeLog.installGlobalExceptionHandlers()
    val pid = DesktopRuntimeLog.processPid()
    DesktopRuntimeLog.info("app startup pid=$pid")
    DesktopRuntimeLog.info(
        "NUVIO_RUNTIME_PATCH_MARKER=cursor-player-session-render-shutdown-v2 " +
            "pid=$pid ts=${System.currentTimeMillis()} user.dir=${System.getProperty("user.dir")} " +
            "buildCommit=${System.getProperty("nuvio.git.commit") ?: "unknown"} " +
            "buildBranch=${System.getProperty("nuvio.git.branch") ?: "unknown"}",
    )
    Runtime.getRuntime().addShutdownHook(
        Thread {
            val startMs = System.currentTimeMillis()
            DesktopRuntimeLog.info("shutdownHook start pid=$pid")
            DesktopRuntimeLog.logNonDaemonThreads("shutdownHook:beforeClose")
            DesktopPlayerRegistry.releaseAll("shutdownHook")
            DesktopPlayerRegistry.closeAll("shutdownHook")
            // Give in-flight `mpv_terminate_destroy` calls a bounded window to
            // release the audio device and event/render threads before the JVM
            // process exits. The close threads are daemons so they do not keep
            // the JVM alive on their own; the explicit wait here is what makes
            // a clean Windows-X close actually clean.
            DesktopPlayerRegistry.awaitAllCloses(timeoutMs = 3000L)
            DesktopRuntimeLog.logNonDaemonThreads("shutdownHook:afterClose")
            DesktopRuntimeLog.info("shutdownHook end pid=$pid elapsedMs=${System.currentTimeMillis() - startMs}")
        },
    )
    DesktopRuntimeLog.info("version=${AppVersionConfig.VERSION_NAME}(${AppVersionConfig.VERSION_CODE})")
    DesktopRuntimeLog.info("os=${System.getProperty("os.name")} ${System.getProperty("os.version")}")
    DesktopRuntimeLog.info("java=${System.getProperty("java.version")}")
    DesktopRuntimeLog.info("user.dir=${System.getProperty("user.dir")}")
    DesktopRuntimeLog.info("compose.resources.dir=${System.getProperty("compose.application.resources.dir") ?: "unset"}")
    DesktopRuntimeLog.info("java.library.path=${System.getProperty("java.library.path") ?: "unset"}")
    DesktopRuntimeLog.info("supabase.url=${SupabaseConfig.URL}")
    DesktopRuntimeLog.info("supabase.anon.present=${SupabaseConfig.ANON_KEY.isNotBlank()} length=${SupabaseConfig.ANON_KEY.length}")
    WindowsNativeBootstrap.bootstrap()
    configureMacOsNativeAppearance()
    application {
        DesktopRuntimeLog.info("window composition start pid=$pid")
        Window(
            onCloseRequest = {
                val closeStartMs = System.currentTimeMillis()
                DesktopRuntimeLog.info("windowClose requested pid=$pid")
                DesktopRuntimeLog.logNonDaemonThreads("windowClose:beforeCleanup")
                // 1) Soft stop on the EDT — fast, halts MPV playback.
                // 2) Trigger the native close path explicitly: Compose's
                //    `exitApplication` does NOT always dispose the player
                //    surface before the JVM shuts down, so onDispose is not a
                //    reliable trigger for closeNative. The croix Windows must
                //    fire closeNative itself to avoid leaving Nuvio.exe alive.
                // 3) `exitApplication` to start Compose teardown.
                // 4) The shutdown hook joins in-flight close threads (bounded)
                //    so the JVM exits only after MPV has terminated cleanly.
                DesktopPlayerRegistry.releaseAll("windowClose")
                DesktopRuntimeLog.info("windowClose releaseAll done pid=$pid")
                DesktopPlayerRegistry.closeAll("windowClose")
                DesktopRuntimeLog.info("windowClose closeAll done pid=$pid")
                DesktopPlayerRegistry.awaitAllCloses(timeoutMs = 1500L)
                DesktopRuntimeLog.logNonDaemonThreads("windowClose:beforeExitApplication")
                DesktopRuntimeLog.info("windowClose exitApplication pid=$pid elapsedMs=${System.currentTimeMillis() - closeStartMs}")
                exitApplication()
                val forceExitEnabled = System.getProperty("nuvio.desktop.forceExitOnClose", "false")
                    .equals("true", ignoreCase = true)
                if (forceExitEnabled) {
                    DesktopRuntimeLog.warn("windowClose force exitProcess enabled pid=$pid")
                    DesktopRuntimeLog.logNonDaemonThreads("windowClose:beforeForceExit")
                    exitProcess(0)
                }
            },
            title = "Nuvio",
            icon = painterResource(Res.drawable.nuvio_window_icon),
        ) {
            DisposableEffect(window) {
                window.background = DesktopWindowBackground
                window.contentPane.background = DesktopWindowBackground
                window.rootPane.background = DesktopWindowBackground
                onDispose { }
            }

            CompositionLocalProvider(LocalDesktopWindow provides window) {
                App()
            }
        }
    }
}
