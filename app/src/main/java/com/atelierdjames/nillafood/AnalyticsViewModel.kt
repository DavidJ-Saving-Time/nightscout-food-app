package com.atelierdjames.nillafood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AnalyticsRepository
    private val ctx = getApplication<Application>()

    val featureFlow: StateFlow<List<TimepointFeatures>> =
        repo.streamFeatures(ctx, 0L).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    val iobFlow: StateFlow<List<IOBPoint>> =
        repo.streamIOB(ctx, 4 * 60 * 60 * 1000L).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    val mealImpacts: StateFlow<List<MealImpact>> =
        repo.streamMealImpacts(ctx).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
}
