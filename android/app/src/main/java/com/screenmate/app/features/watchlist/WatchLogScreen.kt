package com.screenmate.app.features.watchlist

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.MediaEntity
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

class WatchLogViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as ScreenMateApplication).database
    private val mediaDao = db.mediaDao()

    private val owner = (application as ScreenMateApplication).preferences.userId

    val completedMedia: StateFlow<List<MediaEntity>> = mediaDao.getWatchLog(owner)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchLogScreen(
    viewModel: WatchLogViewModel = viewModel()
) {
    val completedMedia by viewModel.completedMedia.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch Log", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (completedMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No completed media yet.\nFinish something from your watchlist!",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(completedMedia, key = { it.id }) { media ->
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
                                    .width(50.dp)
                                    .height(75.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (media.rating != null) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(14.dp))
                                        Text(" ${String.format("%.1f", media.rating)}", color = AccentPrimary, style = MaterialTheme.typography.labelMedium)
                                    }
                                    media.finishedAt?.let {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Finished ${dateFormat.format(Date(it))}",
                                            color = TextTertiary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                media.review?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                                if (media.isFavorite) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("★ Favorite", color = AccentPrimary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
