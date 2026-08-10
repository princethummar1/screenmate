package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.AppUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE dailyUsageId = :dailyUsageId")
    fun getByDailyId(dailyUsageId: Long): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppUsageEntity>)

    @Query("DELETE FROM app_usage WHERE dailyUsageId = :dailyUsageId")
    suspend fun deleteByDailyId(dailyUsageId: Long)
}
