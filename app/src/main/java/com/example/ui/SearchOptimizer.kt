package com.example.ui

import com.example.data.FoodItem
import java.util.Locale

object SearchOptimizer {
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val DIACRITICS_REGEX = Regex("\\p{M}")
    private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")
    private val GENERIC_BRANDS = setOf("generic", "custom", "openfoodfacts", "crea", "generico")

    fun normalizeQuery(query: String): String {
        return WHITESPACE_REGEX.replace(query.trim(), " ")
    }

    fun cacheKey(query: String): String {
        return normalizeQuery(query).lowercase(Locale.ROOT)
    }

    fun shouldSearch(query: String): Boolean {
        return normalizeQuery(query).length >= 2
    }

    fun shouldCacheApiResults(results: List<FoodItem>): Boolean {
        return results.isNotEmpty()
    }

    fun isResponseForActiveQuery(responseQuery: String, activeQuery: String): Boolean {
        return cacheKey(responseQuery) == cacheKey(activeQuery)
    }

    fun escapeForLike(query: String): String {
        return query.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    fun rank(localFoods: List<FoodItem>, apiFoods: List<FoodItem>, rawQuery: String): List<FoodItem> {
        val normalizedQuery = cacheKey(rawQuery)
        if (normalizedQuery.isBlank()) {
            if (apiFoods.isEmpty()) {
                return localFoods.sortedWith(defaultSort())
            }
            val merged = deduplicate(localFoods, apiFoods)
            return merged.sortedWith(defaultSort())
        }

        val merged = if (apiFoods.isEmpty()) localFoods else deduplicate(localFoods, apiFoods)
        val scored = merged.map { it to scoreFood(it, normalizedQuery) }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<FoodItem, Int>> { it.second }
                    .thenByDescending { it.first.isFavorite }
                    .thenByDescending { it.first.useCount }
                    .thenBy { it.first.name.lowercase(Locale.ROOT) }
            )
            .map { it.first }
        return if (scored.isNotEmpty()) scored else merged.take(20)
    }

    private fun defaultSort(): Comparator<FoodItem> {
        return compareByDescending<FoodItem> { it.isFavorite }
            .thenByDescending { it.useCount }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    }

    fun normalizeDiacritics(text: String): String {
        val nfd = java.text.Normalizer.normalize(text.trim().lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(nfd, "")
    }

    fun normalizeAlphanumeric(text: String): String {
        return NON_ALPHANUMERIC_REGEX.replace(normalizeDiacritics(text), "")
    }

    private fun deduplicate(localFoods: List<FoodItem>, apiFoods: List<FoodItem>): List<FoodItem> {
        if (apiFoods.isEmpty()) return localFoods
        val result = ArrayList<FoodItem>(localFoods.size + apiFoods.size)
        result.addAll(localFoods)

        // Precompute local food signatures for fast O(1)/O(K) candidate lookup
        val localByKey = localFoods.groupBy { normalizeAlphanumeric(it.name) }

        for (remote in apiFoods) {
            val remoteName = normalizeAlphanumeric(remote.name)
            val candidates = localByKey[remoteName]
            if (candidates == null) {
                result.add(remote)
                continue
            }

            val remoteBrand = normalizeAlphanumeric(remote.brand)
            val isRemoteGeneric = remoteBrand.isEmpty() || remoteBrand in GENERIC_BRANDS

            val isDuplicate = candidates.any { local ->
                val localBrand = normalizeAlphanumeric(local.brand)
                val isLocalGeneric = localBrand.isEmpty() || localBrand in GENERIC_BRANDS
                val brandMatch = (isRemoteGeneric && isLocalGeneric) || remoteBrand == localBrand
                val calorieMatch = kotlin.math.abs(local.caloriesPer100g - remote.caloriesPer100g) <= 15
                brandMatch && calorieMatch
            }

            if (!isDuplicate) {
                result.add(remote)
            }
        }
        return result
    }

    private fun scoreFood(food: FoodItem, normalizedQuery: String): Int {
        val normQuery = normalizeDiacritics(normalizedQuery)
        val name = normalizeDiacritics(food.name)
        val brand = normalizeDiacritics(food.brand)
        val compactQuery = WHITESPACE_REGEX.replace(normQuery, "")
        val compactName = WHITESPACE_REGEX.replace(name, "")
        val compactBrand = WHITESPACE_REGEX.replace(brand, "")

        val tokens = normQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val nameWords = name.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        var score = 0
        if (name == normQuery) score += 6000
        if (brand == normQuery) score += 4500
        if (name.startsWith(normQuery)) score += 3200
        if (brand.startsWith(normQuery)) score += 2200
        if (name.contains(normQuery)) score += 1800
        if (brand.contains(normQuery)) score += 1000
        if (compactName.contains(compactQuery)) score += 800
        if (compactBrand.contains(compactQuery)) score += 500

        for (token in tokens) {
            if (name.startsWith(token)) score += 520
            if (brand.startsWith(token)) score += 320
            if (name.contains(token)) score += 230
            if (brand.contains(token)) score += 140
            if (nameWords.contains(token)) score += 200
            if (token.all { it.isDigit() } && food.defaultServingSize.toString().contains(token)) {
                score += 160
            }
        }

        if (tokens.size > 1) {
            val allTokensMatch = tokens.all { token ->
                name.contains(token) || brand.contains(token)
            }
            if (allTokensMatch) score += 650
        }

        var finalScore = score + (food.useCount * 35)
        if (food.isFavorite) finalScore += 260
        if (food.brand.equals("Recipe", ignoreCase = true)) finalScore += 15
        return if (food.id > 0) finalScore + 10 else finalScore
    }
}
