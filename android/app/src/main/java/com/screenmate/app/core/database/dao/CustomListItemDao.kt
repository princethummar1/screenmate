package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.CustomListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListItemDao {
    @Query("SELECT * FROM custom_list_items WHERE listId = :listId ORDER BY position ASC")
    fun getByListId(listId: String): Flow<List<CustomListItemEntity>>

    @Query("SELECT * FROM custom_list_items WHERE listId = :listId AND (title LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%')")
    fun search(listId: String, query: String): Flow<List<CustomListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CustomListItemEntity)

    @Update
    suspend fun update(item: CustomListItemEntity)

    @Delete
    suspend fun delete(item: CustomListItemEntity)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId")
    suspend fun deleteAllByListId(listId: String)
}
