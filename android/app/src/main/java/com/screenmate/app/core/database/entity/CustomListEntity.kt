package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val coverImageUrl: String? = null,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
