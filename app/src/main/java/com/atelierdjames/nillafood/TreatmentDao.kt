package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TreatmentDao {
    @Query("SELECT * FROM treatments ORDER BY timestamp DESC")
    suspend fun getAll(): List<TreatmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TreatmentEntity>)

    @Query("SELECT timestamp FROM treatments ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTimestamp(): String?
}
