package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistItemDao {
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getByPlaylistId(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE id = :id")
    fun getById(id: String): Flow<PlaylistItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlaylistItemEntity)

    @Update
    suspend fun update(item: PlaylistItemEntity)

    @Delete
    suspend fun delete(item: PlaylistItemEntity)

    @Update
    suspend fun updatePositions(items: List<PlaylistItemEntity>)
}
