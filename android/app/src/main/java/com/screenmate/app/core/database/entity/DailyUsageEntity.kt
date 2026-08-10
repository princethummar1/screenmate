package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["usageDate"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usageDate: String,
    val totalScreenTimeSeconds: Long,
    val unlockCount: Int,
    val appOpenCount: Int,
    val firstUsageAt: String?,
    val lastUsageAt: String?,
    val timezone: String,
    val syncedToCloud: Boolean = false,
    val syncedToScreenMate: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
    ,
    val ownerUserId: String = ""
)
