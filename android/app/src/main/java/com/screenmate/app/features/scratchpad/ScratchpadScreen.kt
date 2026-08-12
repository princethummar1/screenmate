package com.screenmate.app.features.scratchpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.compose.ui.unit.dp

class ScratchpadViewModel : ViewModel() {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchpadScreen(
    viewModel: ScratchpadViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToNewNote: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Scratchpad") }) },
        floatingActionButton = { FloatingActionButton(onClick = { onNavigateToNewNote() }) { Text("+") } }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your notes will appear here.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
