package com.screenmate.app.usage

import android.content.Context
import android.util.Log
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

    companion object {
        private const val TAG = "MidnightSubmit"
        private const val BACKFILL_DAYS = 4
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenMateApplication
        val preferences = app.preferences
        val database = app.database

        val userId = preferences.userId
        if (userId.isEmpty()) return Result.success()
        val supabase = app.supabase.getClient() ?: return Result.retry()

        try {
            // Process last 4 days (1 to BACKFILL_DAYS days ago)
            for (daysAgo in 1..BACKFILL_DAYS) {
                val targetDate = DateUtils.daysAgo(daysAgo)

                val existing = database.dailyUsageDao().getByDateDirect(targetDate)

                // Skip if already synced to cloud
                if (existing?.syncedToCloud == true) {
                    Log.d(TAG, "$targetDate already synced, skipping")
                    continue
                }

                val dailyData = UsageCollector.getDailyUsage(applicationContext, targetDate)
                if (dailyData == null) {
                    Log.d(TAG, "No usage data for $targetDate")
                    continue
                }

                // Save/update in local Room DB
                val syncedToScreenMate = existing?.syncedToScreenMate ?: false

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
                    syncedToCloud = false,
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

                // Upload to Supabase for all selected rooms (daily_logs)
                val syncRooms = database.syncRoomDao().getSelected().first()
                val totalMinutes = dailyData.totalScreenTimeSeconds / 60
                if (syncRooms.isNotEmpty()) {
                    for (room in syncRooms) {
                        val status = if (totalMinutes <= room.goalMinutes) "verified" else "over_goal"
                        val record = DailyLogRemote(
                            user_id = userId,
                            room_id = room.roomId,
                            screenshot_url = "android_auto",
                            screen_time_minutes = totalMinutes,
                            log_date = targetDate,
                            status = status,
                            source = "android_auto"
                        )
                        supabase.from("daily_logs").upsert(record) {
                            onConflict = "room_id,user_id,log_date"
                        }
                    }
                }

                // Upload to personal device_usage_daily table
                try {
                    val personalRecord = DeviceUsageDailyRemote(
                        user_id = userId,
                        usage_date = targetDate,
                        total_screen_time_seconds = dailyData.totalScreenTimeSeconds,
                        unlock_count = dailyData.unlockCount,
                        app_open_count = dailyData.appOpenCount,
                        first_usage_at = dailyData.firstUsageAt?.let { DateUtils.formatTime(it) },
                        last_usage_at = dailyData.lastUsageAt?.let { DateUtils.formatTime(it) },
                        timezone = java.util.TimeZone.getDefault().id
                    )

                    supabase.from("device_usage_daily").upsert(personalRecord) {
                        onConflict = "user_id,usage_date"
                    }

                    // Get daily_id for app records
                    val dailyIdResult = supabase.from("device_usage_daily")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("usage_date", targetDate)
                            }
                        }
                        .decodeList<DeviceUsageDailyId>()

                    val remoteDailyId = dailyIdResult.firstOrNull()?.id
                    if (remoteDailyId != null && dailyData.apps.isNotEmpty()) {
                        supabase.from("device_usage_apps").delete {
                            filter { eq("daily_id", remoteDailyId) }
                        }

                        val appRecords = dailyData.apps.map { app ->
                            DeviceUsageAppRemote(
                                daily_id = remoteDailyId,
                                package_name = app.packageName,
                                app_label = app.appLabel,
                                usage_seconds = app.usageSeconds,
                                open_count = app.openCount
                            )
                        }
                        supabase.from("device_usage_apps").insert(appRecords)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync personal data for $targetDate: ${e.message}")
                }

                database.dailyUsageDao().markSyncedToCloud(targetDate)
                Log.d(TAG, "Midnight sync: processed $targetDate (${totalMinutes}min)")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Midnight sync failed", e)
            e.printStackTrace()
            return Result.retry()
        }
    }
}
