package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_entries",
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["entryDate"]),
        Index(value = ["habitId", "entryDate"], unique = true)
    ]
)
data class HabitEntryEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val entryDate: String,
    val completed: Boolean = false,
    val note: String?,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
