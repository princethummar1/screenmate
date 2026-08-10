package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistItemDao {
    @Query("SELECT * FROM wishlist_items WHERE purchased = 0 AND ownerUserId = :ownerUserId ORDER BY priority DESC, createdAt DESC")
    fun getAll(ownerUserId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_items WHERE categoryId = :categoryId AND purchased = 0 AND ownerUserId = :ownerUserId ORDER BY priority DESC, createdAt DESC")
    fun getByCategory(categoryId: String, ownerUserId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_items WHERE id = :id")
    fun getById(id: String): Flow<WishlistItemEntity?>

    @Query("SELECT * FROM wishlist_items WHERE purchased = 1 AND ownerUserId = :ownerUserId ORDER BY purchasedAt DESC")
    fun getPurchased(ownerUserId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_items WHERE syncStatus != 0 AND ownerUserId = :ownerUserId")
    fun getPending(ownerUserId: String): List<WishlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistItemEntity)

    @Update
    suspend fun update(item: WishlistItemEntity)

    @Delete
    suspend fun delete(item: WishlistItemEntity)
}
