package com.atelierdjames.nillafood

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MealDao {
    @Query(
        """
        SELECT carbs, peak, peak - baseline AS delta FROM (
            SELECT
                tr.carbs AS carbs,
                (SELECT sgv FROM glucose_entries ge WHERE ge.date <= tr.timestamp ORDER BY ge.date DESC LIMIT 1) AS baseline,
                (SELECT MAX(ge2.sgv) FROM glucose_entries ge2 WHERE ge2.date > tr.timestamp AND ge2.date <= tr.timestamp + 7200000) AS peak
            FROM treatments tr
            ORDER BY tr.timestamp DESC
        )
        """
    )
    suspend fun getRecentMealImpacts(): List<MealImpact>
}
