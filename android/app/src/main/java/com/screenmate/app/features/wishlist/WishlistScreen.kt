package com.screenmate.app.features.wishlist

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.database.entity.WishlistItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class WishlistViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScreenMateApplication
    private val db = app.database
    private val wishlistItemDao = db.wishlistItemDao()

    private val userId = app.preferences.userId
    val items: StateFlow<List<WishlistItemEntity>> = wishlistItemDao.getAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(title: String, price: String, link: String) {
        viewModelScope.launch {
            val owner = app.preferences.userId
            wishlistItemDao.insert(WishlistItemEntity(
                id = UUID.randomUUID().toString(),
                name = title,
                imageUrl = null,
                expectedPrice = price.toDoubleOrNull(),
                currency = "INR",
                productUrl = link.ifBlank { null },
                store = null,
                categoryId = null,
                priority = 1,
                notes = null,
                purchased = false,
                purchasedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ).copy(ownerUserId = owner))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: WishlistViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wishlist") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        item.expectedPrice?.let { Text("Price: $it", style = MaterialTheme.typography.bodySmall) }
                        item.productUrl?.let { Text("Link: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var link by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Wishlist Item") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Link") })
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (title.isNotBlank()) {
                        viewModel.addItem(title, price, link)
                        showAddDialog = false 
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
