package com.screenmate.app.features.playlists

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

class PlaylistDetailViewModel(val playlistId: String) : ViewModel() {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlistId: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Playlist Detail") }) },
        floatingActionButton = { FloatingActionButton(onClick = { }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {}
    }
}
