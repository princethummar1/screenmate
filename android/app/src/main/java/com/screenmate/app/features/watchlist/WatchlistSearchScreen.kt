package com.screenmate.app.features.watchlist

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.screenmate.app.core.database.entity.MediaEntity
import com.screenmate.app.core.network.TmdbApi
import com.screenmate.app.core.network.TmdbResult
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WatchlistSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val mediaDao = db.mediaDao()
    private val tmdbApi = TmdbApi()
    // Prefer the v4 read-access token if present, otherwise fall back to v3 api key
    private val apiKey = if (BuildConfig.TMDB_READ_ACCESS_TOKEN.isNotBlank()) BuildConfig.TMDB_READ_ACCESS_TOKEN else BuildConfig.TMDB_API_KEY

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<TmdbResult>>(emptyList())
    val results: StateFlow<List<TmdbResult>> = _results

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
                _results.value = tmdbApi.searchMulti(q, apiKey)
            } catch (e: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToWatchlist(result: TmdbResult, status: String) {
        viewModelScope.launch {
            val owner = app.preferences.userId
            val entity = MediaEntity(
                id = UUID.randomUUID().toString(),
                tmdbId = result.id,
                mediaType = result.media_type,
                title = result.displayTitle,
                posterPath = result.poster_path,
                releaseYear = result.releaseYear,
                overview = result.overview,
                genres = null,
                status = status,
                rating = null,
                notes = null,
                review = null,
                startedAt = null,
                finishedAt = null,
                isFavorite = false,
                isManual = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            mediaDao.insert(entity)
            _message.value = "\"${result.displayTitle}\" added to watchlist!"
        }
    }

    fun clearMessage() { _message.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistSearchScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WatchlistSearchViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState(initial = "")
    val results by viewModel.results.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val message by viewModel.message.collectAsState(initial = null)
    var showManualDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Add", color = TextPrimary) },
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
                placeholder = { Text("Search movies & TV shows...", color = TextSecondary) },
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
                ) {
                    Text(it)
                }
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
                    Text("No results found.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { "${it.id}_${it.media_type}" }) { result ->
                        SearchResultCard(
                            result = result,
                            onAddToWatchlist = { status -> viewModel.addToWatchlist(result, status) }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualMediaDialog(
            onDismiss = { showManualDialog = false },
            onSave = { title, type, year, notes, status ->
                viewModel.addToWatchlist(
                    TmdbResult(
                        id = 0,
                        media_type = type,
                        title = title,
                        name = null,
                        poster_path = null,
                        release_date = if (year.isNotBlank()) "$year-01-01" else null,
                        first_air_date = null,
                        overview = notes
                    ),
                    status
                )
                showManualDialog = false
            }
        )
    }
}

@Composable
fun SearchResultCard(
    result: TmdbResult,
    onAddToWatchlist: (String) -> Unit
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
                model = result.poster_path?.let { "https://image.tmdb.org/t/p/w200$it" },
                contentDescription = result.displayTitle,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.displayTitle,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                result.media_type.uppercase(),
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant)
                    )
                    result.releaseYear?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$it", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                result.overview?.let {
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
                    DropdownMenuItem(text = { Text("Want to Watch") }, onClick = {
                        onAddToWatchlist("want_to_watch"); showStatusMenu = false
                    })
                    DropdownMenuItem(text = { Text("Watching") }, onClick = {
                        onAddToWatchlist("watching"); showStatusMenu = false
                    })
                }
            }
        }
    }
}
