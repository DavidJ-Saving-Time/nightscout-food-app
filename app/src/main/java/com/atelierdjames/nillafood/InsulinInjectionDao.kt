package com.atelierdjames.nillafood

/**
 * DAO for storing insulin injection entries. These records are synced to and
 * from the Nightscout backend.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InsulinInjectionDao {
    /** Returns all injections ordered with most recent first. */
    @Query("SELECT * FROM insulin_injections ORDER BY time DESC")
    suspend fun getAll(): List<InsulinInjectionEntity>

    /** Injections that occurred on or after the provided timestamp. */
    @Query("SELECT * FROM insulin_injections WHERE time >= :start")
    suspend fun getSince(start: Long): List<InsulinInjectionEntity>

    /** Insert or replace a batch of injections. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<InsulinInjectionEntity>)

    /** Timestamp of the most recent injection or `null` if none exist. */
    @Query("SELECT time FROM insulin_injections ORDER BY time DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    /** Delete all stored insulin injections. */
    @Query("DELETE FROM insulin_injections")
    suspend fun deleteAll()
}
