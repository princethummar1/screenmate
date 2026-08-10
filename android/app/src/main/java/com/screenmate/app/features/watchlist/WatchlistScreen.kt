package com.screenmate.app.features.watchlist

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val mediaDao = db.mediaDao()

    private val _filter = MutableStateFlow("all")
    val filter: StateFlow<String> = _filter

    private val userId = app.preferences.userId
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val media: StateFlow<List<MediaEntity>> = _filter.flatMapLatest { f ->
        when (f) {
            "want_to_watch" -> mediaDao.getByStatus("want_to_watch", userId)
            "watching" -> mediaDao.getByStatus("watching", userId)
            "completed" -> mediaDao.getByStatus("completed", userId)
            else -> mediaDao.getAll(userId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: String) { _filter.value = filter }

    fun deleteMedia(entity: MediaEntity) {
        viewModelScope.launch { mediaDao.delete(entity) }
    }

    fun finishAndReview(id: String, rating: Float, review: String, finishedAt: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            mediaDao.getById(id).firstOrNull()?.let { entity ->
                mediaDao.update(
                    entity.copy(
                        status = "completed",
                        rating = rating,
                        review = review.takeIf { it.isNotBlank() },
                        finishedAt = finishedAt,
                        isFavorite = isFavorite,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateStatus(id: String, status: String) {
        viewModelScope.launch {
            mediaDao.getById(id).firstOrNull()?.let { entity ->
                mediaDao.update(entity.copy(status = status, updatedAt = System.currentTimeMillis()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onNavigateToSearch: () -> Unit = {},
    viewModel: WatchlistViewModel = viewModel()
) {
    val media by viewModel.media.collectAsState(initial = emptyList())
    val filter by viewModel.filter.collectAsState(initial = "all")
    var showFinishDialog by remember { mutableStateOf<MediaEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = AccentPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Search & Add", tint = DarkBackground)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("All", filter == "all") { viewModel.setFilter("all") }
                FilterChip("Want to Watch", filter == "want_to_watch") { viewModel.setFilter("want_to_watch") }
                FilterChip("Watching", filter == "watching") { viewModel.setFilter("watching") }
                FilterChip("Completed", filter == "completed") { viewModel.setFilter("completed") }
            }

            if (media.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No media found.\nTap + to search and add.",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(media, key = { it.id }) { item ->
                        MediaCard(
                            media = item,
                            onFinishAndReview = { showFinishDialog = item },
                            onStatusChange = { newStatus -> viewModel.updateStatus(item.id, newStatus) },
                            onDelete = { viewModel.deleteMedia(item) }
                        )
                    }
                }
            }
        }
    }

    showFinishDialog?.let { media ->
        FinishAndReviewDialog(
            media = media,
            onDismiss = { showFinishDialog = null },
            onConfirm = { rating, review, finishedAt, isFavorite ->
                viewModel.finishAndReview(media.id, rating, review, finishedAt, isFavorite)
                showFinishDialog = null
            }
        )
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, color = if (selected) DarkBackground else TextPrimary, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (selected) AccentPrimary else DarkSurface
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    media: MediaEntity,
    onFinishAndReview: () -> Unit,
    onStatusChange: (String) -> Unit
    , onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

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
                model = media.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" },
                contentDescription = media.title,
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
                    media.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                if (media.releaseYear != null) {
                    Text("${media.releaseYear}", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text(media.status.replace("_", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant)
                )
                if (media.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(14.dp))
                        Text(" ${media.rating}", color = AccentPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Text("...", color = TextSecondary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (media.status != "watching") {
                        DropdownMenuItem(text = { Text("Mark Watching") }, onClick = {
                            onStatusChange("watching"); showMenu = false
                        })
                    }
                    if (media.status != "want_to_watch") {
                        DropdownMenuItem(text = { Text("Mark Want to Watch") }, onClick = {
                            onStatusChange("want_to_watch"); showMenu = false
                        })
                    }
                    if (media.status == "watching") {
                        DropdownMenuItem(text = { Text("Finish & Review") }, onClick = {
                            onFinishAndReview(); showMenu = false
                        })
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        showMenu = false
                        onDelete()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishAndReviewDialog(
    media: MediaEntity,
    onDismiss: () -> Unit,
    onConfirm: (rating: Float, review: String, finishedAt: Long, isFavorite: Boolean) -> Unit
) {
    var rating by remember { mutableFloatStateOf(media.rating ?: 3f) }
    var review by remember { mutableStateOf(media.review ?: "") }
    var isFavorite by remember { mutableStateOf(media.isFavorite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish & Review", color = TextPrimary) },
        text = {
            Column {
                Text(media.title, color = AccentPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Rating", color = TextSecondary)
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..5f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
                Text("${String.format("%.1f", rating)} / 5.0", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Review (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder
                    ),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = AccentPrimary, uncheckedColor = TextSecondary, checkmarkColor = DarkBackground)
                    )
                    Text("Favorite", color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rating, review, System.currentTimeMillis(), isFavorite) }) {
                Text("Save", color = AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
