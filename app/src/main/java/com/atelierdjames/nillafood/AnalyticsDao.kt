package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Query(
        """
        WITH RECURSIVE times(ts) AS (
            SELECT :start
            UNION ALL
            SELECT ts + 300000 FROM times WHERE ts + 300000 <= strftime('%s','now')*1000
        )
        SELECT
            ts,
            (SELECT sgv FROM glucose_entries ge WHERE ge.date <= ts ORDER BY ge.date DESC LIMIT 1) AS glucose,
            (SELECT units FROM insulin_injections ii WHERE (strftime('%s', ii.time)*1000) <= ts ORDER BY strftime('%s', ii.time)*1000 DESC LIMIT 1) AS lastInsulinUnits,
            (SELECT carbs FROM treatments tr WHERE (strftime('%s', tr.timestamp)*1000) <= ts ORDER BY strftime('%s', tr.timestamp)*1000 DESC LIMIT 1) AS lastMealCarbs,
            (SELECT (ts - strftime('%s', tr.timestamp)*1000)/60000 FROM treatments tr WHERE (strftime('%s', tr.timestamp)*1000) <= ts ORDER BY strftime('%s', tr.timestamp)*1000 DESC LIMIT 1) AS minutesSinceMeal,
            (SELECT (ts - strftime('%s', ii.time)*1000)/60000 FROM insulin_injections ii WHERE (strftime('%s', ii.time)*1000) <= ts ORDER BY strftime('%s', ii.time)*1000 DESC LIMIT 1) AS minutesSinceInsulin
        FROM times
        """
    )
    fun streamFeatures(start: Long): Flow<List<TimepointFeatures>>
}
