package com.screenmate.app.features.scratchpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

class NoteEditViewModel(val noteId: String) : ViewModel() {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(noteId: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Note") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {}
    }
}
