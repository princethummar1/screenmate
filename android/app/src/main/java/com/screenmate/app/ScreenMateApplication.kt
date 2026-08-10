package com.screenmate.app

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.screenmate.app.core.database.AppDatabase
import com.screenmate.app.core.preferences.AppPreferences

class ScreenMateApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var supabase: com.screenmate.app.core.network.SupabaseModule
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)

        // Populate preferences from BuildConfig (credentials come from local.properties)
        populateFromBuildConfig()

        supabase = com.screenmate.app.core.network.SupabaseModule(preferences)
        // Migration: add ownerUserId to selected account-scoped tables (reading_items, wishlist_items, bookmark_items)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE reading_items ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE wishlist_items ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE bookmark_items ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE custom_lists ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE media ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE habits ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE habit_entries ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE playlist_items ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE playlists ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE custom_list_items ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE journal_entries ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE bookmark_categories ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE wishlist_categories ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE daily_usage ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { }
            }
        }

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "screenmate_db"
        ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigration().build()

        // Schedule periodic hourly local usage refresh
        try {
            val workRequest = PeriodicWorkRequestBuilder<com.screenmate.app.usage.HourlyUsageRefreshWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "hourly_usage_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            Log.w("ScreenMate", "Failed to schedule hourly usage refresh: ${e.message}")
        }

        // On app resume, trigger a quick local usage refresh if today's data is stale
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                try {
                    val prefs = preferences
                    val last = prefs.lastSyncTimestamp
                    // If last sync/aggregate was more than 5 minutes ago, enqueue quick refresh
                    val age = System.currentTimeMillis() - last
                    if (age > 5 * 60 * 1000) {
                        val oneTime = androidx.work.OneTimeWorkRequestBuilder<com.screenmate.app.usage.HourlyUsageRefreshWorker>()
                            .setInputData(workDataOf("triggeredFrom" to "app_resume"))
                            .build()
                        WorkManager.getInstance(this).enqueue(oneTime)
                    }
                } catch (e: Exception) {
                    Log.w("ScreenMate", "Error scheduling resume refresh: ${e.message}")
                }
            }
        })

        // Schedule midnight submission (daily) with initial delay until next local midnight
        try {
            val now = java.time.ZonedDateTime.now()
            val tomorrow = now.plusDays(1).toLocalDate().atStartOfDay(now.zone)
            val initialDelayMillis = java.time.Duration.between(now, tomorrow).toMillis()

            val midnightWork = androidx.work.PeriodicWorkRequestBuilder<com.screenmate.app.usage.MidnightSubmitWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "midnight_submit",
                ExistingPeriodicWorkPolicy.KEEP,
                midnightWork
            )
        } catch (e: Exception) {
            Log.w("ScreenMate", "Failed to schedule midnight submission: ${e.message}")
        }
    }

    private fun populateFromBuildConfig() {
        val bc = BuildConfig::class.java
        try {
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            val tmdbKey = BuildConfig.TMDB_API_KEY
            val tmdbToken = BuildConfig.TMDB_READ_ACCESS_TOKEN
            val openRouterKey = BuildConfig.OPENROUTER_API_KEY
            val openRouterModel = BuildConfig.OPENROUTER_MODEL

            if (supabaseUrl.isNotBlank()) preferences.supabaseUrl = supabaseUrl
            if (supabaseKey.isNotBlank()) preferences.supabaseAnonKey = supabaseKey
            if (tmdbKey.isNotBlank()) preferences.tmdbApiKey = tmdbKey
            if (tmdbToken.isNotBlank()) preferences.tmdbReadAccessToken = tmdbToken
            if (openRouterKey.isNotBlank()) preferences.openRouterApiKey = openRouterKey
            if (openRouterModel.isNotBlank()) preferences.openRouterModel = openRouterModel

            // Mark setup as complete if Supabase is configured
            if (supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()) {
                preferences.isSetupComplete = true
            }

            if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
                Log.w("ScreenMate", "Missing SUPABASE_URL or SUPABASE_ANON_KEY in local.properties. App may not sync.")
            }
        } catch (e: Exception) {
            Log.e("ScreenMate", "Failed to read BuildConfig fields. Check local.properties.", e)
        }
    }

    companion object {
        lateinit var instance: ScreenMateApplication
            private set
    }
}

