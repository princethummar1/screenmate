package com.screenmate.app.features.reading

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.screenmate.app.BuildConfig
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.ReadingItemEntity
import com.screenmate.app.core.network.BookResult
import com.screenmate.app.core.network.BooksApi
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val readingItemDao = db.readingItemDao()
    private val booksApi = BooksApi()
    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
    private val userId = app.preferences.userId

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<BookResult>>(emptyList())
    val results: StateFlow<List<BookResult>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            try {
                _results.value = booksApi.searchBooks(q, apiKey)
            } catch (e: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToReadingList(book: BookResult, status: String) {
        viewModelScope.launch {
            val entity = ReadingItemEntity(
                id = UUID.randomUUID().toString(),
                title = book.title,
                author = book.authors?.firstOrNull(),
                url = book.isbn?.let { "https://books.google.com/books?id=${book.id}" },
                type = "book",
                status = status,
                rating = null,
                notes = book.description,
                startedAt = null,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ).copy(ownerUserId = userId)
            readingItemDao.insert(entity)
            _message.value = "\"${book.title}\" added to reading list!"
        }
    }

    fun addManual(title: String, author: String, status: String) {
        viewModelScope.launch {
            val entity = ReadingItemEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                author = author.takeIf { it.isNotBlank() },
                url = null,
                type = "book",
                status = status,
                rating = null,
                notes = null,
                startedAt = null,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ).copy(ownerUserId = userId)
            readingItemDao.insert(entity)
            _message.value = "\"$title\" added to reading list!"
        }
    }

    fun clearMessage() { _message.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BookSearchViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState(initial = "")
    val results by viewModel.results.collectAsState(initial = emptyList<BookResult>())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val message by viewModel.message.collectAsState(initial = null)
    var showManualDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Books", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true },
                containerColor = DarkSurfaceVariant
            ) {
                Text("Manual", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Search books...", color = TextSecondary) },
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

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentPrimary,
                    trackColor = DarkSurface
                )
            }

            message?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = AccentPrimary,
                    contentColor = DarkBackground
                ) { Text(it) }
                LaunchedEffect(it) {
                    delay(2000)
                    viewModel.clearMessage()
                }
            }

            if (results.isEmpty() && !isLoading && query.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No books found.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { book ->
                        BookResultCard(
                            book = book,
                            onAddToReadingList = { status -> viewModel.addToReadingList(book, status) }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualBookDialog(
            onDismiss = { showManualDialog = false },
            onSave = { title, author, status ->
                viewModel.addManual(title, author, status)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun BookResultCard(
    book: BookResult,
    onAddToReadingList: (String) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = book.thumbnail,
                contentDescription = book.title,
                modifier = Modifier
                    .width(50.dp)
                    .height(75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                book.authors?.let {
                    Text(it.joinToString(", "), color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                book.publishedDate?.let {
                    Text(it.take(4), color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                book.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Box {
                Button(
                    onClick = { showStatusMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Add", color = DarkBackground, style = MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                    DropdownMenuItem(text = { Text("To Read") }, onClick = {
                        onAddToReadingList("to_read"); showStatusMenu = false
                    })
                    DropdownMenuItem(text = { Text("Reading") }, onClick = {
                        onAddToReadingList("reading"); showStatusMenu = false
                    })
                    DropdownMenuItem(text = { Text("Completed") }, onClick = {
                        onAddToReadingList("completed"); showStatusMenu = false
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualBookDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, status: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("to_read") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Book Manually", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { status = "to_read" },
                        label = { Text("To Read", color = if (status == "to_read") DarkBackground else TextPrimary, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (status == "to_read") AccentPrimary else DarkSurface)
                    )
                    SuggestionChip(
                        onClick = { status = "reading" },
                        label = { Text("Reading", color = if (status == "reading") DarkBackground else TextPrimary, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (status == "reading") AccentPrimary else DarkSurface)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onSave(title.trim(), author.trim(), status) },
                enabled = title.isNotBlank()
            ) { Text("Save", color = AccentPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}
