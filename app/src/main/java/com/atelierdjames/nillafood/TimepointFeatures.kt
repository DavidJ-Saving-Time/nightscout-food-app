package com.atelierdjames.nillafood

/**
 * Combined feature information for a single timestamp.
 *
 * @property ts epoch timestamp in milliseconds of the time step
 * @property glucose latest glucose value up to this timestamp
 * @property lastInsulinUnits units of the last insulin injection before this time
 * @property lastMealCarbs carbs of the last meal before this time
 * @property minutesSinceMeal minutes since the last meal
 * @property minutesSinceInsulin minutes since the last insulin injection
 */
data class TimepointFeatures(
    val ts: Long,
    val glucose: Float?,
    val lastInsulinUnits: Float?,
    val lastMealCarbs: Float?,
    val minutesSinceMeal: Long?,
    val minutesSinceInsulin: Long?
)
