package com.atelierdjames.nillafood

import android.content.Context
import kotlinx.coroutines.flow.Flow

object AnalyticsRepository {
    private fun db(context: Context) = DatabaseProvider.db(context)

    fun streamFeatures(context: Context, start: Long): Flow<List<TimepointFeatures>> {
        return db(context).analyticsDao().streamFeatures(start)
    }
}
