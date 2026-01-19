// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeClockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimeClockEntry): Long

    @Query("SELECT * FROM time_clock_entries WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLastEntryForUser(userId: Int): Flow<TimeClockEntry?>

    @Query("SELECT * FROM time_clock_entries WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllEntriesForUser(userId: Int): Flow<List<TimeClockEntry>>

    @Query("SELECT * FROM time_clock_entries WHERE id NOT IN (SELECT id FROM time_clock_entries WHERE staff_supabase_id IS NOT NULL)")
    suspend fun getUnsyncedEntries(): List<TimeClockEntry>

    @Update
    suspend fun updateEntry(entry: TimeClockEntry)
}
