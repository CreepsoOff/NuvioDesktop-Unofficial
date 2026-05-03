package com.nuvio.app

import androidx.compose.runtime.staticCompositionLocalOf

val LocalDesktopWindow = staticCompositionLocalOf<java.awt.Window?> { null }