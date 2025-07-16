package com.atelierdjames.nillafood

/**
 * Collection of statistics derived from stored glucose readings. Values are
 * pre-calculated so the UI can display them without performing heavy
 * computations on the main thread.
 */

/** Percentage of readings that fall within or outside the target range. */
data class TimeInRange(
    val inRange: Float,
    val above: Float,
    val below: Float,
)

/** Aggregated glucose metrics used throughout the application. */
data class GlucoseStats(
    val avg24h: Float,
    val avg7d: Float,
    val avg14d: Float,
    val tir24h: TimeInRange,
    val tir7d: TimeInRange,
    val tir14d: TimeInRange,
    val hba1c: Float,
    val sd: Float,
    val daysUsed: Int,
)
