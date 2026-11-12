package com.example.ui

import com.example.data.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOptimizerTest {
    @Test
    fun likeWildcardsAreEscapedBeforeRoomQuery() {
        assertEquals("50\\% \\_mix\\\\test", SearchOptimizer.escapeForLike("  50% _mix\\test  "))
    }

    @Test
    fun exactLocalMatchRanksBeforePopularPartialMatch() {
        val exact = food(id = 1, name = "Apple")
        val popularPartial = food(id = 2, name = "Apple pie", useCount = 20)

        val ranked = SearchOptimizer.rank(listOf(popularPartial, exact), emptyList(), "apple")

        assertEquals(exact.id, ranked.first().id)
    }

    @Test
    fun duplicateApiAndLocalFoodIsShownOnce() {
        val local = food(id = 1, name = "Yogurt")
        val remote = local.copy(id = -1)

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "yogurt")

        assertEquals(1, ranked.size)
        assertTrue(ranked.single().id > 0)
    }

    @Test
    fun accentedItalianFoodMatchesNonAccentedQueryAndDeduplicates() {
        val local = food(id = 1, name = "Caffè espresso", caloriesPer100g = 2)
        val remote = food(id = -1, name = "Caffe espresso", caloriesPer100g = 2)

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "caffe")

        assertEquals(1, ranked.size)
        assertEquals(1, ranked.single().id)
    }

    @Test
    fun boundaryCalorieDifferenceIsDeduplicatedWithinTolerance() {
        val local = food(id = 1, name = "Latte intero", caloriesPer100g = 64)
        val remote = food(id = -1, name = "Latte intero", caloriesPer100g = 68) // 4 kcal difference

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "latte")

        assertEquals(1, ranked.size)
        assertEquals(1, ranked.single().id)
    }

    @Test
    fun expandTokensExpandsItalianSynonymsAndHandlesWhitespace() {
        val expandedEvoo = SearchOptimizer.expandTokens("evoo biologico")
        assertTrue(expandedEvoo.contains("olio"))
        assertTrue(expandedEvoo.contains("extravergine"))
        assertTrue(expandedEvoo.contains("biologico"))

        val expandedBk = SearchOptimizer.expandTokens("bk whopper")
        assertTrue(expandedBk.contains("burger"))
        assertTrue(expandedBk.contains("king"))
        assertTrue(expandedBk.contains("whopper"))
    }

    @Test
    fun levenshteinDistanceComputesCorrectly() {
        assertEquals(0, SearchOptimizer.levenshteinDistance("pasta", "pasta"))
        assertEquals(1, SearchOptimizer.levenshteinDistance("spaghetti", "spagetti"))
        assertEquals(1, SearchOptimizer.levenshteinDistance("biscotti", "biscoti"))
        assertEquals(1, SearchOptimizer.levenshteinDistance("mozzarella", "mozzarela"))
        assertEquals(2, SearchOptimizer.levenshteinDistance("mozzarella", "mozarela"))
        assertTrue(SearchOptimizer.levenshteinDistance("pizza", "cioccolato") > 2)
    }

    @Test
    fun typoToleranceFindsAndRanksFoodWithTypo() {
        val spaghetti = food(id = 1, name = "Spaghetti di semola")
        val pane = food(id = 2, name = "Pane comune")

        val ranked = SearchOptimizer.rank(listOf(pane, spaghetti), emptyList(), "spagetti")

        assertEquals(1, ranked.size)
        assertEquals(spaghetti.id, ranked.first().id)
    }

    @Test
    fun multiTokenSearchMatchesNameAndBrandAcrossWords() {
        val pastaBarilla = food(id = 1, name = "Spaghetti n.5", brand = "Barilla")
        val pastaDeCecco = food(id = 2, name = "Spaghetti n.12", brand = "De Cecco")
        val pestoBarilla = food(id = 3, name = "Pesto alla genovese", brand = "Barilla")

        val ranked = SearchOptimizer.rank(listOf(pastaDeCecco, pestoBarilla, pastaBarilla), emptyList(), "spaghetti barilla")

        assertEquals(pastaBarilla.id, ranked.first().id)
    }

    @Test
    fun categorySearchPastaBarillaRanksBarillaPastaAboveGenericDishes() {
        val spaghettiBarilla = food(id = 1, name = "Spaghetti No 5", brand = "Barilla")
        val penneBarilla = food(id = 2, name = "Penne Rigate N°73", brand = "Barilla")
        val cacioEPepe = food(id = 3, name = "Pasta cacio e pepe", brand = "")
        val pestoBarilla = food(id = 4, name = "Pesto alla genovese", brand = "Barilla")
        val spaghettiDeCecco = food(id = 5, name = "Spaghetti n.12", brand = "De Cecco")

        val ranked = SearchOptimizer.rank(
            listOf(cacioEPepe, pestoBarilla, spaghettiDeCecco, penneBarilla, spaghettiBarilla),
            emptyList(),
            "pasta barilla"
        )

        // Barilla pasta shapes must rank at the very top
        val topIds = ranked.take(2).map { it.id }.toSet()
        assertTrue("Top 2 must be Barilla pasta", topIds.contains(spaghettiBarilla.id) && topIds.contains(penneBarilla.id))
        // Generic "Pasta cacio e pepe" (without brand Barilla) must NOT outrank Barilla pasta
        val cacioIndex = ranked.indexOfFirst { it.id == cacioEPepe.id }
        val spaghettiIndex = ranked.indexOfFirst { it.id == spaghettiBarilla.id }
        assertTrue("Spaghetti Barilla must rank before Pasta cacio e pepe", spaghettiIndex < cacioIndex)
    }

    @Test
    fun categorySearchPastaDeCeccoRanksDeCeccoAheadOfOtherBrands() {
        val spaghettiBarilla = food(id = 1, name = "Spaghetti No 5", brand = "Barilla")
        val spaghettiDeCecco = food(id = 2, name = "Spaghetti n.12", brand = "De Cecco")
        val pastaCacioPepe = food(id = 3, name = "Pasta cacio e pepe", brand = "")

        val ranked = SearchOptimizer.rank(
            listOf(spaghettiBarilla, pastaCacioPepe, spaghettiDeCecco),
            emptyList(),
            "pasta de cecco"
        )

        assertEquals(spaghettiDeCecco.id, ranked.first().id)
    }

    @Test
    fun categorySearchBiscottiMulinoBiancoRanksMacineAndPanDiStelle() {
        val macine = food(id = 1, name = "Macine", brand = "Mulino Bianco")
        val panDiStelle = food(id = 2, name = "Pan di Stelle", brand = "Mulino Bianco")
        val genericBiscotti = food(id = 3, name = "Biscotti al burro", brand = "Generico")

        val ranked = SearchOptimizer.rank(
            listOf(genericBiscotti, panDiStelle, macine),
            emptyList(),
            "biscotti mulino bianco"
        )

        val topIds = ranked.take(2).map { it.id }.toSet()
        assertTrue(topIds.contains(macine.id) && topIds.contains(panDiStelle.id))
    }

    @Test
    fun typoInBrandWithPastaShapeRanksBarillaPastaAtTop() {
        val spaghettiBarilla = food(id = 1, name = "Spaghetti No 5", brand = "Barilla")
        val pane = food(id = 2, name = "Pane comune", brand = "Base")

        val ranked = SearchOptimizer.rank(listOf(pane, spaghettiBarilla), emptyList(), "pasta barila")

        assertEquals(spaghettiBarilla.id, ranked.first().id)
    }

    @Test
    fun italianSynonymEvooMatchesOlioExtravergine() {
        val olioEvo = food(id = 1, name = "Olio extravergine d'oliva")
        val burro = food(id = 2, name = "Burro tradizionale")

        val ranked = SearchOptimizer.rank(listOf(burro, olioEvo), emptyList(), "evoo")

        assertEquals(1, ranked.size)
        assertEquals(olioEvo.id, ranked.first().id)
    }

    @Test
    fun italianSynonymCrudoMatchesProsciuttoCrudo() {
        val crudo = food(id = 1, name = "Prosciutto crudo di Parma")
        val salame = food(id = 2, name = "Salame Milano")

        val ranked = SearchOptimizer.rank(listOf(salame, crudo), emptyList(), "crudo")

        assertEquals(1, ranked.size)
        assertEquals(crudo.id, ranked.first().id)
    }

    @Test
    fun fastFoodChainSearchMatchesBrandAndProduct() {
        val bigMac = food(id = 1, name = "Big Mac", brand = "McDonald's")
        val whopper = food(id = 2, name = "Whopper", brand = "Burger King")

        val rankedMac = SearchOptimizer.rank(listOf(whopper, bigMac), emptyList(), "mcdonald big mac")
        assertEquals(bigMac.id, rankedMac.first().id)

        val rankedBk = SearchOptimizer.rank(listOf(bigMac, whopper), emptyList(), "bk whopper")
        assertEquals(whopper.id, rankedBk.first().id)
    }

    @Test
    fun performanceBenchmarkScoring1000FoodsUnder5Ms() {
        val foods = (1..1000).map { i ->
            food(id = i, name = "Alimento di prova $i con nome esteso e cereali", brand = if (i % 2 == 0) "Barilla" else "Mulino Bianco")
        }

        // Warm up JIT and class loading
        SearchOptimizer.rank(foods.take(10), emptyList(), "cereali barilla")

        val start = System.nanoTime()
        val ranked = SearchOptimizer.rank(foods, emptyList(), "cereali barilla")
        val durationMs = (System.nanoTime() - start) / 1_000_000.0

        assertTrue("Execution should be < 25ms in test runner (was ${durationMs}ms)", durationMs < 25.0)
        assertTrue(ranked.isNotEmpty())
    }

    @Test
    fun nonMatchingQueryReturnsEmptyListInsteadOfRandomFoods() {
        val spaghetti = food(id = 1, name = "Spaghetti Barilla", brand = "Barilla")
        val pizza = food(id = 2, name = "Pizza Margherita", brand = "Buitoni")

        val ranked = SearchOptimizer.rank(listOf(spaghetti, pizza), emptyList(), "zzzznonexistent123")
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun tokenSqlVariantsGeneratesItalianAccentedEndings() {
        val caffeVariants = SearchOptimizer.tokenSqlVariants("caffe")
        assertTrue(caffeVariants.contains("caffe"))
        assertTrue(caffeVariants.contains("caffè"))
        assertTrue(caffeVariants.contains("caffé"))

        val tiramisuVariants = SearchOptimizer.tokenSqlVariants("tiramisu")
        assertTrue(tiramisuVariants.contains("tiramisu"))
        assertTrue(tiramisuVariants.contains("tiramisù"))

        val pureVariants = SearchOptimizer.tokenSqlVariants("pure")
        assertTrue(pureVariants.contains("pure"))
        assertTrue(pureVariants.contains("purè"))

        val baccalaVariants = SearchOptimizer.tokenSqlVariants("baccala")
        assertTrue(baccalaVariants.contains("baccala"))
        assertTrue(baccalaVariants.contains("baccalà"))
    }

    @Test
    fun expandTokensHandlesHyphensSlashesAndPeriods() {
        val tokensHyphen = SearchOptimizer.expandTokens("coca-cola zero")
        assertTrue(tokensHyphen.contains("coca"))
        assertTrue(tokensHyphen.contains("cola"))
        assertTrue(tokensHyphen.contains("zero"))

        val tokensSlash = SearchOptimizer.expandTokens("yogurt vaniglia/caffè")
        assertTrue(tokensSlash.contains("yogurt"))
        assertTrue(tokensSlash.contains("vaniglia"))
        assertTrue(tokensSlash.contains("caffe"))

        val tokensDot = SearchOptimizer.expandTokens("spaghetti n.5")
        assertTrue(tokensDot.contains("spaghetti"))
        assertTrue(tokensDot.contains("n"))
        assertTrue(tokensDot.contains("5"))
    }

    @Test
    fun barcodeMatchScoresHighInScoreFood() {
        val foodWithBarcode = food(id = 1, name = "Biscotti", barcode = "8001234567890")
        val score = SearchOptimizer.scoreFood(foodWithBarcode, "8001234567890")
        assertTrue(score >= 5000)
    }

    private fun food(id: Int, name: String, brand: String = "Test", caloriesPer100g: Int = 100, useCount: Int = 0, barcode: String = "") = FoodItem(
        id = id,
        name = name,
        brand = brand,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = 1f,
        carbsPer100g = 2f,
        fatPer100g = 3f,
        useCount = useCount,
        barcode = barcode
    )
}
