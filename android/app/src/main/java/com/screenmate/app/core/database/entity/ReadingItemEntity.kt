package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_items")
data class ReadingItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val url: String?,
    val type: String = "book",
    val status: String = "to_read",
    val rating: Float?,
    val notes: String?,
    val startedAt: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
