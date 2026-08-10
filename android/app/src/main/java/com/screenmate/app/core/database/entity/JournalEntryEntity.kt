package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["journalDate"], unique = true)]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val journalDate: String,
    val title: String?,
    val content: String = "",
    val mood: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
