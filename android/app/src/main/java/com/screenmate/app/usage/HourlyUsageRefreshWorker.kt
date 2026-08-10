package com.screenmate.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.AppUsageEntity
import com.screenmate.app.core.database.entity.DailyUsageEntity
import com.screenmate.app.core.util.DateUtils

class HourlyUsageRefreshWorker(
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

        val dailyData = UsageCollector.getDailyUsage(applicationContext, todayDate)
        if (dailyData != null) {
            val existing = database.dailyUsageDao().getByDateDirect(todayDate)
            val syncedToScreenMate = existing?.syncedToScreenMate ?: false
            val syncedToCloud = existing?.syncedToCloud ?: false

            if (existing != null) {
                database.appUsageDao().deleteByDailyId(existing.id)
            }

            val dailyEntity = DailyUsageEntity(
                id = existing?.id ?: 0,
                usageDate = todayDate,
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

            // Update a last-aggregated timestamp in preferences
            preferences.lastSyncTimestamp = System.currentTimeMillis()
        }

        return Result.success()
    }
}
