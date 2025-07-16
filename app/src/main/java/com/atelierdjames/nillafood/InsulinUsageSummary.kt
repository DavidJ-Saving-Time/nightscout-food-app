package com.atelierdjames.nillafood

/**
 * Summary statistics for a single day showing insulin usage and macros
 * consumed with meals.
 */

data class InsulinUsageSummary(
    /** Formatted date string (e.g. "Apr 01"). */
    val day: String,
    /** Total units of rapid acting insulin. */
    val novorapid: Float,
    /** Total units of long acting insulin. */
    val tresiba: Float,
    /** Total carbohydrates consumed in grams. */
    val carbs: Float,
    /** Total protein consumed in grams. */
    val protein: Float,
    /** Total fat consumed in grams. */
    val fat: Float,
)
