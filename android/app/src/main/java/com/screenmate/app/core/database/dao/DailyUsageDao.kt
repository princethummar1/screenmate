package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage WHERE usageDate = :date")
    fun getByDate(date: String): Flow<DailyUsageEntity?>

    @Query("SELECT * FROM daily_usage WHERE usageDate = :date")
    suspend fun getByDateDirect(date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE usageDate BETWEEN :startDate AND :endDate ORDER BY usageDate ASC")
    fun getRange(startDate: String, endDate: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE syncedToCloud = 0")
    fun getUnsynced(): List<DailyUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyUsage: DailyUsageEntity): Long

    @Update
    suspend fun update(dailyUsage: DailyUsageEntity)

    @Query("UPDATE daily_usage SET syncedToCloud = 1 WHERE usageDate = :date")
    suspend fun markSyncedToCloud(date: String)

    @Query("UPDATE daily_usage SET syncedToScreenMate = 1 WHERE usageDate = :date")
    suspend fun markSyncedToScreenMate(date: String)
}
