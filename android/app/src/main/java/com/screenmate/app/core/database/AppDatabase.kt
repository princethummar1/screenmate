package com.screenmate.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.screenmate.app.core.database.dao.*
import com.screenmate.app.core.database.entity.*

@Database(
    entities = [
        DailyUsageEntity::class, AppUsageEntity::class, TaskEntity::class, HabitEntity::class, HabitEntryEntity::class,
        MediaEntity::class, ReadingItemEntity::class, PlaylistEntity::class, PlaylistItemEntity::class,
        WishlistCategoryEntity::class, WishlistItemEntity::class, NoteEntity::class, JournalEntryEntity::class,
        BookmarkCategoryEntity::class, BookmarkEntity::class, SyncRoomEntity::class,
        CustomListEntity::class, CustomListItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun mediaDao(): MediaDao
    abstract fun readingItemDao(): ReadingItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun wishlistCategoryDao(): WishlistCategoryDao
    abstract fun wishlistItemDao(): WishlistItemDao
    abstract fun noteDao(): NoteDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun bookmarkCategoryDao(): BookmarkCategoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun syncRoomDao(): SyncRoomDao
    abstract fun customListDao(): CustomListDao
    abstract fun customListItemDao(): CustomListItemDao
}
