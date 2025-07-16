package com.atelierdjames.nillafood

/**
 * DAO used for accessing meal treatment records. These correspond to the
 * "Meal Entry" events stored in Nightscout.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TreatmentDao {
    /** Retrieve all treatments ordered by most recent first. */
    @Query("SELECT * FROM treatments ORDER BY timestamp DESC")
    suspend fun getAll(): List<TreatmentEntity>

    /** Insert or replace a list of treatments. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TreatmentEntity>)

    /** Remove a treatment by its id. */
    @Query("DELETE FROM treatments WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Delete all stored treatments. */
    @Query("DELETE FROM treatments")
    suspend fun deleteAll()

    /** Timestamp of the newest treatment in storage. */
    @Query("SELECT timestamp FROM treatments ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    /** All treatments occurring at or after [start]. */
    @Query("SELECT * FROM treatments WHERE timestamp >= :start")
    suspend fun getSince(start: Long): List<TreatmentEntity>
}
