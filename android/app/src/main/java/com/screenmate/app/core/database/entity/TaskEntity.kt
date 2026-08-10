package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["completed"]), Index(value = ["dueAt"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val dueAt: Long?,
    val priority: Int = 0,
    val completed: Boolean = false,
    val category: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
