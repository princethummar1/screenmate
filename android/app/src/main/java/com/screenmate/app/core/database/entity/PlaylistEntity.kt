package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
