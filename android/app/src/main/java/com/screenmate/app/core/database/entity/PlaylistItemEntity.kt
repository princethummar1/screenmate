package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_items",
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val title: String,
    val creator: String?,
    val url: String?,
    val platform: String = "other",
    val notes: String?,
    val position: Int = 0,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
{
    // ownerUserId will be added in future migrations if needed
}
