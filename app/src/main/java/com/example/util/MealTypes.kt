package com.example.util

object MealTypes {
    const val BREAKFAST = "BREAKFAST"
    const val LUNCH = "LUNCH"
    const val DINNER = "DINNER"
    const val SNACK = "SNACK"
    val ALL = listOf(BREAKFAST, LUNCH, DINNER, SNACK)

    fun canonicalize(type: String?): String {
        return when (type?.trim()?.uppercase()) {
            "BREAKFAST", "COLAZIONE" -> BREAKFAST
            "LUNCH", "PRANZO" -> LUNCH
            "DINNER", "CENA" -> DINNER
            "SNACK", "SPUNTINO" -> SNACK
            else -> type?.trim()?.uppercase() ?: BREAKFAST
        }
    }

    fun displayName(type: String?): String {
        return when (canonicalize(type)) {
            BREAKFAST -> "Colazione"
            LUNCH -> "Pranzo"
            DINNER -> "Cena"
            SNACK -> "Spuntino"
            else -> type?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Pasto"
        }
    }

    fun inferMealTypeFromHour(hour: Int): String {
        return when (hour) {
            in 4..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}
