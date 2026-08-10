package com.screenmate.app.features.journal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel

class JournalEntryViewModel(val date: String) : ViewModel() {
    fun toggleFavorite() {}
    fun delete() {}
    fun getAdjacentDates() {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(date: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(date) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {}
    }
}
