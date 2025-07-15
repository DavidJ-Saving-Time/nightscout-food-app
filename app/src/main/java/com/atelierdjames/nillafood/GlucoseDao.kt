package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_entries")
    suspend fun getAll(): List<GlucoseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<GlucoseEntry>)

    @Query("SELECT date FROM glucose_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    @Query("SELECT date FROM glucose_entries ORDER BY date ASC LIMIT 1")
    suspend fun getEarliestTimestamp(): Long?

    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAll()
}
