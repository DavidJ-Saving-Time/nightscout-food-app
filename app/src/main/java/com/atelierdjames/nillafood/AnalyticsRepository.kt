package com.atelierdjames.nillafood

import android.content.Context
import kotlinx.coroutines.flow.Flow

object AnalyticsRepository {
    private fun db(context: Context) = DatabaseProvider.db(context)

    fun streamFeatures(context: Context, start: Long): Flow<List<TimepointFeatures>> {
        return db(context).analyticsDao().streamFeatures(start)
    }

    fun streamIOB(context: Context, activityWindowMs: Long): Flow<List<IOBPoint>> {
        return db(context).insulinDao().streamIOB(activityWindowMs)
    }

    fun streamMealImpacts(context: Context): Flow<List<MealImpact>> =
        kotlinx.coroutines.flow.flow {
            emit(db(context).mealDao().getRecentMealImpacts())
        }
}
