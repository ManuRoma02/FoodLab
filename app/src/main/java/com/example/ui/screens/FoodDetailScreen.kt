package com.example.ui.screens

import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import android.util.Log
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyTarget
import com.example.data.MealEntry
import com.example.ui.DashboardViewModel
import com.example.ui.FoodSearchViewModel
import com.example.ui.SettingsViewModel
import com.example.util.MealTypes
import com.example.ui.components.BlinkingText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import java.util.Locale
import java.util.Calendar
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.data.FoodItem
import kotlinx.coroutines.launch
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun FoodDetailScreen(
    viewModel: FoodSearchViewModel,
    dashboardViewModel: com.example.ui.DashboardViewModel,
    pedometerViewModel: com.example.ui.PedometerViewModel,
    foodId: Int,
    currentDate: String,
    mealType: String,
    onNavigateBack: () -> Unit,
    onMealLogged: () -> Unit = onNavigateBack
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val soundManager = com.example.util.LocalSoundManager.current
    val dailyTarget by dashboardViewModel.dailyTarget.collectAsStateWithLifecycle()
    val dailyMeals by dashboardViewModel.dailyMeals.collectAsStateWithLifecycle()
    val latestWeight by dashboardViewModel.latestWeight.collectAsStateWithLifecycle()

    val liveSteps by pedometerViewModel.steps.collectAsStateWithLifecycle()
    val preferencesManager = com.example.util.LocalPreferencesManager.current
    val syncStepsEnabled by preferencesManager.syncSteps.collectAsStateWithLifecycle()
    val isDateToday = remember(currentDate) { com.example.ui.SessionManager.isToday(currentDate) }
    val steps = if (isDateToday) liveSteps else remember(currentDate, syncStepsEnabled) {
        if (syncStepsEnabled) com.example.util.StepCounterStore.savedSteps(context, currentDate) else 0
    }
    
    val caloriesBurned = remember(steps, latestWeight) {
        com.example.util.NutritionCalculator.calculateCaloriesBurned(steps, latestWeight?.weightKg)
    }
    LaunchedEffect(currentDate) {
        dashboardViewModel.setDate(currentDate)
    }

    val consumedCalories = remember(dailyMeals) { dailyMeals.sumOf { it.calories } }
    val remainingCalories = remember(dailyTarget.targetCalories, consumedCalories, caloriesBurned) { dailyTarget.targetCalories - consumedCalories + caloriesBurned }
    val mealsForCurrentCategory = remember(dailyMeals, mealType) {
        val canon = MealTypes.canonicalize(mealType)
        dailyMeals.filter { MealTypes.canonicalize(it.mealType) == canon }
    }
    val currentMealCalories = remember(mealsForCurrentCategory) { mealsForCurrentCategory.sumOf { it.calories } }

    var isFoodLoading by remember(foodId) { mutableStateOf(true) }
    val food by produceState<FoodItem?>(initialValue = null, foodId) {
        value = viewModel.getFoodById(foodId)
        isFoodLoading = false
    }
    
    val portions by produceState<List<com.example.data.FoodPortion>>(initialValue = emptyList(), foodId) {
        value = viewModel.getPortionsForFood(foodId)
    }

    val selectablePortions = remember(food, portions) {
        val list = mutableListOf<com.example.data.FoodPortion>()
        list.add(com.example.data.FoodPortion(id = -1, foodId = foodId, name = "Grammi", weightGrams = 1.0f))
        food?.let { f ->
            if (f.defaultServingSize > 0 && f.defaultServingSize != 1) {
                list.add(com.example.data.FoodPortion(id = -2, foodId = foodId, name = "Porzione (${f.defaultServingSize}g)", weightGrams = f.defaultServingSize.toFloat()))
            }
        }
        list.addAll(portions)
        list
    }

    var selectedPortion by remember(selectablePortions) {
        val customPortion = selectablePortions.firstOrNull { it.id >= 0 || (it.name != "Grammi" && it.name != "Gram" && !it.name.startsWith("Porzione") && !it.name.startsWith("Serving")) }
        val servingPortion = selectablePortions.firstOrNull { it.id == -2 }
        mutableStateOf(customPortion ?: servingPortion ?: selectablePortions.first())
    }

    val isGramsPortion = selectedPortion.name == "Grammi" || selectedPortion.name == "Gram"
    var quantity by remember(selectedPortion) {
        mutableFloatStateOf(if (isGramsPortion) 100f else 1f)
    }
    var quantityText by remember(selectedPortion) {
        mutableStateOf(if (isGramsPortion) "100" else "1")
    }

    fun updateQuantity(newVal: Float) {
        quantity = newVal
        quantityText = if (newVal == newVal.toInt().toFloat()) "${newVal.toInt()}" else String.format(Locale.US, "%.2f", newVal)
    }

    val currentFood = food
    
    if (currentFood == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isFoodLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Caricamento alimento…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.foodlab_mascot_duo),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Dettagli alimento non disponibili", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Questo può accadere se un risultato online è scaduto. Torna indietro e cerca di nuovo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNavigateBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("TORNA ALLA RICERCA") }
            }
        }
        return
    }

    val totalGrams = quantity * selectedPortion.weightGrams
    val finalGrams = totalGrams.roundToInt().coerceIn(1, 5_000)
    val nutrition = com.example.util.NutritionCalculator.fromPer100g(
        quantityGrams = finalGrams,
        caloriesPer100g = currentFood.caloriesPer100g,
        proteinPer100g = currentFood.proteinPer100g,
        carbsPer100g = currentFood.carbsPer100g,
        fatPer100g = currentFood.fatPer100g
    )
    val currentCalories = nutrition.calories
    val currentPro = nutrition.protein
    val currentCarbs = nutrition.carbs
    val currentFat = nutrition.fat
    val mealAccent = mealAccentColor(mealType)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FoodLabScreenHeader(
                    title = "Registra alimento",
                    subtitle = "Aggiunta a ${MealTypes.displayName(mealType)}",
                    onBack = onNavigateBack
                )

                val overAfterLog = (currentCalories - remainingCalories).coerceAtLeast(0)
                val calorieAccent = calorieOverTargetColor(overAfterLog, MaterialTheme.colorScheme.primary)
                val servingAccent = if (overAfterLog > 0) calorieAccent else mealAccent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, servingAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(mealAccent, calorieAccent)
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier
                                .background(mealAccent.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                                .border(1.dp, mealAccent.copy(alpha = 0.48f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(mealIcon(mealType), contentDescription = null, tint = mealAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(MealTypes.displayName(mealType).uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = mealAccent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(currentFood.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                if (currentFood.brand.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verificato", tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(currentFood.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = if (overAfterLog > 0) "+$overAfterLog kcal oltre l'obiettivo" else "$remainingCalories kcal disponibili oggi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = calorieAccent
                                )
                                if (currentMealCalories > 0) {
                                    Text(
                                        text = "$currentMealCalories kcal già registrate in ${MealTypes.displayName(mealType).lowercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = mealAccent
                                    )
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(calorieAccent.copy(alpha = if (overAfterLog > 0) 0.18f else 0.14f), RoundedCornerShape(20.dp))
                                    .border(1.dp, calorieAccent.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$currentCalories", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = calorieAccent)
                                    Text("KCAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                MacroPreviewCard(
                    calories = currentCalories,
                    protein = currentPro,
                    carbs = currentCarbs,
                    fat = currentFat,
                    accent = calorieAccent,
                    subtitle = "Valori nutrizionali per la porzione"
                )

                // Serving Size Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(servingAccent.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .border(1.dp, servingAccent.copy(alpha = 0.42f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val isGramPortion = selectedPortion.name == "Grammi" || selectedPortion.name == "Gram"
                        val maxRange = if (isGramPortion) 1000f.coerceAtLeast(quantity) else 20f.coerceAtLeast(quantity)
                        val stepSize = if (isGramPortion) 10f else 0.5f

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("PORZIONE E QUANTITÀ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (isGramPortion) "${quantity.toInt()} G" else "${if (quantity == quantity.toInt().toFloat()) "${quantity.toInt()}" else String.format(Locale.getDefault(), "%.2f", quantity)} x ${selectedPortion.name} (${totalGrams.toInt()}g)",
                                style = MaterialTheme.typography.labelMedium,
                                color = servingAccent
                            )
                        }

                        // Portion selection chips
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectablePortions.size) { index ->
                                val portion = selectablePortions[index]
                                val isSelected = selectedPortion == portion
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) servingAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) servingAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            selectedPortion = portion
                                            soundManager.playRandomSelect()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = portion.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) servingAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Quantity Input Text Field
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { newValue ->
                                val replaced = newValue.replace(',', '.')
                                val sb = StringBuilder()
                                var dotCount = 0
                                for (ch in replaced) {
                                    if (ch.isDigit()) {
                                        sb.append(ch)
                                    } else if (ch == '.' && dotCount == 0) {
                                        sb.append('.')
                                        dotCount++
                                    }
                                }
                                val sanitized = sb.toString().take(6)
                                quantityText = sanitized
                                val parsed = sanitized.toFloatOrNull()?.takeIf { it.isFinite() }
                                if (parsed != null && parsed > 0f) {
                                    quantity = parsed.coerceIn(0.1f, 5000f)
                                } else if (sanitized.isEmpty()) {
                                    quantity = 0f
                                }
                            },
                            label = { Text("Quantità (${if (isGramPortion) "g" else "porzioni"})", style = MaterialTheme.typography.labelSmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Modifica Quantità", tint = servingAccent, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = servingAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Quick Preset Chips
                        val quickPresets = if (isGramPortion) listOf(50f, 80f, 100f, 150f, 200f) else listOf(0.5f, 1f, 1.5f, 2f)
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickPresets.size) { idx ->
                                val p = quickPresets[idx]
                                val isCurrent = quantity == p
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) servingAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) servingAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { updateQuantity(p) }
                                ) {
                                    Text(
                                        text = if (isGramPortion) "${p.toInt()}g" else "${if (p == p.toInt().toFloat()) "${p.toInt()}" else "$p"}x",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) servingAccent else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            if (isGramPortion) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable { updateQuantity((quantity + 25f).coerceAtMost(5000f)) }
                                    ) {
                                        Text(
                                            text = "+25g",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = servingAccent,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable { updateQuantity((quantity + 50f).coerceAtMost(5000f)) }
                                    ) {
                                        Text(
                                            text = "+50g",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = servingAccent,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { 
                                    val minQty = if (isGramPortion) 1f else 0.1f
                                    updateQuantity((quantity - stepSize).coerceAtLeast(minQty))
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = servingAccent)
                            }

                            Slider(
                                value = quantity.coerceIn(0f, maxRange),
                                onValueChange = { raw ->
                                    val snapped = if (isGramPortion) {
                                        ((raw / 5f).roundToInt() * 5f).coerceIn(1f, maxRange)
                                    } else {
                                        ((raw * 2f).roundToInt() / 2f).coerceIn(0.5f, maxRange)
                                    }
                                    updateQuantity(snapped)
                                },
                                valueRange = 0f..maxRange,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = servingAccent,
                                    activeTrackColor = servingAccent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            
                            IconButton(
                                onClick = { 
                                    val maxQty = if (isGramPortion) 5000f else 100f
                                    updateQuantity((quantity + stepSize).coerceAtMost(maxQty))
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = servingAccent)
                            }
                        }

                        // Real-time live macro calculator preview
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, servingAccent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calorie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currentCalories kcal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = calorieAccent)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Proteine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.1f", currentPro)}g", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Carboidrati", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.1f", currentCarbs)}g", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Grassi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.1f", currentFat)}g", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .navigationBarsPadding()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(16.dp)
            ) {
                val ctaOverAfterLog = (currentCalories - remainingCalories).coerceAtLeast(0)
                val ctaColor = calorieOverTargetColor(ctaOverAfterLog, MaterialTheme.colorScheme.primary)
                val ctaContentColor = if (ctaOverAfterLog in 1..240) Color(0xFF16110A) else MaterialTheme.colorScheme.onPrimary
                Button(
                    onClick = { 
                        soundManager.playSound("coin")
                        val finalGrams = (quantity * selectedPortion.weightGrams).roundToInt().coerceIn(1, 5_000)
                        viewModel.addMealEntry(currentFood, finalGrams, mealType, currentDate)
                        android.widget.Toast.makeText(context, "${currentFood.name} registrato in ${MealTypes.displayName(mealType).lowercase()}!", android.widget.Toast.LENGTH_SHORT).show()
                        onMealLogged()
                    },
                    enabled = quantity.isFinite() && totalGrams in 1f..5_000f,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ctaColor, contentColor = ctaContentColor)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Registra")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REGISTRA IN ${MealTypes.displayName(mealType).uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
