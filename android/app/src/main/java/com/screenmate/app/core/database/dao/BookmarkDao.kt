package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark_items WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark_items WHERE categoryId = :categoryId AND ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getByCategory(categoryId: String, ownerUserId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark_items WHERE isFavorite = 1 AND ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getFavorites(ownerUserId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark_items WHERE id = :id")
    fun getById(id: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmark_items WHERE ownerUserId = :ownerUserId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')")
    fun search(ownerUserId: String, query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark_items WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("UPDATE bookmark_items SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun moveToCategoryOrUncategorize(oldCategoryId: String, newCategoryId: String?)
}
