package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.FoodLabApplication
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.StreakCalculator
import java.util.Date
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FoodLabApplication).repository

    val allWeights: StateFlow<List<WeightLog>> = repository.allWeights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealEntry>> = repository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeight(weight: Float, dateString: String) {
        if (!weight.isFinite() || weight !in 20f..400f) return
        viewModelScope.launch {
            repository.insertWeight(
                WeightLog(dateString = dateString, weightKg = weight)
            )
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteLatestWeight() {
        viewModelScope.launch {
            repository.deleteLatestWeight()
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun calculateStreak(meals: List<MealEntry>, targetCalories: Int): Int {
        return StreakCalculator.calculate(
            meals = meals,
            targets = listOf(DailyTarget(dateString = "FIXED", targetCalories = targetCalories)),
            today = SessionManager.formatDate(Date())
        )
    }

}
