package com.nuvio.app.core.sync

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal actual object AppForegroundMonitor {
    actual fun events(): Flow<Unit> = callbackFlow {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                trySend(Unit)
            }
        }
        lifecycle.addObserver(observer)
        awaitClose {
            lifecycle.removeObserver(observer)
        }
    }
}
