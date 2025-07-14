package com.atelierdjames.nillafood

/**
 * Summary of a meal's effect on glucose levels.
 *
 * @property carbs carbohydrate grams of the meal
 * @property peak maximum glucose value within two hours after the meal
 * @property delta difference between the peak and glucose immediately before the meal
 */
data class MealImpact(
    val carbs: Float,
    val peak: Float?,
    val delta: Float?
)
