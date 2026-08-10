package com.screenmate.app.features.tasks

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
import com.screenmate.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val taskDao = db.taskDao()

    private val userId = app.preferences.userId
    val tasks: StateFlow<List<TaskEntity>> = taskDao.getAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTask(title: String, description: String, dueAt: Long?, priority: String, category: String?) {
        viewModelScope.launch {
            val owner = app.preferences.userId
            taskDao.insert(TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                dueAt = dueAt ?: System.currentTimeMillis(),
                priority = if (priority == "high") 2 else if (priority == "medium") 1 else 0,
                completed = false,
                category = category,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            ).copy(ownerUserId = owner))
        }
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                completed = !task.completed,
                completedAt = if (!task.completed) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.update(updated)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.delete(task) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    val tasks by viewModel.tasks.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tasks") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    onClick = { viewModel.toggleComplete(task) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        if (!task.description.isNullOrEmpty()) {
                            Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Completed: ${task.completed}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Task") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (title.isNotBlank()) {
                        viewModel.addTask(title, desc, null, "medium", null)
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
