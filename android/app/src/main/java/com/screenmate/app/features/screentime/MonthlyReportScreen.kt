package com.screenmate.app.features.screentime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.DailyUsageEntity
import com.screenmate.app.core.ui.theme.*
import com.screenmate.app.core.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MonthlyReportViewModel : ViewModel() {
    private val _monthlyData = MutableStateFlow<List<DailyUsageEntity>>(emptyList())
    val monthlyData = _monthlyData.asStateFlow()

    fun loadData(context: android.content.Context) {
        viewModelScope.launch {
            val db = (context.applicationContext as ScreenMateApplication).database
            val start = DateUtils.daysAgo(30)
            val end = DateUtils.todayDate()
            val data = db.dailyUsageDao().getRange(start, end).first()
            _monthlyData.value = data
        }
    }
}

@Composable
fun MonthlyReportScreen(
    viewModel: MonthlyReportViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    val monthlyData by viewModel.monthlyData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Since prompt asks for a generic month/year header, we will use Java Time if available or simple text
        val currentMonthYear = try {
            val date = LocalDate.now()
            "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"
        } catch (e: Exception) {
            "Last 30 Days"
        }

        Text(
            text = currentMonthYear,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (monthlyData.isEmpty()) {
            Text("Not enough data to display report.", color = TextSecondary)
            return
        }

        val totalSeconds = monthlyData.sumOf { it.totalScreenTimeSeconds }
        val averageSeconds = totalSeconds / monthlyData.size
        val totalUnlocks = monthlyData.sumOf { it.unlockCount }
        val avgUnlocks = totalUnlocks / monthlyData.size

        val maxDay = monthlyData.maxByOrNull { it.totalScreenTimeSeconds }
        val minDay = monthlyData.minByOrNull { it.totalScreenTimeSeconds }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Screen Time: ${DateUtils.formatDuration(totalSeconds)}", color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Daily Average: ${DateUtils.formatDuration(averageSeconds)}", color = AccentPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Average Unlocks/Day: $avgUnlocks", color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Highest Day:", color = TextSecondary)
                if (maxDay != null) {
                    Text("${maxDay.usageDate} - ${DateUtils.formatDuration(maxDay.totalScreenTimeSeconds)}", color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Best Day (Lowest):", color = TextSecondary)
                if (minDay != null) {
                    Text("${minDay.usageDate} - ${DateUtils.formatDuration(minDay.totalScreenTimeSeconds)}", color = TextPrimary)
                }
            }
        }
    }
}
