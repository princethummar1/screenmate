package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    foreignKeys = [
        ForeignKey(
            entity = DailyUsageEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyUsageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dailyUsageId"])]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyUsageId: Long,
    val packageName: String,
    val appLabel: String,
    val usageSeconds: Long,
    val openCount: Int
)
