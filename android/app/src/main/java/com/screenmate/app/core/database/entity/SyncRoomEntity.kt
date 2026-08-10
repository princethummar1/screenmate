package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_rooms")
data class SyncRoomEntity(
    @PrimaryKey val roomId: String,
    val roomName: String,
    val goalMinutes: Int,
    val selected: Boolean = true
)
