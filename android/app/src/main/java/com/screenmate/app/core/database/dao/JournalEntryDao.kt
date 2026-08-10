package com.screenmate.app.core.database.dao

import androidx.room.*
import com.screenmate.app.core.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY journalDate DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE journalDate = :date")
    fun getByDate(date: String): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE isFavorite = 1 ORDER BY journalDate DESC")
    fun getFavorites(): Flow<List<JournalEntryEntity>>

    @Query("SELECT journalDate FROM journal_entries ORDER BY journalDate DESC")
    fun getAllDatesWithEntries(): Flow<List<String>>

    @Query("SELECT * FROM journal_entries WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE syncStatus != 0")
    fun getPending(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity)

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)
}
