package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FoodLabApplication
import com.example.data.DailyTarget
import com.example.data.MealEntry
import com.example.data.WeightLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FoodLabApplication).repository

    val weights: StateFlow<List<WeightLog>> = repository.allWeightsDesc
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealEntry>> = repository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTargets: StateFlow<List<DailyTarget>> = repository.allTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixedTarget: StateFlow<DailyTarget?> = repository.getFixedTarget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun insertWeight(weightKg: Float, dateString: String) {
        if (!weightKg.isFinite() || weightKg !in 20f..400f) return
        viewModelScope.launch {
            repository.insertWeight(
                WeightLog(
                    dateString = dateString,
                    weightKg = weightKg,
                    timestamp = System.currentTimeMillis()
                )
            )
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteWeight(id: Int) {
        viewModelScope.launch {
            repository.deleteWeightById(id)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }
}
