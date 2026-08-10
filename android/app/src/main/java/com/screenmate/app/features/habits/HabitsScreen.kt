package com.screenmate.app.features.habits

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.HabitEntity
import com.screenmate.app.core.database.entity.HabitEntryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as ScreenMateApplication).database
    private val habitDao = db.habitDao()
    private val habitEntryDao = db.habitEntryDao()
    private val owner = (application as ScreenMateApplication).preferences.userId

    val habits: StateFlow<List<HabitEntity>> = habitDao.getAll(owner)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(name: String, icon: String?, frequency: String) {
        viewModelScope.launch {
            habitDao.insert(HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                frequency = frequency,
                active = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                ownerUserId = owner
            ))
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch {
            habitEntryDao.insert(HabitEntryEntity(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                entryDate = com.screenmate.app.core.util.DateUtils.todayDate(),
                completed = true,
                note = null,
                syncStatus = 0
            ).copy(ownerUserId = owner))
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { habitDao.delete(habit) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(viewModel: HabitsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    val habits by viewModel.habits.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Habits") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(habits) { habit ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    onClick = { viewModel.toggleHabitToday(habit) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium)
                        Text("Frequency: ${habit.frequency}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Habit") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (name.isNotBlank()) {
                        viewModel.addHabit(name, null, "daily")
                        showAddDialog = false 
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
