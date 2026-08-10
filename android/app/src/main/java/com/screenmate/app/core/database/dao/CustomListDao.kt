package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.CustomListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {
    @Query("SELECT * FROM custom_lists WHERE ownerUserId = :ownerUserId ORDER BY position ASC")
    fun getAll(ownerUserId: String): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_lists WHERE id = :id")
    fun getById(id: String): Flow<CustomListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: CustomListEntity)

    @Update
    suspend fun update(list: CustomListEntity)

    @Delete
    suspend fun delete(list: CustomListEntity)

    @Query("SELECT COUNT(*) FROM custom_list_items WHERE listId = :listId")
    suspend fun getItemCount(listId: String): Int
}
