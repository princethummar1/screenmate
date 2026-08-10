package com.screenmate.app.usage

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val PERIODIC_COLLECTION_NAME = "periodic_usage_collection"
    private const val SYNC_CHAIN_NAME = "immediate_sync_chain"

    fun schedulePeriodicCollection(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<UsageCollectionWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_COLLECTION_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    fun scheduleImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val collectionWork = OneTimeWorkRequestBuilder<UsageCollectionWorker>()
            .setConstraints(constraints)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<ScreenMateSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(SYNC_CHAIN_NAME, ExistingWorkPolicy.REPLACE, collectionWork)
            .then(syncWork)
            .enqueue()
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
