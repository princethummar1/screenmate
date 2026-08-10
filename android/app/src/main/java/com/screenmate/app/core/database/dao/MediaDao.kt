package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE status = :status AND ownerUserId = :ownerUserId ORDER BY priority DESC, createdAt DESC")
    fun getByStatus(status: String, ownerUserId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE status = 'completed' AND ownerUserId = :ownerUserId ORDER BY finishedAt DESC")
    fun getWatchLog(ownerUserId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE id = :id")
    fun getById(id: String): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE isFavorite = 1 AND ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getFavorites(ownerUserId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE ownerUserId = :ownerUserId AND title LIKE '%' || :query || '%'")
    fun search(ownerUserId: String, query: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)
}
