package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_list_items",
    indices = [Index(value = ["listId"])]
)
data class CustomListItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val rating: Float? = null,
    val tags: String? = null,
    val metadata: String? = null,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
