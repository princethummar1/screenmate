package com.screenmate.app.core.network

import com.screenmate.app.core.preferences.AppPreferences
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient

class SupabaseModule(private val preferences: AppPreferences) {
    private var client: SupabaseClient? = null
    private var configuredUrl: String = ""
    private var configuredKey: String = ""

    @Synchronized
    fun getClient(): SupabaseClient? {
        val url = preferences.supabaseUrl
        val key = preferences.supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return null
        if (client == null || url != configuredUrl || key != configuredKey) {

            client = createSupabaseClient(url, key) {
                install(Auth)
                install(Postgrest)
            }
            configuredUrl = url
            configuredKey = key
        }
        return client
    }

    fun isConfigured(): Boolean {
        return preferences.supabaseUrl.isNotBlank() && preferences.supabaseAnonKey.isNotBlank()
    }

    @Synchronized
    fun invalidate() {

        client = null
    }
}
