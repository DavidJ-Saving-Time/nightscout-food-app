package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinInjectionDao {
    @Query("SELECT * FROM insulin_injections ORDER BY time DESC")
    suspend fun getAll(): List<InsulinInjectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<InsulinInjectionEntity>)

    @Query("SELECT time FROM insulin_injections ORDER BY time DESC LIMIT 1")
    suspend fun getLatestTimestamp(): Long?

    @Query(
        """
        WITH RECURSIVE times(ts) AS (
            SELECT (SELECT MIN(time) FROM insulin_injections)
            UNION ALL
            SELECT ts + 300000 FROM times
            WHERE ts + 300000 <= (SELECT MAX(time) FROM insulin_injections) + :activityWindowMs
        )
        SELECT ts AS ts,
            COALESCE(SUM(
                CASE
                    WHEN ts >= ii.time AND ts <= ii.time + :activityWindowMs
                        THEN ii.units * (1 - CAST(ts - ii.time AS REAL) / :activityWindowMs)
                    ELSE 0
                END
            ), 0) AS iob
        FROM times
        LEFT JOIN insulin_injections ii ON ii.time <= ts AND ii.time + :activityWindowMs >= ts
        GROUP BY ts
        ORDER BY ts
        """
    )
    fun streamIOB(activityWindowMs: Long): Flow<List<IOBPoint>>
}
