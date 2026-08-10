package com.screenmate.app.features.screentime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

class WeeklyReportViewModel : ViewModel() {
    private val _weeklyData = MutableStateFlow<List<DailyUsageEntity>>(emptyList())
    val weeklyData = _weeklyData.asStateFlow()

    fun loadData(context: android.content.Context) {
        viewModelScope.launch {
            val db = (context.applicationContext as ScreenMateApplication).database
            val start = DateUtils.daysAgo(6)
            val end = DateUtils.todayDate()
            val data = db.dailyUsageDao().getRange(start, end).first()
            _weeklyData.value = data
        }
    }
}

@Composable
fun WeeklyReportScreen(
    viewModel: WeeklyReportViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    val weeklyData by viewModel.weeklyData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Last 7 Days",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (weeklyData.isEmpty()) {
            Text("Not enough data to display chart.", color = TextSecondary)
            return
        }

        val maxSeconds = weeklyData.maxOfOrNull { it.totalScreenTimeSeconds }?.coerceAtLeast(1L) ?: 1L
        val totalSeconds = weeklyData.sumOf { it.totalScreenTimeSeconds }
        val averageSeconds = if (weeklyData.isNotEmpty()) totalSeconds / weeklyData.size else 0L

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val barCount = 7
                val spacing = 16.dp.toPx()
                val barWidth = (size.width - (spacing * (barCount - 1))) / barCount

                for (i in 0 until barCount) {
                    val date = DateUtils.daysAgo(6 - i)
                    val data = weeklyData.find { it.usageDate == date }
                    val seconds = data?.totalScreenTimeSeconds ?: 0L
                    
                    val heightRatio = seconds.toFloat() / maxSeconds.toFloat()
                    val barHeight = size.height * heightRatio
                    
                    val x = i * (barWidth + spacing)
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = AccentPrimary,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Total Screen Time: ${DateUtils.formatDuration(totalSeconds)}", color = TextPrimary)
        Text("Daily Average: ${DateUtils.formatDuration(averageSeconds)}", color = TextPrimary)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val maxDay = weeklyData.maxByOrNull { it.totalScreenTimeSeconds }
        val minDay = weeklyData.minByOrNull { it.totalScreenTimeSeconds }
        
        if (maxDay != null) {
            Text("Highest Usage Day: ${maxDay.usageDate} (${DateUtils.formatDuration(maxDay.totalScreenTimeSeconds)})", color = TextSecondary)
        }
        if (minDay != null) {
            Text("Lowest Usage Day: ${minDay.usageDate} (${DateUtils.formatDuration(minDay.totalScreenTimeSeconds)})", color = TextSecondary)
        }
    }
}
