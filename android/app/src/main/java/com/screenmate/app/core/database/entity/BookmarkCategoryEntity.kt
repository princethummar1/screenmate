package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark_categories")
data class BookmarkCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    val createdAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
