package com.screenmate.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishlist_items",
    indices = [Index(value = ["categoryId"]), Index(value = ["purchased"])]
)
data class WishlistItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val expectedPrice: Double?,
    val currency: String = "INR",
    val productUrl: String?,
    val store: String?,
    val categoryId: String?,
    val priority: Int = 1,
    val notes: String?,
    val purchased: Boolean = false,
    val purchasedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
    val ownerUserId: String = ""
)
