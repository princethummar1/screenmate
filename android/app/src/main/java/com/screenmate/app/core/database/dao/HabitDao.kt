package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE active = 1 AND ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getActive(ownerUserId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)
}
