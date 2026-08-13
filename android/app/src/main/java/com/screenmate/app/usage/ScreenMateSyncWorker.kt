package com.screenmate.app.usage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class DailyLogRemote(
    val user_id: String,
    val room_id: String,
    val screenshot_url: String,
    val screen_time_minutes: Long,
    val log_date: String,
    val status: String,
    val source: String
)

@Serializable
data class RoomMemberRemote(
    val user_id: String,
    val room_id: String,
    val total_points: Int,
    val current_streak: Int,
    val best_streak: Int
)

@Serializable
data class DailyLogCheck(
    val id: String? = null,
    val user_id: String,
    val room_id: String,
    val log_date: String,
    val screen_time_minutes: Long,
    val status: String
)

@Serializable
data class DeviceUsageDailyRemote(
    val user_id: String,
    val usage_date: String,
    val total_screen_time_seconds: Long,
    val unlock_count: Int,
    val app_open_count: Int,
    val first_usage_at: String? = null,
    val last_usage_at: String? = null,
    val timezone: String? = null
)

@Serializable
data class DeviceUsageAppRemote(
    val daily_id: Long,
    val package_name: String,
    val app_label: String,
    val usage_seconds: Long,
    val open_count: Int
)

@Serializable
data class DeviceUsageDailyId(
    val id: Long
)

class ScreenMateSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ScreenMateSync"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenMateApplication
        val preferences = app.preferences
        val database = app.database

        val userId = preferences.userId
        if (userId.isEmpty()) {
            Log.w(TAG, "No userId, skipping sync")
            return Result.success()
        }
        val supabase = app.supabase.getClient() ?: return Result.retry()

        try {
            val syncRooms = database.syncRoomDao().getSelected().first()
            val unsyncedUsage = database.dailyUsageDao().getUnsynced()
            Log.d(TAG, "Found ${unsyncedUsage.size} unsynced daily records")

            for (usage in unsyncedUsage) {
                val totalMinutes = usage.totalScreenTimeSeconds / 60

                // ── 1. Sync to challenge rooms (daily_logs) ──
                if (syncRooms.isNotEmpty()) {
                    for (room in syncRooms) {
                        val status = if (totalMinutes <= room.goalMinutes) "verified" else "over_goal"

                        val record = DailyLogRemote(
                            user_id = userId,
                            room_id = room.roomId,
                            screenshot_url = "android_auto",
                            screen_time_minutes = totalMinutes,
                            log_date = usage.usageDate,
                            status = status,
                            source = "android_auto"
                        )

                        supabase.from("daily_logs").upsert(record) {
                            onConflict = "room_id,user_id,log_date"
                        }

                        Log.d(TAG, "Upserted daily_log for ${usage.usageDate} room=${room.roomName}: ${totalMinutes}min, status=$status")
                    }
                }

                // ── 2. Sync to personal device_usage_daily table ──
                try {
                    val personalRecord = DeviceUsageDailyRemote(
                        user_id = userId,
                        usage_date = usage.usageDate,
                        total_screen_time_seconds = usage.totalScreenTimeSeconds,
                        unlock_count = usage.unlockCount,
                        app_open_count = usage.appOpenCount,
                        first_usage_at = usage.firstUsageAt,
                        last_usage_at = usage.lastUsageAt,
                        timezone = usage.timezone
                    )

                    supabase.from("device_usage_daily").upsert(personalRecord) {
                        onConflict = "user_id,usage_date"
                    }

                    // Get the daily_id for inserting app usage
                    val dailyIdResult = supabase.from("device_usage_daily")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("usage_date", usage.usageDate)
                            }
                        }
                        .decodeList<DeviceUsageDailyId>()

                    val dailyId = dailyIdResult.firstOrNull()?.id
                    if (dailyId != null) {
                        // Delete old app records and insert fresh
                        supabase.from("device_usage_apps").delete {
                            filter { eq("daily_id", dailyId) }
                        }

                        // Load app usage from local Room DB
                        val localApps = database.appUsageDao().getByDailyId(usage.id).first()
                        if (localApps.isNotEmpty()) {
                            val appRecords = localApps.map { app ->
                                DeviceUsageAppRemote(
                                    daily_id = dailyId,
                                    package_name = app.packageName,
                                    app_label = app.appLabel,
                                    usage_seconds = app.usageSeconds,
                                    open_count = app.openCount
                                )
                            }
                            supabase.from("device_usage_apps").insert(appRecords)
                        }
                    }

                    Log.d(TAG, "Synced personal usage for ${usage.usageDate}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync personal data for ${usage.usageDate}: ${e.message}")
                    // Don't fail the whole sync — personal tables are secondary
                }

                // Mark local record as synced to cloud
                database.dailyUsageDao().markSyncedToCloud(usage.usageDate)
            }

            // ── 3. Recalculate points & streaks for each room from ALL daily_logs ──
            for (room in syncRooms) {
                try {
                    // Fetch ALL logs for this user in this room
                    val allLogs = supabase.from("daily_logs")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("room_id", room.roomId)
                            }
                        }
                        .decodeList<DailyLogCheck>()
                        .sortedBy { it.log_date }

                    // Recalculate total points from all logs (matches web formula)
                    var totalPoints = 0
                    for (log in allLogs) {
                        totalPoints += if (log.screen_time_minutes <= room.goalMinutes) {
                            100 + (room.goalMinutes - log.screen_time_minutes).toInt()
                        } else {
                            20
                        }
                    }

                    // Calculate current streak from the end
                    var currentStreak = 0
                    for (log in allLogs.reversed()) {
                        if (log.status == "verified") {
                            currentStreak++
                        } else {
                            break
                        }
                    }

                    // Fetch existing member to preserve best_streak
                    val memberResult = supabase.from("room_members")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("room_id", room.roomId)
                            }
                        }
                        .decodeList<RoomMemberRemote>()

                    val existingBest = memberResult.firstOrNull()?.best_streak ?: 0
                    val bestStreak = maxOf(existingBest, currentStreak)

                    // Update room_members with recalculated values
                    supabase.from("room_members")
                        .update(
                            mapOf(
                                "total_points" to totalPoints,
                                "current_streak" to currentStreak,
                                "best_streak" to bestStreak
                            )
                        ) {
                            filter {
                                eq("user_id", userId)
                                eq("room_id", room.roomId)
                            }
                        }

                    Log.d(TAG, "Room ${room.roomName}: recalculated totalPts=$totalPoints, streak=$currentStreak, best=$bestStreak")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to recalculate points for room ${room.roomName}: ${e.message}")
                }
            }

            preferences.lastSyncTimestamp = System.currentTimeMillis()
            Log.d(TAG, "Sync completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return Result.retry()
        }
    }
}
