package com.screenmate.app.features.reading

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.ReadingItemEntity
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import com.screenmate.app.core.network.BooksApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val readingItemDao = db.readingItemDao()
    private val booksApi = BooksApi()
    private val booksApiKey = com.screenmate.app.BuildConfig.GOOGLE_BOOKS_API_KEY

    private val userId = app.preferences.userId
    val items: StateFlow<List<ReadingItemEntity>> = readingItemDao.getAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(title: String, author: String?, url: String?, type: String, status: String, notes: String?) {
        viewModelScope.launch {
            val owner = app.preferences.userId
            readingItemDao.insert(ReadingItemEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                author = author,
                url = url,
                type = type,
                status = status,
                rating = null,
                notes = notes,
                startedAt = null,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ).copy(ownerUserId = owner))
        }
    }
    suspend fun searchBooks(query: String): List<com.screenmate.app.core.network.BookResult> {
        return booksApi.searchBooks(query, booksApiKey)
    }
    fun updateItem(item: ReadingItemEntity) { viewModelScope.launch { readingItemDao.update(item) } }
    fun deleteItem(item: ReadingItemEntity) { viewModelScope.launch { readingItemDao.delete(item) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    onNavigateToSearch: () -> Unit = {},
    viewModel: ReadingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading List", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = AccentPrimary
            ) {
                Text("+", color = DarkBackground)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No books yet.\nTap + to search and add.", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            item.author?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("") }
        var query by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<com.screenmate.app.core.network.BookResult>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }
        var searchJob: Job? by remember { mutableStateOf(null) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Reading Item") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchJob?.cancel()
                            if (it.isBlank()) {
                                searchResults = emptyList(); isSearching = false
                            } else {
                                searchJob = viewModel.viewModelScope.launch {
                                    isSearching = true
                                    delay(500)
                                    val results = viewModel.searchBooks(it)
                                    searchResults = results
                                    isSearching = false
                                }
                            }
                        },
                        label = { Text("Search Google Books") }
                    )
                    if (isSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (searchResults.isNotEmpty()) {
                        Text("Results", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.fillMaxHeight(0.4f)) {
                            searchResults.forEach { r ->
                                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(r.title, style = MaterialTheme.typography.bodyLarge)
                                            r.authors?.let { Text(it.joinToString(", "), style = MaterialTheme.typography.bodySmall) }
                                        }
                                        TextButton(onClick = {
                                            viewModel.addItem(r.title, r.authors?.firstOrNull(), null, "book", "to_read", r.description)
                                            showAddDialog = false
                                        }) { Text("Add") }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (title.isNotBlank()) {
                        viewModel.addItem(title, author, null, "book", "to_read", null)
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
