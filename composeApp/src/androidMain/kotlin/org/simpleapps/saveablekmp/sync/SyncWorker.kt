package org.simpleapps.saveablekmp.sync

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: SaveableRepository by inject()
    private val authManager: GoogleAuthManager by inject()
    private val driveSync: DriveSync by inject()

    override suspend fun doWork(): Result {
        println("=== SyncWorker start")
        if (!authManager.isSignedIn()) {
            println("=== SyncWorker: not signed in")
            return Result.success()
        }

        val token = authManager.getSavedToken() ?: return Result.success()
        driveSync.setToken(token)

        return try {
            withContext(Dispatchers.IO) {
                // Pull нових даних
                val remoteItems = driveSync.pullItems()
                remoteItems.forEach { repository.insertItem(it) }
                println("=== SyncWorker pulled ${remoteItems.size} items")

                // Push незбережених
                val unsynced = repository.getUnsyncedItems()
                if (unsynced.isNotEmpty()) {
                    driveSync.pushItems()
                    println("=== SyncWorker pushed ${unsynced.size} items")
                }
            }
            Result.success()
        } catch (e: Exception) {
            println("=== SyncWorker error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "saveable_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES, // мінімум для WorkManager
                5, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}