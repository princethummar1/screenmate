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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.CustomListEntity
import com.screenmate.app.core.database.entity.CustomListItemEntity
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class CustomListDetailViewModel(application: Application, private val listId: String) : AndroidViewModel(application) {
    private val db = (application as ScreenMateApplication).database
    private val customListDao = db.customListDao()
    private val customListItemDao = db.customListItemDao()

    val list: StateFlow<CustomListEntity?> = customListDao.getById(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<CustomListItemEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            customListItemDao.getByListId(listId)
        } else {
            customListItemDao.search(listId, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addItem(title: String, subtitle: String?, url: String?, notes: String?) {
        viewModelScope.launch {
            val item = CustomListItemEntity(
                id = UUID.randomUUID().toString(),
                listId = listId,
                title = title,
                subtitle = subtitle?.takeIf { it.isNotBlank() },
                url = url?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            customListItemDao.insert(item)
        }
    }

    fun updateItem(entity: CustomListItemEntity, title: String, subtitle: String?, url: String?, notes: String?) {
        viewModelScope.launch {
            customListItemDao.update(
                entity.copy(
                    title = title,
                    subtitle = subtitle?.takeIf { it.isNotBlank() },
                    url = url?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFavorite(item: CustomListItemEntity) {
        viewModelScope.launch {
            customListItemDao.update(item.copy(isFavorite = !item.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteItem(item: CustomListItemEntity) {
        viewModelScope.launch {
            customListItemDao.delete(item)
        }
    }
}

class CustomListDetailViewModelFactory(
    private val application: Application,
    private val listId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomListDetailViewModel(application, listId) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomListDetailScreen(
    listId: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: CustomListDetailViewModel = viewModel(
        factory = CustomListDetailViewModelFactory(application, listId)
    )

    val list by viewModel.list.collectAsState()
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<CustomListItemEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomListItemEntity?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DarkBackground)) {
                TopAppBar(
                    title = { Text(list?.name ?: "List Details", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search items...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = DarkBackground)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isNotBlank()) "No items match your search." else "No items yet.\nTap + to add one.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { editItem = item },
                                onLongClick = { deleteTarget = item }
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!item.subtitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.subtitle!!,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!item.status.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(item.status!!, color = TextPrimary) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant)
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Favorite",
                                    tint = if (item.isFavorite) AccentPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editItem != null) {
        var title by remember(editItem) { mutableStateOf(editItem?.title ?: "") }
        var subtitle by remember(editItem) { mutableStateOf(editItem?.subtitle ?: "") }
        var url by remember(editItem) { mutableStateOf(editItem?.url ?: "") }
        var notes by remember(editItem) { mutableStateOf(editItem?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editItem = null 
            },
            title = { Text(if (editItem != null) "Edit Item" else "New Item", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subtitle, onValueChange = { subtitle = it },
                        label = { Text("Subtitle (Optional)") }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            if (editItem != null) {
                                viewModel.updateItem(editItem!!, title, subtitle, url, notes)
                            } else {
                                viewModel.addItem(title, subtitle, url, notes)
                            }
                            showAddDialog = false
                            editItem = null
                        }
                    }
                ) {
                    Text("Save", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    editItem = null 
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Item?", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${deleteTarget!!.title}'?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(deleteTarget!!)
                        deleteTarget = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
