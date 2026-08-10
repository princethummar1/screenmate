package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["isPinned"])]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val content: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
{
    // ownerUserId reserved for future per-user partitioning
}
