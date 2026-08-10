package com.screenmate.app.features.bookmarks

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
import com.screenmate.app.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val bookmarkDao = db.bookmarkDao()

    private val userId = app.preferences.userId
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            bookmarkDao.insert(BookmarkEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                url = url,
                description = null,
                categoryId = null,
                tags = null,
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    val bookmarks by viewModel.bookmarks.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bookmarks") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(bookmarks) { bookmark ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(bookmark.title, style = MaterialTheme.typography.titleMedium)
                        Text(bookmark.url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (title.isNotBlank() && url.isNotBlank()) {
                        viewModel.addBookmark(title, url)
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
