package com.atelierdjames.nillafood

/**
 * Data Access Object for the `glucose_entries` table which stores raw glucose
 * readings retrieved from Nightscout.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GlucoseDao {
    /** Returns all stored glucose entries ordered as inserted. */
    @Query("SELECT * FROM glucose_entries")
    suspend fun getAll(): List<GlucoseEntry>

    /** Inserts new glucose entries, replacing any conflicting rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<GlucoseEntry>)

    /** Timestamp of the most recent entry or `null` if none exist. */
    @Query("SELECT date FROM glucose_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    /** Timestamp of the earliest entry or `null` if the table is empty. */
    @Query("SELECT date FROM glucose_entries ORDER BY date ASC LIMIT 1")
    suspend fun getEarliestTimestamp(): Long?

    /** Removes all glucose data from the table. */
    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAll()
}
