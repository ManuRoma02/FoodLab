package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.SessionManager
import com.example.util.LocalSoundManager
import com.example.util.MealTypes
import com.example.ui.theme.macroColors
import kotlinx.coroutines.launch
import java.util.Date

// Retro Dark Palette Tokens
private val CalorieBlue = Color(0xFF38BDF8)
private val CarbYellow = Color(0xFFFACC15)
private val FatGrey = Color(0xFFCBD5E1)
private val ProteinGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    db: AppDatabase = AppDatabase.getDatabase(LocalContext.current),
    onNavigateBack: () -> Unit = {},
    onNavigateToCreateRecipe: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current
    val today = remember { SessionManager.formatDate(Date()) }

    val templatesWithItems by db.mealTemplateDao().getAllTemplatesWithItems().collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar Header (NO ARROW ICONS)
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
                Row(
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    Column {
                        Text(
                            text = "Le Mie Ricette",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Piatti composti e ricette personalizzate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager.playSound("click")
                        onNavigateToCreateRecipe()
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuova Ricetta",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // List of Recipes
        if (templatesWithItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Nessuna ricetta salvata",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Crea un piatto composto combinando più ingredienti per registrarlo in 1 tap nei tuoi pasti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSound("click")
                                onNavigateToCreateRecipe()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CREA PRIMA RICETTA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)
            ) {
                items(templatesWithItems, key = { it.template.id }) { recipeWithItems ->
                    RecipeCard(
                        recipeWithItems = recipeWithItems,
                        onDelete = {
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSound("click")
                                db.mealTemplateDao().deleteTemplate(recipeWithItems.template.id)
                            }
                        },
                        onLogToMeal = { mealType ->
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSound("coin")
                                val servings = recipeWithItems.template.servings.coerceAtLeast(1)
                                recipeWithItems.items.forEach { item ->
                                    val portionGrams = (item.quantityGrams.toFloat() / servings).toInt().coerceAtLeast(1)
                                    val entry = MealEntry(
                                        dateString = today,
                                        mealType = mealType,
                                        foodId = item.foodId,
                                        foodName = item.foodName,
                                        quantityGrams = portionGrams,
                                        calories = (item.caloriesPer100g * portionGrams / 100f).toInt(),
                                        protein = item.proteinPer100g * portionGrams / 100f,
                                        carbs = item.carbsPer100g * portionGrams / 100f,
                                        fat = item.fatPer100g * portionGrams / 100f
                                    )
                                    db.mealEntryDao().insertMeal(entry)
                                }
                                onNavigateBack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipeWithItems: MealTemplateWithItems,
    onDelete: () -> Unit,
    onLogToMeal: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val servings = remember(recipeWithItems) {
        recipeWithItems.template.servings.coerceAtLeast(1)
    }

    val totalCals = remember(recipeWithItems) {
        recipeWithItems.items.sumOf { (it.caloriesPer100g * it.quantityGrams / 100f).toInt() }
    }
    val totalPro = remember(recipeWithItems) {
        recipeWithItems.items.fold(0f) { acc, item -> acc + (item.proteinPer100g * item.quantityGrams / 100f) }
    }
    val totalCarbs = remember(recipeWithItems) {
        recipeWithItems.items.fold(0f) { acc, item -> acc + (item.carbsPer100g * item.quantityGrams / 100f) }
    }
    val totalFat = remember(recipeWithItems) {
        recipeWithItems.items.fold(0f) { acc, item -> acc + (item.fatPer100g * item.quantityGrams / 100f) }
    }

    val perServingCals = remember(totalCals, servings) {
        (totalCals.toFloat() / servings).toInt()
    }
    val perServingPro = remember(totalPro, servings) {
        totalPro / servings
    }
    val perServingCarbs = remember(totalCarbs, servings) {
        totalCarbs / servings
    }
    val perServingFat = remember(totalFat, servings) {
        totalFat / servings
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipeWithItems.template.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recipeWithItems.items.size} ingredienti • $servings ${if (servings == 1) "porzione" else "porzioni"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$perServingCals kcal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CalorieBlue
                    )
                    Text(
                        text = "per porzione",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Macro summary pills row per portion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroPill(label = "C: ${String.format("%.1fg", perServingCarbs)}/porz", color = MaterialTheme.macroColors.carbs)
                MacroPill(label = "P: ${String.format("%.1fg", perServingPro)}/porz", color = MaterialTheme.macroColors.protein)
                MacroPill(label = "F: ${String.format("%.1fg", perServingFat)}/porz", color = MaterialTheme.macroColors.fat)
            }

            // Expanded ingredients details & total comparison
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "RIEPILOGO NUTRIZIONALE",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Per porzione (1 di $servings): $perServingCals kcal | C: ${String.format("%.1fg", perServingCarbs)}g | P: ${String.format("%.1fg", perServingPro)}g | F: ${String.format("%.1fg", perServingFat)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Totale ricetta: $totalCals kcal | C: ${String.format("%.1fg", totalCarbs)}g | P: ${String.format("%.1fg", totalPro)}g | F: ${String.format("%.1fg", totalFat)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INGREDIENTI",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    recipeWithItems.items.forEach { item ->
                        val itemCals = (item.caloriesPer100g * item.quantityGrams / 100f).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${item.foodName} (${item.quantityGrams}g)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$itemCals kcal",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = CalorieBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "REGISTRA 1 PORZIONE IN 1-TAP",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(MealTypes.BREAKFAST, MealTypes.LUNCH, MealTypes.DINNER, MealTypes.SNACK).forEach { meal ->
                            AssistChip(
                                onClick = { onLogToMeal(meal) },
                                label = { Text(meal, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
