package com.screenmate.app.features.customlists

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.CustomListEntity
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CustomListWithCount(val list: CustomListEntity, val count: Int)

class CustomListsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val customListDao = db.customListDao()

    private val userId = app.preferences.userId
    val lists: StateFlow<List<CustomListWithCount>> = customListDao.getAll(userId)
        .map { items ->
            items.map { list ->
                val count = customListDao.getItemCount(list.id)
                CustomListWithCount(list, count)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addList(name: String, description: String?) {
        viewModelScope.launch {
            customListDao.insert(
                CustomListEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    ownerUserId = app.preferences.userId
                )
            )
        }
    }

    fun updateList(entity: CustomListEntity, name: String, description: String?) {
        viewModelScope.launch {
            customListDao.update(
                entity.copy(
                    name = name,
                    description = description?.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteList(list: CustomListEntity) {
        viewModelScope.launch {
            db.customListItemDao().deleteAllByListId(list.id)
            customListDao.delete(list)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomListsScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: CustomListsViewModel = viewModel()
) {
    val lists by viewModel.lists.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editList by remember { mutableStateOf<CustomListEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomListWithCount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Lists", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add List", tint = DarkBackground)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (lists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No custom lists yet.\nTap + to create one.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lists, key = { it.list.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onNavigateToDetail(item.list.id) },
                                onLongClick = { deleteTarget = item }
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.list.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (!item.list.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.list.description!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${item.count} items", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { editList = item.list }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit dialog
    if (showAddDialog || editList != null) {
        var name by remember(editList) { mutableStateOf(editList?.name ?: "") }
        var description by remember(editList) { mutableStateOf(editList?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; editList = null },
            title = { Text(if (editList != null) "Edit List" else "New List", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        if (editList != null) viewModel.updateList(editList!!, name, description)
                        else viewModel.addList(name, description)
                        showAddDialog = false; editList = null
                    }
                }) { Text("Save", color = AccentPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; editList = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Delete confirmation
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete List?", color = TextPrimary) },
            text = { Text("This list has ${deleteTarget!!.count} items. Delete?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(deleteTarget!!.list)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }
}
