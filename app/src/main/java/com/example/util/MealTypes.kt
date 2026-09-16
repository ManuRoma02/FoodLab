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
}
