package com.screenmate.app.features.playlists

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

class PlaylistsViewModel : ViewModel() {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Playlists") }) },
        floatingActionButton = { FloatingActionButton(onClick = { }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {}
    }
}
