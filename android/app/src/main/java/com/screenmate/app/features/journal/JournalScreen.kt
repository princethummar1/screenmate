package com.screenmate.app.features.journal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.screenmate.app.core.database.entity.JournalEntryEntity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

class JournalViewModel : ViewModel() {
    private val _entries = MutableStateFlow<List<JournalEntryEntity>>(emptyList())
    val entries: StateFlow<List<JournalEntryEntity>> = _entries
    
    private val _datesWithEntries = MutableStateFlow<Set<String>>(emptySet())
    val datesWithEntries: StateFlow<Set<String>> = _datesWithEntries
    
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    fun previousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToToday: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Journal") }) },
        floatingActionButton = { FloatingActionButton(onClick = { onNavigateToToday() }) { Text("Write Today") } }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your Journal entries will appear here.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
