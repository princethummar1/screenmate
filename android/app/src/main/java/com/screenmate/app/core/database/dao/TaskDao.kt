package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt BETWEEN :todayStart AND :todayEnd ORDER BY priority DESC, dueAt ASC")
    fun getToday(todayStart: Long, todayEnd: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY completedAt DESC")
    fun getCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt > :now AND completed = 0 ORDER BY dueAt ASC")
    fun getUpcoming(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
