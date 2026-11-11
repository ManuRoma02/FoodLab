package com.example.ui

import com.example.data.FoodItem
import java.util.Locale

object SearchOptimizer {
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val DIACRITICS_REGEX = Regex("\\p{M}")
    private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")
    private val APOSTROPHE_REGEX = Regex("['’`]")
    private val GENERIC_BRANDS = setOf("generic", "custom", "openfoodfacts", "crea", "generico")

    // Common Italian food synonyms, abbreviations, and colloquial search terms
    private val SYNONYMS: Map<String, String> = mapOf(
        "evoo" to "olio extravergine oliva",
        "evo" to "olio extravergine oliva",
        "crudo" to "prosciutto crudo",
        "cotto" to "prosciutto cotto",
        "fesa" to "fesa tacchino",
        "parmigiano" to "parmigiano reggiano",
        "grana" to "grana padano",
        "fiocchi" to "fiocchi latte",
        "albume" to "albume uovo",
        "albumi" to "albume uovo",
        "bresaola" to "bresaola valtellina",
        "mac" to "mcdonald",
        "mc" to "mcdonald",
        "bk" to "burger king",
        "kfc" to "kentucky chicken"
    )

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
        val withoutAccents = DIACRITICS_REGEX.replace(nfd, "")
        return APOSTROPHE_REGEX.replace(withoutAccents, " ")
    }

    fun normalizeAlphanumeric(text: String): String {
        return NON_ALPHANUMERIC_REGEX.replace(normalizeDiacritics(text), "")
    }

    fun levenshteinDistance(s1: String, s2: String, maxDistance: Int = 2): Int {
        if (s1 == s2) return 0
        val len1 = s1.length
        val len2 = s2.length
        if (kotlin.math.abs(len1 - len2) > maxDistance) return maxDistance + 1
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        val dp = IntArray(len2 + 1) { it }
        for (i in 1..len1) {
            var prev = dp[0]
            dp[0] = i
            val c1 = s1[i - 1]
            for (j in 1..len2) {
                val temp = dp[j]
                val cost = if (c1 == s2[j - 1]) 0 else 1
                dp[j] = minOf(
                    dp[j] + 1,       // deletion
                    dp[j - 1] + 1,   // insertion
                    prev + cost      // substitution
                )
                prev = temp
            }
        }
        return dp[len2]
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

    fun scoreFood(food: FoodItem, normalizedQuery: String): Int {
        val normQuery = normalizeDiacritics(normalizedQuery)
        val name = normalizeDiacritics(food.name)
        val brand = normalizeDiacritics(food.brand)
        val compactQuery = WHITESPACE_REGEX.replace(normQuery, "")
        val compactName = WHITESPACE_REGEX.replace(name, "")
        val compactBrand = WHITESPACE_REGEX.replace(brand, "")

        val tokens = normQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val nameWords = name.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val brandWords = brand.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        var score = 0
        if (name == normQuery) score += 6000
        if (brand == normQuery) score += 4500
        if (name.startsWith(normQuery)) score += 3200
        if (brand.startsWith(normQuery)) score += 2200
        if (name.contains(normQuery)) score += 1800
        if (brand.contains(normQuery)) score += 1000
        if (compactName.contains(compactQuery)) score += 800
        if (compactBrand.contains(compactQuery)) score += 500

        var matchedTokensCount = 0

        for (token in tokens) {
            var tokenMatched = false

            if (name.startsWith(token)) { score += 520; tokenMatched = true }
            if (brand.startsWith(token)) { score += 320; tokenMatched = true }
            if (name.contains(token)) { score += 230; tokenMatched = true }
            if (brand.contains(token)) { score += 140; tokenMatched = true }
            if (nameWords.contains(token)) { score += 200; tokenMatched = true }
            if (brandWords.contains(token)) { score += 150; tokenMatched = true }

            if (token.all { it.isDigit() } && food.defaultServingSize.toString().contains(token)) {
                score += 160
                tokenMatched = true
            }

            // Synonym / abbreviation check (e.g. evoo -> olio extravergine oliva)
            val syn = SYNONYMS[token]
            if (syn != null) {
                val synWords = syn.split(WHITESPACE_REGEX)
                for (sw in synWords) {
                    if (name.contains(sw) || brand.contains(sw)) {
                        score += 350
                        tokenMatched = true
                        break
                    }
                }
            }

            // Word-level Typo matching with bounded Levenshtein distance
            if (!tokenMatched && token.length >= 4) {
                // Check distance 1 for tokens >= 4 chars
                val hitNameDist1 = nameWords.any { nw -> nw.length >= 3 && levenshteinDistance(token, nw, 1) <= 1 }
                val hitBrandDist1 = brandWords.any { bw -> bw.length >= 3 && levenshteinDistance(token, bw, 1) <= 1 }

                if (hitNameDist1) {
                    score += 380
                    tokenMatched = true
                } else if (hitBrandDist1) {
                    score += 240
                    tokenMatched = true
                } else if (token.length >= 7) {
                    // Check distance 2 for tokens >= 7 chars
                    val hitNameDist2 = nameWords.any { nw -> nw.length >= 5 && levenshteinDistance(token, nw, 2) <= 2 }
                    val hitBrandDist2 = brandWords.any { bw -> bw.length >= 5 && levenshteinDistance(token, bw, 2) <= 2 }
                    if (hitNameDist2) {
                        score += 200
                        tokenMatched = true
                    } else if (hitBrandDist2) {
                        score += 120
                        tokenMatched = true
                    }
                }
            }

            if (tokenMatched) {
                matchedTokensCount++
            }
        }

        // Multi-token match across name, brand, or synonyms
        if (tokens.size > 1 && matchedTokensCount == tokens.size) {
            score += 1500
        } else if (tokens.size > 1 && matchedTokensCount > 0) {
            score += (matchedTokensCount * 250)
        }

        if (score <= 0) return 0

        var finalScore = score + (food.useCount * 35)
        if (food.isFavorite) finalScore += 300
        if (food.brand.equals("Recipe", ignoreCase = true)) finalScore += 15
        return if (food.id > 0) finalScore + 10 else finalScore
    }
}
