package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.util.LocalSoundManager
import com.example.ui.theme.macroColors
import kotlinx.coroutines.launch

private val CalorieBlue = Color(0xFF38BDF8)
private val CarbYellow = Color(0xFFFACC15)
private val FatGrey = Color(0xFFCBD5E1)
private val ProteinGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    viewModel: com.example.ui.RecipesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current

    var recipeName by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("1") }
    var searchQuery by remember { mutableStateOf("") }
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    var selectedIngredients by remember { mutableStateOf<List<Pair<FoodItem, Int>>>(emptyList()) }

    val servings = remember(servingsText) {
        servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    // Live Totals & Per Portion Calculations
    val totalGrams = remember(selectedIngredients) {
        selectedIngredients.sumOf { it.second }
    }
    val totalNutrition = remember(selectedIngredients) {
        selectedIngredients.fold(com.example.util.NutritionCalculator.NutritionResult(0, 0f, 0f, 0f)) { acc, (food, weight) ->
            val item = com.example.util.NutritionCalculator.fromPer100g(
                quantityGrams = weight,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g
            )
            com.example.util.NutritionCalculator.NutritionResult(
                calories = acc.calories + item.calories,
                protein = acc.protein + item.protein,
                carbs = acc.carbs + item.carbs,
                fat = acc.fat + item.fat
            )
        }
    }
    val totalCalories = totalNutrition.calories
    val totalProtein = totalNutrition.protein
    val totalCarbs = totalNutrition.carbs
    val totalFat = totalNutrition.fat

    val perServingGrams = remember(totalGrams, servings) {
        if (servings > 0) totalGrams / servings else 0
    }
    val perServingCalories = remember(totalCalories, servings) {
        if (servings > 0) (totalCalories.toFloat() / servings).toInt() else 0
    }
    val perServingProtein = remember(totalProtein, servings) {
        if (servings > 0) totalProtein / servings else 0f
    }
    val perServingCarbs = remember(totalCarbs, servings) {
        if (servings > 0) totalCarbs / servings else 0f
    }
    val perServingFat = remember(totalFat, servings) {
        if (servings > 0) totalFat / servings else 0f
    }

    val searchResults = remember(searchQuery, allFoods) {
        if (searchQuery.isBlank()) {
            allFoods.take(25)
        } else {
            allFoods.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.brand.contains(searchQuery, ignoreCase = true) 
            }.take(25)
        }
    }

    val isSaveEnabled = recipeName.isNotBlank() && selectedIngredients.isNotEmpty() && servings >= 1

    fun safePlaySound(soundName: String) {
        try {
            soundManager.playSound(soundName)
        } catch (e: Exception) {
            Log.e("CreateRecipeScreen", "Sound play error", e)
        }
    }

    fun safeHaptics() {
        try {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            Log.e("CreateRecipeScreen", "Haptics error", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Bar (ZERO ARROWS)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Crea Nuova Ricetta",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        if (isSaveEnabled) {
                            safeHaptics()
                            safePlaySound("power_up")
                            viewModel.saveRecipe(recipeName, servings, selectedIngredients)
                            onNavigateBack()
                        }
                    },
                    enabled = isSaveEnabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Salva",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recipe Name & Servings Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    label = { Text("Nome Ricetta (es. Carbonara)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                // Servings Stepper Input
                Column(
                    modifier = Modifier.width(130.dp)
                ) {
                    Text(
                        text = "PORZIONI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val current = servingsText.toIntOrNull() ?: 1
                                servingsText = (current - 1).coerceAtLeast(1).toString()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }

                        Text(
                            text = "$servings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                val current = servingsText.toIntOrNull() ?: 1
                                servingsText = (current + 1).coerceAtMost(99).toString()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Live Hero Macro Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VALORI NUTRIZIONALI (IN TEMPO REALE)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Totale ricetta: ${totalCalories} kcal (${totalGrams}g)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$perServingCalories kcal",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = CalorieBlue
                            )
                            Text(
                                text = "per porzione (${perServingGrams}g)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Macro distribution bar
                    MacroDistributionBar(
                        carbs = totalCarbs,
                        protein = totalProtein,
                        fat = totalFat,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Macro pills row (Per Portion & Total)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutrientPill(
                            label = "Carboidrati",
                            value = perServingCarbs,
                            color = MaterialTheme.macroColors.carbs,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientPill(
                            label = "Proteine",
                            value = perServingProtein,
                            color = MaterialTheme.macroColors.protein,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientPill(
                            label = "Grassi",
                            value = perServingFat,
                            color = MaterialTheme.macroColors.fat,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Selected Ingredients Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INGREDIENTI SELEZIONATI (${selectedIngredients.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedIngredients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun ingrediente ancora aggiunto. Cerca e aggiungi ingredienti sotto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedIngredients.forEachIndexed { index, (food, weight) ->
                        val factor = weight / 100f
                        val itemCals = (food.caloriesPer100g * factor).toInt()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (food.brand.isNotEmpty()) {
                                            Text(
                                                text = food.brand,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$itemCals kcal",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CalorieBlue
                                    )

                                    IconButton(
                                        onClick = {
                                            safeHaptics()
                                            safePlaySound("click")
                                            selectedIngredients = selectedIngredients.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Rimuovi",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                // Inline Gram Stepper Controls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val newWeight = (weight - 10).coerceAtLeast(1)
                                            selectedIngredients = selectedIngredients.mapIndexed { i, pair ->
                                                if (i == index) pair.first to newWeight else pair
                                            }
                                        },
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("-10g", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Text(
                                        text = "${weight}g",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Button(
                                        onClick = {
                                            val newWeight = (weight + 10).coerceAtMost(5000)
                                            selectedIngredients = selectedIngredients.mapIndexed { i, pair ->
                                                if (i == index) pair.first to newWeight else pair
                                            }
                                        },
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Text("+10g", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Food Search Section Header
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AGGIUNGI INGREDIENTI DA DATABASE",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cerca alimento...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            // Search Results List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                searchResults.forEach { food ->
                    val isAlreadyAdded = selectedIngredients.any { it.first.id == food.id && food.id != 0 }

                    Surface(
                        onClick = {
                            safeHaptics()
                            safePlaySound("coin")
                            selectedIngredients = selectedIngredients + (food to 100)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isAlreadyAdded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAlreadyAdded) ProteinGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (food.brand.isNotEmpty()) {
                                    Text(
                                        text = food.brand,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${food.caloriesPer100g} kcal/100g",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CalorieBlue
                                )

                                Button(
                                    onClick = {
                                        safeHaptics()
                                        safePlaySound("coin")
                                        selectedIngredients = selectedIngredients + (food to 100)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aggiungi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
