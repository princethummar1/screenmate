package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.BookmarkCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkCategoryDao {
    @Query("SELECT * FROM bookmark_categories ORDER BY position ASC, createdAt DESC")
    fun getAll(): Flow<List<BookmarkCategoryEntity>>

    @Query("SELECT * FROM bookmark_categories WHERE id = :id")
    fun getById(id: String): Flow<BookmarkCategoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: BookmarkCategoryEntity)

    @Update
    suspend fun update(category: BookmarkCategoryEntity)

    @Delete
    suspend fun delete(category: BookmarkCategoryEntity)
}
