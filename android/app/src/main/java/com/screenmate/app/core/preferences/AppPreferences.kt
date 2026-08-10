package com.screenmate.app.core.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("screenmate_prefs", Context.MODE_PRIVATE)

    var supabaseUrl: String
        get() = prefs.getString("supabaseUrl", "") ?: ""
        set(value) = prefs.edit().putString("supabaseUrl", value).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("supabaseAnonKey", "") ?: ""
        set(value) = prefs.edit().putString("supabaseAnonKey", value).apply()

    var tmdbApiKey: String
        get() = prefs.getString("tmdbApiKey", "") ?: ""
        set(value) = prefs.edit().putString("tmdbApiKey", value).apply()

    var tmdbReadAccessToken: String
        get() = prefs.getString("tmdbReadAccessToken", "") ?: ""
        set(value) = prefs.edit().putString("tmdbReadAccessToken", value).apply()

    var openRouterApiKey: String
        get() = prefs.getString("openRouterApiKey", "") ?: ""
        set(value) = prefs.edit().putString("openRouterApiKey", value).apply()

    var openRouterModel: String
        get() = prefs.getString("openRouterModel", "google/gemini-2.0-flash-001") ?: "google/gemini-2.0-flash-001"
        set(value) = prefs.edit().putString("openRouterModel", value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean("isSetupComplete", false)
        set(value) = prefs.edit().putBoolean("isSetupComplete", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("isLoggedIn", false)
        set(value) = prefs.edit().putBoolean("isLoggedIn", value).apply()

    var userId: String
        get() = prefs.getString("userId", "") ?: ""
        set(value) = prefs.edit().putString("userId", value).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("lastSyncTimestamp", 0L)
        set(value) = prefs.edit().putLong("lastSyncTimestamp", value).apply()

    var aiCommentaryEnabled: Boolean
        get() = prefs.getBoolean("aiCommentaryEnabled", true)
        set(value) = prefs.edit().putBoolean("aiCommentaryEnabled", value).apply()

    var backfillDays: Int
        get() = prefs.getInt("backfillDays", 7)
        set(value) = prefs.edit().putInt("backfillDays", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
