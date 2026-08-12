package com.screenmate.app.features.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.BuildConfig
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.SyncRoomEntity
import com.screenmate.app.core.ui.theme.*
import com.screenmate.app.usage.SyncScheduler
import com.screenmate.app.usage.UsageCollector
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(val id: String, val name: String, val goal_minutes: Int)

class SettingsViewModel : ViewModel() {
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    
    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess = _hasUsageAccess.asStateFlow()

    private val _rooms = MutableStateFlow<List<SyncRoomEntity>>(emptyList())
    val rooms = _rooms.asStateFlow()

    private val _aiEnabled = MutableStateFlow(true)
    val aiEnabled = _aiEnabled.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime = _lastSyncTime.asStateFlow()

    fun load(context: android.content.Context) {
        val prefs = (context.applicationContext as ScreenMateApplication).preferences
        _username.value = prefs.username.takeIf { it.isNotEmpty() } ?: "Unknown User"
        _hasUsageAccess.value = UsageCollector.hasUsageAccess(context)
        _aiEnabled.value = prefs.aiCommentaryEnabled

        val lastSync = prefs.lastSyncTimestamp
        _lastSyncTime.value = if (lastSync > 0) {
            val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(lastSync))
        } else "Never"

        val db = (context.applicationContext as ScreenMateApplication).database
        viewModelScope.launch {
            db.syncRoomDao().getAll().collect { _rooms.value = it }
        }
    }

    fun toggleAiEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = (context.applicationContext as ScreenMateApplication).preferences
        prefs.aiCommentaryEnabled = enabled
        _aiEnabled.value = enabled
    }

    fun testSupabaseConnection(context: android.content.Context) {
        viewModelScope.launch {
            val app = context.applicationContext as ScreenMateApplication
            val client = app.supabase.getClient()
            if (client == null) {
                Toast.makeText(context, "Supabase Client not configured", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                client.postgrest["rooms"].select()
                Toast.makeText(context, "Supabase Connection Successful!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Supabase Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun testTmdbConnection(context: android.content.Context) {
        viewModelScope.launch {
            val api = com.screenmate.app.core.network.TmdbApi()
            // Use TMDB API key (v3 style) for search and basic connectivity checks
            val connResult = api.testConnection(BuildConfig.TMDB_API_KEY)
            if (connResult.isSuccess) {
                val searchResult = api.testSearch(BuildConfig.TMDB_API_KEY)
                if (searchResult.isSuccess) {
                    Toast.makeText(context, "TMDb Connection & Search Successful!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, searchResult.exceptionOrNull()?.message ?: "TMDb Search Error", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, connResult.exceptionOrNull()?.message ?: "TMDb Connection Error", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun testOpenRouterConnection(context: android.content.Context) {
        viewModelScope.launch {
            val api = com.screenmate.app.core.network.OpenRouterApi()
            val summary = com.screenmate.app.core.network.UsageSummary(60, "App", 30, 45, 10)
            val result = api.generateCommentary(BuildConfig.OPENROUTER_API_KEY, BuildConfig.OPENROUTER_MODEL, summary)
            if (result.isSuccess) {
                Toast.makeText(context, "OpenRouter Connection Successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "OpenRouter Error: ${result.exceptionOrNull()?.message ?: "Check your API key or model"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun fetchRoomsFromSupabase(context: android.content.Context) {
        viewModelScope.launch {
            val app = context.applicationContext as ScreenMateApplication
            val client = app.supabase.getClient() ?: return@launch
            try {
                val roomsDto = client.postgrest["rooms"].select().decodeList<RoomDto>()
                val existingSelected = app.database.syncRoomDao().getSelected().firstOrNull()?.map { it.roomId }?.toSet() ?: emptySet()
                val entities = roomsDto.map { 
                    SyncRoomEntity(it.id, it.name, it.goal_minutes, selected = existingSelected.contains(it.id) || existingSelected.isEmpty()) 
                }
                app.database.syncRoomDao().upsertAll(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleRoomSelection(context: android.content.Context, roomId: String, selected: Boolean) {
        viewModelScope.launch {
            val app = context.applicationContext as ScreenMateApplication
            app.database.syncRoomDao().updateSelection(roomId, selected)
        }
    }

    fun triggerSync(context: android.content.Context) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Checking screen time..."

            try {
                val app = context.applicationContext as ScreenMateApplication
                val db = app.database
                val prefs = app.preferences

                if (!UsageCollector.hasUsageAccess(context)) {
                    _syncStatus.value = "Usage access not granted"
                    _isSyncing.value = false
                    return@launch
                }

                val daysToCheck = 4
                var updatedCount = 0

                for (i in 0 until daysToCheck) {
                    val targetDate = com.screenmate.app.core.util.DateUtils.daysAgo(i)
                    _syncStatus.value = "Checking ${if (i == 0) "today" else "${i}d ago"}..."

                    val phoneData = UsageCollector.getDailyUsage(context, targetDate)
                    val localData = db.dailyUsageDao().getByDateDirect(targetDate)

                    val phoneMinutes = phoneData?.totalScreenTimeSeconds ?: 0L
                    val localMinutes = localData?.totalScreenTimeSeconds ?: 0L

                    if (localData == null || phoneMinutes != localMinutes) {
                        val entity = com.screenmate.app.core.database.entity.DailyUsageEntity(
                            id = localData?.id ?: 0,
                            usageDate = targetDate,
                            totalScreenTimeSeconds = phoneMinutes,
                            unlockCount = phoneData?.unlockCount ?: localData?.unlockCount ?: 0,
                            appOpenCount = phoneData?.appOpenCount ?: localData?.appOpenCount ?: 0,
                            firstUsageAt = phoneData?.firstUsageAt?.let { com.screenmate.app.core.util.DateUtils.formatTime(it) } ?: localData?.firstUsageAt,
                            lastUsageAt = phoneData?.lastUsageAt?.let { com.screenmate.app.core.util.DateUtils.formatTime(it) } ?: localData?.lastUsageAt,
                            timezone = localData?.timezone ?: java.util.TimeZone.getDefault().id,
                            syncedToScreenMate = localData?.syncedToScreenMate ?: false,
                            syncedToCloud = false,
                            createdAt = localData?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        db.dailyUsageDao().insert(entity)
                        updatedCount++
                    }
                }

                _syncStatus.value = if (updatedCount > 0) {
                    "Updated $updatedCount day(s). Syncing to site..."
                } else {
                    "All days up to date. Syncing..."
                }

                SyncScheduler.scheduleImmediateSync(context)

                _syncStatus.value = "Done! $updatedCount day(s) updated. Points will refresh."
                prefs.lastSyncTimestamp = System.currentTimeMillis()
                val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
                _lastSyncTime.value = sdf.format(java.util.Date())
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signOut(context: android.content.Context, onNavigateToLogin: () -> Unit) {
        val prefs = (context.applicationContext as ScreenMateApplication).preferences
        prefs.userId = ""
        prefs.username = ""
        SyncScheduler.cancelAll(context)
        // Clear local DB to isolate account-specific data
        val app = context.applicationContext as ScreenMateApplication
        try {
            app.database.clearAllTables()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onNavigateToLogin()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load(context) }

    val username by viewModel.username.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBackground).padding(16.dp)
    ) {
        item {
            Text("Settings", color = AccentPrimary, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Account", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text("Logged in as $username", color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.signOut(context, onNavigateToLogin) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Sign Out", color = DarkBackground)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("ScreenMate Sync", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.fetchRoomsFromSupabase(context) }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)) {
                Text("Refresh Rooms", color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(rooms) { room ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = room.selected,
                    onCheckedChange = { checked -> viewModel.toggleRoomSelection(context, room.roomId, checked) },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary, uncheckedColor = TextSecondary, checkmarkColor = DarkBackground)
                )
                Text(room.roomName, color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.triggerSync(context) },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...", color = DarkBackground)
                } else {
                    Text("Sync Site Now", color = DarkBackground)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (syncStatus != null) {
                Text(syncStatus!!, color = AccentPrimary, style = MaterialTheme.typography.bodySmall)
            }
            Text("Last synced: $lastSyncTime", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
            Text("Collects screen time, syncs to site, and calculates points. Midnight updates also happen automatically.", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Screen Time", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Usage Access: ", color = TextSecondary)
                Text(if (hasUsageAccess) "Granted" else "Denied", color = if (hasUsageAccess) AccentPrimary else MaterialTheme.colorScheme.error)
            }
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)) {
                Text("Open Usage Settings", color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        item {
            Text("AI", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = aiEnabled,
                    onCheckedChange = { viewModel.toggleAiEnabled(context, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = AccentPrimary)
                )
                Text("Enable AI Commentary", color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Diagnostics", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.testSupabaseConnection(context) }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)) {
                Text("Test Supabase Connection", color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.testTmdbConnection(context) }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)) {
                Text("Test TMDb Connection", color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.testOpenRouterConnection(context) }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)) {
                Text("Test OpenRouter Connection", color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Appearance", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text("Theme: Dark (Fixed)", color = TextSecondary)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("About", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text("Version 1.0.0", color = TextSecondary)
            Text("This product uses the TMDb API but is not endorsed or certified by TMDb.", color = TextSecondary)
        }
    }
}
