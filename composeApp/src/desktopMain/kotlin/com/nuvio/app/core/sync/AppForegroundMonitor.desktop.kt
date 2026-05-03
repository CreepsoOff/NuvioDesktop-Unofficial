package com.nuvio.app.core.sync

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal actual object AppForegroundMonitor {
    actual fun events(): Flow<Unit> = callbackFlow {
        trySend(Unit)
        awaitClose {}
    }
}