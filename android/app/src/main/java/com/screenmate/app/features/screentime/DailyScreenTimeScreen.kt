package com.screenmate.app.features.screentime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.screenmate.app.core.database.entity.AppUsageEntity
import com.screenmate.app.core.database.entity.DailyUsageEntity
import com.screenmate.app.core.ui.theme.*
import com.screenmate.app.core.util.DateUtils
import com.screenmate.app.usage.UsageCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailyScreenTimeViewModel : ViewModel() {
    private val _dailyUsage = MutableStateFlow<DailyUsageEntity?>(null)
    val dailyUsage = _dailyUsage.asStateFlow()

    private val _appUsage = MutableStateFlow<List<AppUsageEntity>>(emptyList())
    val appUsage = _appUsage.asStateFlow()

    private val _yesterdayUsage = MutableStateFlow<DailyUsageEntity?>(null)
    val yesterdayUsage = _yesterdayUsage.asStateFlow()
    
    private val _hasAccess = MutableStateFlow(true)
    val hasAccess = _hasAccess.asStateFlow()

    fun loadData(context: android.content.Context) {
        _hasAccess.value = UsageCollector.hasUsageAccess(context)
        if (!_hasAccess.value) return

        viewModelScope.launch {
            val db = (context.applicationContext as ScreenMateApplication).database
            val today = DateUtils.todayDate()
            val yesterday = DateUtils.yesterdayDate()

            val todayData = db.dailyUsageDao().getByDateDirect(today)
            _dailyUsage.value = todayData
            
            if (todayData != null) {
                db.appUsageDao().getByDailyId(todayData.id).collect { apps ->
                    _appUsage.value = apps.sortedByDescending { it.usageSeconds }.take(10)
                }
            }
            
            _yesterdayUsage.value = db.dailyUsageDao().getByDateDirect(yesterday)
        }
    }
}

@Composable
fun DailyScreenTimeScreen(
    viewModel: DailyScreenTimeViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    val dailyUsage by viewModel.dailyUsage.collectAsState()
    val appUsage by viewModel.appUsage.collectAsState()
    val yesterdayUsage by viewModel.yesterdayUsage.collectAsState()
    val hasAccess by viewModel.hasAccess.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        if (!hasAccess) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Usage Access Required", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please grant access to see detailed screen time.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) 
                    }, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                        Text("Open Settings", color = DarkBackground)
                    }
                }
            }
            return
        }

        Text("Today", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val totalTime = dailyUsage?.totalScreenTimeSeconds ?: 0L
        Text(DateUtils.formatDuration(totalTime), color = AccentPrimary, style = MaterialTheme.typography.displayMedium)
        
        val yesterdayTime = yesterdayUsage?.totalScreenTimeSeconds ?: 0L
        if (yesterdayTime > 0) {
            val diff = totalTime - yesterdayTime
            val sign = if (diff > 0) "↑" else "↓"
            Text("Yesterday ${DateUtils.formatDuration(yesterdayTime)} $sign ${DateUtils.formatDuration(kotlin.math.abs(diff))}", color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Device Stats", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text("Unlocks: ${dailyUsage?.unlockCount ?: 0}", color = TextSecondary)
        dailyUsage?.firstUsageAt?.let { Text("First Usage: $it", color = TextSecondary) }
        dailyUsage?.lastUsageAt?.let { Text("Last Usage: $it", color = TextSecondary) }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Top Apps", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(appUsage) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appLabel, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("Opened ${app.openCount} times", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(DateUtils.formatDuration(app.usageSeconds), color = AccentPrimary)
                }
            }
        }
    }
}
