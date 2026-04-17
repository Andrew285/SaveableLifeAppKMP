package org.simpleapps.saveablekmp.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.simpleapps.saveablekmp.data.repository.SaveableRepository

// SyncManager.kt — новий файл
class SyncManager(
    private val repository: SaveableRepository,
    private val authManager: GoogleAuthManager,
) {
    private val syncTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val flow: SharedFlow<Unit> = syncTrigger.asSharedFlow()

    fun trigger() {
        if (authManager.isSignedIn()) {
            syncTrigger.tryEmit(Unit)
        }
    }
}