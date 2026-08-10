package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmark_items",
    indices = [Index(value = ["categoryId"]), Index(value = ["isFavorite"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val description: String?,
    val categoryId: String?,
    val tags: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
