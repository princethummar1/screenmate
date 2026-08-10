package com.screenmate.app.features.scratchpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

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
        Column(modifier = Modifier.padding(padding)) {}
    }
}
