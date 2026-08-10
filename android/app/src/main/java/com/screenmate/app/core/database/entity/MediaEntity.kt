package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    indices = [Index(value = ["status"]), Index(value = ["isFavorite"])]
)
data class MediaEntity(
    @PrimaryKey val id: String,
    val tmdbId: Int?,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val releaseYear: Int?,
    val overview: String?,
    val genres: String?,
    val status: String = "want_to_watch",
    val priority: Int = 0,
    val rating: Float?,
    val notes: String?,
    val review: String?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val isFavorite: Boolean = false,
    val isManual: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
    ,
    val ownerUserId: String = ""
)
{
    // ownerUserId will be added with DB migration if required
}
