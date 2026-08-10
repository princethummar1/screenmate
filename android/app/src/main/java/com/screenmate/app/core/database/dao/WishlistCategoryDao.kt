package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.WishlistCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistCategoryDao {
    @Query("SELECT * FROM wishlist_categories ORDER BY position ASC, createdAt DESC")
    fun getAll(): Flow<List<WishlistCategoryEntity>>

    @Query("SELECT * FROM wishlist_categories WHERE id = :id")
    fun getById(id: String): Flow<WishlistCategoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: WishlistCategoryEntity)

    @Update
    suspend fun update(category: WishlistCategoryEntity)

    @Delete
    suspend fun delete(category: WishlistCategoryEntity)
}
