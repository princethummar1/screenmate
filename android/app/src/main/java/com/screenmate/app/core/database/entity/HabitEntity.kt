package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,
    val frequency: String = "daily",
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
