package com.screenmate.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.AppUsageEntity
import com.screenmate.app.core.database.entity.DailyUsageEntity
import com.screenmate.app.core.util.DateUtils
import java.util.UUID

class UsageCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenMateApplication
        val database = app.database
        val preferences = app.preferences

        if (!UsageCollector.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val todayDate = DateUtils.todayDate()
        val backfillDays = preferences.backfillDays

        for (i in 0..backfillDays) {
            val targetDate = DateUtils.daysAgo(i)
            
            val dailyData = UsageCollector.getDailyUsage(applicationContext, targetDate)
            if (dailyData != null) {
                val existing = database.dailyUsageDao().getByDateDirect(targetDate)
                val syncedToScreenMate = existing?.syncedToScreenMate ?: false
                var syncedToCloud = existing?.syncedToCloud ?: false
                
                if (existing != null && existing.totalScreenTimeSeconds != dailyData.totalScreenTimeSeconds) {
                    syncedToCloud = false
                }
                
                // Delete old app usage if updating
                if (existing != null) {
                    database.appUsageDao().deleteByDailyId(existing.id)
                }
                
                val dailyEntity = DailyUsageEntity(
                    id = existing?.id ?: 0,
                    usageDate = targetDate,
                    totalScreenTimeSeconds = dailyData.totalScreenTimeSeconds,
                    unlockCount = dailyData.unlockCount,
                    appOpenCount = dailyData.appOpenCount,
                    firstUsageAt = dailyData.firstUsageAt?.let { DateUtils.formatTime(it) },
                    lastUsageAt = dailyData.lastUsageAt?.let { DateUtils.formatTime(it) },
                    timezone = existing?.timezone ?: java.util.TimeZone.getDefault().id,
                    syncedToScreenMate = syncedToScreenMate,
                    syncedToCloud = syncedToCloud,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                val dailyId = database.dailyUsageDao().insert(dailyEntity)
                
                val appEntities = dailyData.apps.map { appData ->
                    AppUsageEntity(
                        dailyUsageId = dailyId,
                        packageName = appData.packageName,
                        appLabel = appData.appLabel,
                        usageSeconds = appData.usageSeconds,
                        openCount = appData.openCount
                    )
                }
                database.appUsageDao().insertAll(appEntities)
            }
        }

        // Schedule sync worker
        val syncWorkRequest = OneTimeWorkRequestBuilder<ScreenMateSyncWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(syncWorkRequest)

        return Result.success()
    }
}
