package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.HabitEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitEntryDao {
    @Query("SELECT * FROM habit_entries WHERE entryDate = :date AND ownerUserId = :ownerUserId")
    fun getByDate(date: String, ownerUserId: String): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND ownerUserId = :ownerUserId ORDER BY entryDate DESC")
    fun getByHabitId(habitId: String, ownerUserId: String): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND entryDate = :date AND ownerUserId = :ownerUserId")
    fun getByHabitAndDate(habitId: String, date: String, ownerUserId: String): Flow<HabitEntryEntity?>

    @Query("SELECT COUNT(*) FROM habit_entries WHERE habitId = :habitId AND entryDate IN (:dates) AND completed = 1 AND ownerUserId = :ownerUserId")
    fun getStreakCount(habitId: String, dates: List<String>, ownerUserId: String): Int

    @Query("SELECT * FROM habit_entries WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<HabitEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HabitEntryEntity)

    @Update
    suspend fun update(entry: HabitEntryEntity)
}
