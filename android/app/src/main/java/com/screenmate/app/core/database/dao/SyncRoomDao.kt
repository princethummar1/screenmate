package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.SyncRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRoomDao {
    @Query("SELECT * FROM sync_rooms ORDER BY roomName ASC")
    fun getAll(): Flow<List<SyncRoomEntity>>

    @Query("SELECT * FROM sync_rooms WHERE selected = 1 ORDER BY roomName ASC")
    fun getSelected(): Flow<List<SyncRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rooms: List<SyncRoomEntity>)

    @Query("UPDATE sync_rooms SET selected = :selected WHERE roomId = :roomId")
    suspend fun updateSelection(roomId: String, selected: Boolean)

    @Query("DELETE FROM sync_rooms")
    suspend fun deleteAll()
}
