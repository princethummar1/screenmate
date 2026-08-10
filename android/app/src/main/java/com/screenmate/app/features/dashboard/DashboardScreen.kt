package com.screenmate.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.ui.theme.*
import com.screenmate.app.core.util.DateUtils
import com.screenmate.app.core.util.FallbackCommentary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel : ViewModel() {
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _todayTime = MutableStateFlow(0L)
    val todayTime = _todayTime.asStateFlow()

    private val _diffTime = MutableStateFlow(0L)
    val diffTime = _diffTime.asStateFlow()
    
    private val _unlocks = MutableStateFlow(0)
    val unlocks = _unlocks.asStateFlow()

    private val _aiCommentary = MutableStateFlow("")
    val aiCommentary = _aiCommentary.asStateFlow()

    private val _currentWatchingTitle = MutableStateFlow<String?>(null)
    val currentWatchingTitle = _currentWatchingTitle.asStateFlow()

    private val _currentReadingTitle = MutableStateFlow<String?>(null)
    val currentReadingTitle = _currentReadingTitle.asStateFlow()

    private val _hasJournalToday = MutableStateFlow(false)
    val hasJournalToday = _hasJournalToday.asStateFlow()

    fun loadData(context: android.content.Context) {
        viewModelScope.launch {
            val app = context.applicationContext as ScreenMateApplication
            val db = app.database
            val prefs = app.preferences

            _username.value = prefs.username ?: "User"

            val today = DateUtils.todayDate()
            val yesterday = DateUtils.yesterdayDate()

            val todayData = db.dailyUsageDao().getByDate(today).firstOrNull()
            val yesterdayData = db.dailyUsageDao().getByDate(yesterday).firstOrNull()

            _todayTime.value = todayData?.totalScreenTimeSeconds ?: 0L
            _unlocks.value = todayData?.unlockCount ?: 0
            
            val yTime = yesterdayData?.totalScreenTimeSeconds ?: 0L
            _diffTime.value = _todayTime.value - yTime

            if (_aiCommentary.value.isEmpty()) {
                _aiCommentary.value = FallbackCommentary.generateFallback((_todayTime.value / 60).toInt(), null, _unlocks.value)
            }

            // Load media
            val userId = prefs.userId
            val watchingList = db.mediaDao().getByStatus("watching", userId).firstOrNull()
            _currentWatchingTitle.value = watchingList?.firstOrNull()?.title

            // Load reading
            val readingList = db.readingItemDao().getByStatus("reading", userId).firstOrNull()
            _currentReadingTitle.value = readingList?.firstOrNull()?.title

            // Load journal
            val journalEntry = db.journalEntryDao().getByDate(today).firstOrNull()
            _hasJournalToday.value = journalEntry != null
        }
    }

    fun refreshCommentary() {
        _aiCommentary.value = FallbackCommentary.generateFallback((_todayTime.value / 60).toInt(), null, _unlocks.value)
    }
}

@Composable
fun DashboardScreen(
    onNavigateToWatchlist: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToDailyTime: () -> Unit,
    onNavigateToScratchpad: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    val username by viewModel.username.collectAsState()
    val todayTime by viewModel.todayTime.collectAsState()
    val diffTime by viewModel.diffTime.collectAsState()
    val unlocks by viewModel.unlocks.collectAsState()
    val commentary by viewModel.aiCommentary.collectAsState()
    val watchingTitle by viewModel.currentWatchingTitle.collectAsState()
    val readingTitle by viewModel.currentReadingTitle.collectAsState()
    val hasJournal by viewModel.hasJournalToday.collectAsState()

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "$greeting, $username", 
                color = TextPrimary, 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold
            )
            Text(DateUtils.todayDate(), color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToDailyTime() },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's Screen Time", color = TextSecondary)
                    Text(DateUtils.formatDuration(todayTime), color = AccentPrimary, style = MaterialTheme.typography.displaySmall)
                    
                    val sign = if (diffTime > 0) "↑" else "↓"
                    val diffText = DateUtils.formatDuration(kotlin.math.abs(diffTime))
                    Text("vs yesterday: $sign $diffText", color = if (diffTime > 0) MaterialTheme.colorScheme.error else AccentPrimary)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$unlocks Unlocks", color = TextSecondary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Commentary", color = AccentPrimary, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.refreshCommentary() }) { Text("Refresh") }
                    }
                    Text(commentary, color = TextPrimary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToWatchlist() }, 
                colors = CardDefaults.cardColors(containerColor = DarkSurface), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Continue Watching", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(watchingTitle ?: "Nothing watching", color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = DarkSurface), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Reading", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(readingTitle ?: "Nothing reading", color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = DarkSurface), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Journal", color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (hasJournal) {
                            Text("Written today ✓", color = AccentPrimary, fontWeight = FontWeight.Medium)
                        } else {
                            Text("Not written today", color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                    Button(
                        onClick = onNavigateToJournal,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Write", color = DarkBackground)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = onNavigateToScratchpad, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Note", color = AccentPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onNavigateToBookmarks, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Bookmark", color = AccentPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onNavigateToWatchlist, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Watch", color = AccentPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
