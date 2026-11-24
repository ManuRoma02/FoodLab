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

    // Category-to-format mappings for semantic Italian food searches (e.g. "pasta" matches "spaghetti", "penne", etc.)
    val CATEGORY_SHAPES: Map<String, Set<String>> = mapOf(
        "pasta" to setOf(
            "spaghetti", "spaghettini", "spaghettoni", "penne", "pennette", "fusilli", "farfalle",
            "rigatoni", "tortiglioni", "maccheroni", "tagliatelle", "bavette", "linguine",
            "sedanini", "sedani", "ditalini", "bucatini", "orecchiette", "lasagne", "gnocchi",
            "ravioli", "tortellini", "vermicelli", "pappardelle", "fettuccine", "cannelloni",
            "mezze maniche", "paccheri", "calamarata", "caserecce", "trofie", "pici", "garganelli",
            "strozzapreti", "capellini", "ziti", "mafaldine", "anellini", "stelline", "maltagliati"
        ),
        "biscotti" to setOf(
            "frollini", "savoiardi", "gocciole", "pan di stelle", "tarallucci", "abbracci",
            "nascondini", "baiocchi", "macine", "campagnole", "batticuori", "spicchi di sole",
            "ritornelli", "galletti", "rigoli", "molinetti", "cuoricini", "krumiri", "amaretti",
            "cantucci", "wafer", "oro saiwa", "biscotto", "frollino"
        ),
        "biscotto" to setOf(
            "frollini", "savoiardi", "gocciole", "pan di stelle", "tarallucci", "abbracci",
            "nascondini", "baiocchi", "macine", "campagnole", "batticuori", "spicchi di sole",
            "ritornelli", "galletti", "rigoli", "molinetti", "cuoricini", "krumiri", "amaretti",
            "cantucci", "wafer", "oro saiwa"
        ),
        "cereali" to setOf(
            "muesli", "granola", "corn flakes", "avena", "fiocchi", "porridge", "cheerios", "special k"
        ),
        "riso" to setOf(
            "basmati", "carnaroli", "arborio", "vialone", "roma", "originario", "parboiled",
            "venere", "rosso", "integrale", "risotto"
        ),
        "pane" to setOf(
            "ciabatta", "baguette", "rosetta", "focaccia", "piadina", "toast", "pancarre",
            "pan bauletto", "michetta", "sfilatino"
        )
    )

    val ITALIAN_STOPWORDS: Set<String> = setOf(
        "il", "lo", "la", "i", "gli", "le",
        "un", "uno", "una", "un'",
        "di", "del", "dello", "della", "dei", "degli", "delle",
        "a", "ad", "al", "allo", "alla", "ai", "agli", "alle",
        "da", "dal", "dallo", "dalla", "dai", "dagli", "dalle",
        "in", "nel", "nello", "nella", "nei", "negli", "nelle",
        "con", "col", "coi",
        "su", "sul", "sullo", "sulla", "sui", "sugli", "sulle",
        "per", "tra", "fra",
        "e", "ed", "o", "od"
    )

    fun normalizeQuery(query: String): String {
        return WHITESPACE_REGEX.replace(query.trim(), " ")
    }

    fun expandTokens(rawQuery: String): List<String> {
        val sanitized = rawQuery
            .replace('/', ' ')
            .replace('-', ' ')
            .replace('.', ' ')
            .replace(',', ' ')
        val norm = normalizeDiacritics(normalizeQuery(sanitized)).lowercase(Locale.ROOT)
        val tokens = norm.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val expanded = mutableListOf<String>()
        for (t in tokens) {
            val syn = SYNONYMS[t]
            if (syn != null) {
                expanded.addAll(syn.split(WHITESPACE_REGEX).filter { it.isNotBlank() })
            } else {
                expanded.add(t)
            }
        }
        return expanded
    }

    fun searchTokens(rawQuery: String): List<String> {
        val tokens = expandTokens(rawQuery)
        val content = tokens.filter { token ->
            token !in ITALIAN_STOPWORDS && (token.length >= 2 || token.all { it.isDigit() })
        }
        return content.ifEmpty { tokens }
    }

    fun tokenSqlVariants(token: String): List<String> {
        val clean = escapeForLike(token)
        val norm = escapeForLike(normalizeDiacritics(token))
        val variants = linkedSetOf(clean, norm)

        val target = if (norm.isNotBlank()) norm else clean
        if (target.endsWith("e")) {
            val base = target.dropLast(1)
            variants.add("${base}è")
            variants.add("${base}é")
        } else if (target.endsWith("u")) {
            val base = target.dropLast(1)
            variants.add("${base}ù")
            variants.add("${base}ú")
        } else if (target.endsWith("a")) {
            val base = target.dropLast(1)
            variants.add("${base}à")
            variants.add("${base}á")
        } else if (target.endsWith("i")) {
            val base = target.dropLast(1)
            variants.add("${base}ì")
            variants.add("${base}í")
        } else if (target.endsWith("o")) {
            val base = target.dropLast(1)
            variants.add("${base}ò")
            variants.add("${base}ó")
        }
        return variants.filter { it.isNotBlank() }
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

    fun categoryShapeNeedles(token: String): List<String> {
        val shapes = CATEGORY_SHAPES[token] ?: return emptyList()
        return shapes.map { escapeForLike(it) }
    }

    fun escapeForLike(query: String): String {
        return query.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    private fun stripSpaces(text: String): String {
        var hasSpace = false
        for (i in text.indices) {
            if (text[i].isWhitespace()) { hasSpace = true; break }
        }
        if (!hasSpace) return text
        val sb = StringBuilder(text.length)
        for (i in text.indices) {
            val ch = text[i]
            if (!ch.isWhitespace()) sb.append(ch)
        }
        return sb.toString()
    }

    private fun splitWords(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val result = ArrayList<String>(4)
        var start = -1
        for (i in text.indices) {
            if (text[i].isWhitespace()) {
                if (start >= 0) {
                    result.add(text.substring(start, i))
                    start = -1
                }
            } else if (start < 0) {
                start = i
            }
        }
        if (start >= 0) result.add(text.substring(start))
        return result
    }

    private class PreparedQuery(rawNormalizedQuery: String) {
        val normQuery: String = normalizeDiacritics(rawNormalizedQuery)
        val compactQuery: String = stripSpaces(normQuery)
        val tokens: List<String> = splitWords(normQuery)
        val tokenAllDigits: BooleanArray = BooleanArray(tokens.size) { tokens[it].all(Char::isDigit) }
        val tokenCategories: Array<Set<String>?> = Array(tokens.size) { CATEGORY_SHAPES[tokens[it]] }
        val tokenSynonyms: Array<List<String>?> = Array(tokens.size) { SYNONYMS[tokens[it]]?.let(::splitWords) }
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
        val prepared = PreparedQuery(normalizedQuery)
        val scored = ArrayList<Pair<FoodItem, Int>>(merged.size)
        for (food in merged) {
            val s = scoreFoodPrepared(food, prepared)
            if (s > 0) scored.add(food to s)
        }
        scored.sortWith(
            compareByDescending<Pair<FoodItem, Int>> { it.second }
                .thenByDescending { it.first.isFavorite }
                .thenByDescending { it.first.useCount }
                .thenBy { it.first.name.lowercase(Locale.ROOT) }
        )
        return scored.map { it.first }
    }

    private fun defaultSort(): Comparator<FoodItem> {
        return compareByDescending<FoodItem> { it.isFavorite }
            .thenByDescending { it.useCount }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    }

    fun normalizeDiacritics(text: String): String {
        val trimmed = text.trim().lowercase(Locale.ROOT)
        var hasNonAscii = false
        var hasApostrophe = false
        for (i in trimmed.indices) {
            val ch = trimmed[i]
            if (ch.code >= 128) hasNonAscii = true
            if (ch == '\'' || ch == '\u2019' || ch == '`') hasApostrophe = true
        }
        val withoutAccents = if (!hasNonAscii) {
            trimmed
        } else {
            val nfd = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
            DIACRITICS_REGEX.replace(nfd, "")
        }
        return if (hasApostrophe) APOSTROPHE_REGEX.replace(withoutAccents, " ") else withoutAccents
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
        return scoreFoodPrepared(food, PreparedQuery(normalizedQuery))
    }

    private fun scoreFoodPrepared(food: FoodItem, query: PreparedQuery): Int {
        val normQuery = query.normQuery
        val name = normalizeDiacritics(food.name)
        val brand = normalizeDiacritics(food.brand)
        val compactQuery = query.compactQuery
        val compactName = stripSpaces(name)
        val compactBrand = stripSpaces(brand)

        val tokens = query.tokens
        val nameWords = splitWords(name)
        val brandWords = splitWords(brand)

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
        var matchedBrandTokensCount = 0

        for (idx in tokens.indices) {
            val token = tokens[idx]
            var tokenMatched = false
            var tokenMatchedBrand = false

            // Brand direct matches (boosted so brand query heavily rewards brand)
            if (brand == token) { score += 700; tokenMatched = true; tokenMatchedBrand = true }
            if (brand.startsWith(token)) { score += 450; tokenMatched = true; tokenMatchedBrand = true }
            if (brandWords.contains(token)) { score += 400; tokenMatched = true; tokenMatchedBrand = true }
            if (brand.contains(token)) { score += 250; tokenMatched = true; tokenMatchedBrand = true }

            // Name direct matches
            if (name == token) { score += 600; tokenMatched = true }
            if (name.startsWith(token)) { score += 520; tokenMatched = true }
            if (nameWords.contains(token)) { score += 300; tokenMatched = true }
            if (name.contains(token)) { score += 230; tokenMatched = true }

            val isDigitToken = query.tokenAllDigits[idx]
            if (isDigitToken && food.defaultServingSize.toString().contains(token)) {
                score += 160
                tokenMatched = true
            }

            if (isDigitToken && token.length >= 6 && food.barcode.isNotEmpty() && food.barcode.contains(token)) {
                score += 5000
                tokenMatched = true
            }

            // Category semantic format match (e.g. token is "pasta" and food name contains "spaghetti", "penne", etc.)
            val categoryFormats = query.tokenCategories[idx]
            if (categoryFormats != null) {
                if (categoryFormats.any { fmt -> name.contains(fmt) }) {
                    score += 750
                    tokenMatched = true
                }
            }

            // Synonym / abbreviation check (e.g. evoo -> olio extravergine oliva)
            val synWords = query.tokenSynonyms[idx]
            if (synWords != null) {
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
                    score += 320
                    tokenMatched = true
                    tokenMatchedBrand = true
                } else if (token.length >= 7) {
                    // Check distance 2 for tokens >= 7 chars
                    val hitNameDist2 = nameWords.any { nw -> nw.length >= 5 && levenshteinDistance(token, nw, 2) <= 2 }
                    val hitBrandDist2 = brandWords.any { bw -> bw.length >= 5 && levenshteinDistance(token, bw, 2) <= 2 }
                    if (hitNameDist2) {
                        score += 200
                        tokenMatched = true
                    } else if (hitBrandDist2) {
                        score += 180
                        tokenMatched = true
                        tokenMatchedBrand = true
                    }
                }
            }

            if (tokenMatched) {
                matchedTokensCount++
            }
            if (tokenMatchedBrand) {
                matchedBrandTokensCount++
            }
        }

        // Multi-token match across name, brand, category shapes, or synonyms
        if (tokens.size > 1 && matchedTokensCount == tokens.size) {
            score += 2000
            if (matchedBrandTokensCount > 0) {
                score += 1000 // Product/category + brand match synergy bonus
            }
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
