package com.screenmate.app.usage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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
            if (syncRooms.isEmpty()) {
                Log.w(TAG, "No sync rooms selected")
                return Result.success()
            }

            val unsyncedUsage = database.dailyUsageDao().getUnsynced()
            Log.d(TAG, "Found ${unsyncedUsage.size} unsynced daily records")

            for (usage in unsyncedUsage) {
                val totalMinutes = usage.totalScreenTimeSeconds / 60

                for (room in syncRooms) {
                    val status = if (totalMinutes <= room.goalMinutes) "verified" else "over_goal"

                    // Upsert daily log
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

                    // Fetch current member state from Supabase
                    val memberResult = supabase.from("room_members")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("room_id", room.roomId)
                            }
                        }
                        .decodeList<RoomMemberRemote>()

                    val member = memberResult.firstOrNull()
                    if (member != null) {
                        // Calculate points for this submission
                        val points = if (totalMinutes <= room.goalMinutes) {
                            100 + (room.goalMinutes - totalMinutes).toInt()
                        } else {
                            20
                        }

                        // Calculate streak: check if previous day was under goal
                        val prevDate = com.screenmate.app.core.util.DateUtils.daysAgo(1)
                        val prevLogResult = supabase.from("daily_logs")
                            .select {
                                filter {
                                    eq("user_id", userId)
                                    eq("room_id", room.roomId)
                                    eq("log_date", prevDate)
                                }
                            }
                            .decodeList<DailyLogCheck>()

                        val prevLog = prevLogResult.firstOrNull()
                        val wasPrevUnderGoal = prevLog?.status == "verified"

                        val newStreak = if (status == "verified") {
                            if (wasPrevUnderGoal) member.current_streak + 1 else 1
                        } else {
                            0 // Over goal resets streak
                        }

                        val bestStreak = maxOf(member.best_streak, newStreak)
                        val newTotalPoints = member.total_points + points

                        // Update room_members
                        supabase.from("room_members")
                            .update(
                                mapOf(
                                    "total_points" to newTotalPoints,
                                    "current_streak" to newStreak,
                                    "best_streak" to bestStreak
                                )
                            ) {
                                filter {
                                    eq("user_id", userId)
                                    eq("room_id", room.roomId)
                                }
                            }

                        Log.d(TAG, "Synced ${usage.usageDate} for room ${room.roomName}: ${totalMinutes}min, +${points}pts, streak=$newStreak")
                    }
                }

                // Mark local record as synced
                database.dailyUsageDao().markSyncedToCloud(usage.usageDate)
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
