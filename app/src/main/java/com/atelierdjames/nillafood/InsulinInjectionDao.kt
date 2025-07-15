package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InsulinInjectionDao {
    @Query("SELECT * FROM insulin_injections ORDER BY time DESC")
    suspend fun getAll(): List<InsulinInjectionEntity>

    @Query("SELECT * FROM insulin_injections WHERE time >= :start")
    suspend fun getSince(start: Long): List<InsulinInjectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<InsulinInjectionEntity>)

    @Query("SELECT time FROM insulin_injections ORDER BY time DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    @Query("DELETE FROM insulin_injections")
    suspend fun deleteAll()
}
