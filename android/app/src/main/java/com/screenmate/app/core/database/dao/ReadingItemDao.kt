package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.ReadingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingItemDao {
    @Query("SELECT * FROM reading_items WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<ReadingItemEntity>>

    @Query("SELECT * FROM reading_items WHERE status = :status AND ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getByStatus(status: String, ownerUserId: String): Flow<List<ReadingItemEntity>>

    @Query("SELECT * FROM reading_items WHERE id = :id AND ownerUserId = :ownerUserId")
    fun getById(id: String, ownerUserId: String): Flow<ReadingItemEntity?>

    @Query("SELECT * FROM reading_items WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<ReadingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReadingItemEntity)

    @Update
    suspend fun update(item: ReadingItemEntity)

    @Delete
    suspend fun delete(item: ReadingItemEntity)
}
