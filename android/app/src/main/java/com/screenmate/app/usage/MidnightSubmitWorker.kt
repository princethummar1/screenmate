package com.screenmate.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.DailyUsageEntity
import com.screenmate.app.core.util.DateUtils
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first

class MidnightSubmitWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenMateApplication
        val preferences = app.preferences
        val database = app.database

        val userId = preferences.userId
        if (userId.isEmpty()) return Result.success()
        val supabase = app.supabase.getClient() ?: return Result.retry()

        try {
            val yesterday = DateUtils.daysAgo(1)
            val dailyData = UsageCollector.getDailyUsage(applicationContext, yesterday)
            if (dailyData != null) {
                val existing = database.dailyUsageDao().getByDateDirect(yesterday)
                val syncedToScreenMate = existing?.syncedToScreenMate ?: false
                val syncedToCloud = existing?.syncedToCloud ?: false

                if (existing != null) {
                    database.appUsageDao().deleteByDailyId(existing.id)
                }

                val dailyEntity = DailyUsageEntity(
                    id = existing?.id ?: 0,
                    usageDate = yesterday,
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
                    com.screenmate.app.core.database.entity.AppUsageEntity(
                        dailyUsageId = dailyId,
                        packageName = appData.packageName,
                        appLabel = appData.appLabel,
                        usageSeconds = appData.usageSeconds,
                        openCount = appData.openCount
                    )
                }
                database.appUsageDao().insertAll(appEntities)

                // Now upload to Supabase for all selected rooms
                val syncRooms = database.syncRoomDao().getSelected().first()
                if (syncRooms.isNotEmpty()) {
                    val totalMinutes = dailyData.totalScreenTimeSeconds / 60
                    for (room in syncRooms) {
                        val status = if (totalMinutes <= room.goalMinutes) "verified" else "over_goal"
                        val record = com.screenmate.app.usage.DailyLogRemote(
                            user_id = userId,
                            room_id = room.roomId,
                            screenshot_url = "android_auto",
                            screen_time_minutes = totalMinutes,
                            log_date = yesterday,
                            status = status,
                            source = "android_auto"
                        )
                        supabase.from("daily_logs").upsert(record) {
                            onConflict = "room_id,user_id,log_date"
                        }
                    }
                    database.dailyUsageDao().markSyncedToCloud(yesterday)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
