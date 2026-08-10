package com.screenmate.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.network.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class RemoteTaskRecord(
    val id: String,
    val user_id: String,
    val title: String,
    val description: String?,
    val is_completed: Boolean
)

class PersonalSyncWorker(
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

        syncTasks(supabase, userId, database)
        // TODO: syncHabits(supabase, userId, database)
        // TODO: syncEntries(supabase, userId, database)
        // TODO: syncMedia(supabase, userId, database)
        // TODO: syncReading(supabase, userId, database)
        // TODO: syncPlaylists(supabase, userId, database)
        // TODO: syncItems(supabase, userId, database)
        // TODO: syncWishlistCategories(supabase, userId, database)
        // TODO: syncWishlistItems(supabase, userId, database)
        // TODO: syncNotes(supabase, userId, database)
        // TODO: syncJournalEntries(supabase, userId, database)
        // TODO: syncBookmarkCategories(supabase, userId, database)
        // TODO: syncBookmarks(supabase, userId, database)
        // TODO: syncDailyUsage(supabase, userId, database)
        // TODO: syncAppUsage(supabase, userId, database)

        return Result.success()
    }

    private suspend fun syncTasks(supabase: SupabaseClient, userId: String, database: com.screenmate.app.core.database.AppDatabase) {
        // TODO: Implement syncing for tasks
        /*
        val pending = database.taskDao().getPending()
        for (task in pending) {
            try {
                when (task.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        supabase.from("personal_tasks").insert(task.toRemote(userId))
                        database.taskDao().update(task.copy(syncStatus = SyncStatus.SYNCED))
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        supabase.from("personal_tasks").update(task.toRemote(userId)) { filter { eq("id", task.id) } }
                        database.taskDao().update(task.copy(syncStatus = SyncStatus.SYNCED))
                    }
                    SyncStatus.PENDING_DELETE -> {
                        supabase.from("personal_tasks").delete { filter { eq("id", task.id) } }
                        database.taskDao().delete(task)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Log and continue
            }
        }
        */
    }
}
