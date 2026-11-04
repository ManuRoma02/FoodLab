package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MealEntry
import com.example.data.WeightLog
import com.example.ui.AnalyticsViewModel
import com.example.ui.SessionManager
import com.example.ui.theme.*
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.TargetResolver
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private data class DayCalorieData(
    val date: String,
    val intake: Int,
    val target: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val dayLabel: String = ""
)

private enum class ChartMetricMode(val label: String) {
    CALORIES("Calorie"),
    PROTEIN("Proteine"),
    CARBS("Carbo"),
    FAT("Grassi")
}

@Composable
private fun AnalyticsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val cardShape = when {
        isNeo -> RoundedCornerShape(12.dp)
        isBento -> RoundedCornerShape(22.dp)
        else -> RoundedCornerShape(18.dp)
    }
    val cardBorder = when {
        isNeo -> BorderStroke(2.dp, Color.Black)
        isBento -> BorderStroke(1.8.dp, BentoBorder)
        else -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
    val cardBg = when {
        isNeo -> Color.White
        isBento -> BentoSurface
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.5.dp, color = Color.Black, cornerRadius = 12.dp)
                else if (isBento) Modifier.shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    spotColor = BentoCocoa.copy(alpha = 0.12f),
                    ambientColor = BentoCocoa.copy(alpha = 0.08f)
                )
                else Modifier
            ),
        shape = cardShape,
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        content = content
    )
}

@Composable
private fun AppleHighlightTile(
    title: String,
    primaryValue: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val shape = when {
        isNeo -> RoundedCornerShape(10.dp)
        isBento -> RoundedCornerShape(18.dp)
        else -> RoundedCornerShape(14.dp)
    }
    val border = when {
        isNeo -> BorderStroke(1.5.dp, Color.Black)
        isBento -> BorderStroke(1.5.dp, BentoBorder)
        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
    val bg = when {
        isNeo -> Color.White
        isBento -> BentoSurface
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Box(
        modifier = modifier
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 10.dp)
                else if (isBento) Modifier.shadow(elevation = 3.dp, shape = shape, spotColor = BentoCocoa.copy(alpha = 0.12f))
                else Modifier
            )
            .clip(shape)
            .background(bg)
            .border(border.width, border.brush, shape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isNeo || isBento) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = if (isBento) BentoCocoa.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(if (isBento) RoundedCornerShape(99.dp) else RoundedCornerShape(6.dp))
                        .background(if (isBento) BentoSurfaceContainerHigh else iconBg.copy(alpha = 0.18f))
                        .then(if (isBento) Modifier.border(1.dp, BentoBorder, RoundedCornerShape(99.dp)) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isBento) BentoBerryPink else iconBg,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isBento) BentoCocoa.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current
    val prefsManager = LocalPreferencesManager.current
    val isNeo = MaterialTheme.isNeoBrutalist

    val weights by viewModel.weights.collectAsStateWithLifecycle()
    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val allTargets by viewModel.allTargets.collectAsStateWithLifecycle()
    val fixedTarget by viewModel.fixedTarget.collectAsStateWithLifecycle()

    // Visibility toggles
    val showCalorieDeficit by prefsManager.showCalorieDeficit.collectAsStateWithLifecycle()
    val showCalorieChart by prefsManager.showCalorieChart.collectAsStateWithLifecycle()
    val showMacroBreakdown by prefsManager.showMacroBreakdown.collectAsStateWithLifecycle()
    val showWeightChart by prefsManager.showWeightChart.collectAsStateWithLifecycle()
    val showStepsChart by prefsManager.showStepsChart.collectAsStateWithLifecycle()
    val showStreakCard by prefsManager.showStreakCard.collectAsStateWithLifecycle()
    val stepGoal by prefsManager.stepGoal.collectAsStateWithLifecycle()

    var showCustomizationSheet by remember { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }

    // Selected Time Horizon: 7, 14, 30 days
    var selectedHorizonDays by remember { mutableIntStateOf(7) }

    val todayStr = remember { SessionManager.formatDate(Date()) }
    val todaySteps: Int = remember(todayStr) { com.example.util.StepCounterStore.savedSteps(context, todayStr) }

    val horizonDaysList = remember(todayStr, selectedHorizonDays) {
        ((selectedHorizonDays - 1) downTo 0).map { daysAgo ->
            SessionManager.getPreviousDay(todayStr, daysAgo)
        }
    }

    val defaultTargetCalories = fixedTarget?.targetCalories ?: 2000
    val defaultTargetProtein = fixedTarget?.targetProtein ?: 150
    val defaultTargetCarbs = fixedTarget?.targetCarbs ?: 220
    val defaultTargetFat = fixedTarget?.targetFat ?: 65

    // Compute horizon intake data
    val dailyIntakes = remember(allMeals, allTargets, horizonDaysList, defaultTargetCalories) {
        val sdf = SimpleDateFormat("EEE", Locale.US)
        horizonDaysList.map { date ->
            val mealsForDate = allMeals.filter { it.dateString == date }
            val cals = mealsForDate.sumOf { it.calories }
            val prot = mealsForDate.fold(0f) { acc, m -> acc + m.protein }
            val carbs = mealsForDate.fold(0f) { acc, m -> acc + m.carbs }
            val fat = mealsForDate.fold(0f) { acc, m -> acc + m.fat }
            val target = TargetResolver.resolveTargetCalories(date, allTargets, defaultTargetCalories)
            val dayLabel = try {
                val dateObj = SessionManager.parseDateString(date)
                sdf.format(dateObj).uppercase()
            } catch (e: Exception) {
                date.takeLast(2)
            }
            DayCalorieData(
                date = date,
                intake = cals,
                target = target,
                protein = prot,
                carbs = carbs,
                fat = fat,
                dayLabel = dayLabel
            )
        }
    }

    // Filter to only days where the user actually logged meals in this horizon
    val loggedDaysIntakes = remember(dailyIntakes, allMeals) {
        dailyIntakes.filter { day -> allMeals.any { it.dateString == day.date } }
    }

    // Aggregated Metrics (computed ONLY on days with logged meals so unlogged days don't distort averages or balance)
    val totalIntake = loggedDaysIntakes.sumOf { it.intake }
    val totalTarget = loggedDaysIntakes.sumOf { it.target }
    val avgIntake = if (loggedDaysIntakes.isNotEmpty()) totalIntake / loggedDaysIntakes.size else 0
    val avgTarget = if (loggedDaysIntakes.isNotEmpty()) totalTarget / loggedDaysIntakes.size else defaultTargetCalories
    val netDifference = totalTarget - totalIntake // Positive = Deficit, Negative = Surplus
    val avgDifference = if (loggedDaysIntakes.isNotEmpty()) netDifference / loggedDaysIntakes.size else 0

    val avgProtein = if (loggedDaysIntakes.isNotEmpty()) loggedDaysIntakes.map { it.protein }.average().toFloat() else 0f
    val avgCarbs = if (loggedDaysIntakes.isNotEmpty()) loggedDaysIntakes.map { it.carbs }.average().toFloat() else 0f
    val avgFat = if (loggedDaysIntakes.isNotEmpty()) loggedDaysIntakes.map { it.fat }.average().toFloat() else 0f

    // Today's macros
    val todayMeals = remember(allMeals, todayStr) { allMeals.filter { it.dateString == todayStr } }
    val todayProtein = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.protein } }
    val todayCarbs = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.carbs } }
    val todayFat = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.fat } }

    val sortedWeights = remember(weights) {
        weights.sortedWith(compareBy({ it.dateString }, { it.timestamp }))
    }
    val latestWeight = sortedWeights.lastOrNull()?.weightKg ?: 0f

    // Dialog & Sheets
    if (showLogWeightDialog) {
        val initialWeightStr = if (latestWeight > 0f) String.format(Locale.US, "%.1f", latestWeight) else ""
        LogWeightDialog(
            initialWeight = initialWeightStr,
            onDismiss = { showLogWeightDialog = false },
            onSave = { weight ->
                viewModel.insertWeight(weight, todayStr)
                showLogWeightDialog = false
            }
        )
    }

    if (showCustomizationSheet) {
        CustomizationBottomSheet(
            onDismiss = { showCustomizationSheet = false },
            prefsManager = prefsManager
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Unified Header
        FoodLabScreenHeader(
            title = "Statistiche",
            subtitle = "Panoramica andamento e progressi",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            onBack = null,
            trailing = {
                val isBento = MaterialTheme.isBentoCraft
                val buttonShape = when {
                    isNeo -> RoundedCornerShape(8.dp)
                    isBento -> RoundedCornerShape(99.dp)
                    else -> RoundedCornerShape(12.dp)
                }
                val buttonBorder = when {
                    isNeo -> BorderStroke(2.dp, Color.Black)
                    isBento -> BorderStroke(1.5.dp, BentoBorder)
                    else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
                val buttonBg = when {
                    isNeo -> Color.White
                    isBento -> BentoSurfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 8.dp)
                                else if (isBento) Modifier.shadow(elevation = 2.dp, shape = buttonShape, spotColor = BentoCocoa.copy(alpha = 0.12f))
                                else Modifier
                            )
                            .clip(buttonShape)
                            .background(buttonBg)
                            .border(buttonBorder.width, buttonBorder.brush, buttonShape)
                            .applePressEffect(pressedScale = 0.94f) {
                                soundManager.playSound("click")
                                showLogWeightDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = "Registra Peso",
                            tint = if (isBento) BentoBerryPink else AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 8.dp)
                                else if (isBento) Modifier.shadow(elevation = 2.dp, shape = buttonShape, spotColor = BentoCocoa.copy(alpha = 0.12f))
                                else Modifier
                            )
                            .clip(buttonShape)
                            .background(buttonBg)
                            .border(buttonBorder.width, buttonBorder.brush, buttonShape)
                            .applePressEffect(pressedScale = 0.94f) {
                                soundManager.playSound("click")
                                showCustomizationSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Personalizza",
                            tint = if (isBento) BentoCocoa else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val isBento = MaterialTheme.isBentoCraft
            val pickerShape = when {
                isNeo -> RoundedCornerShape(10.dp)
                isBento -> RoundedCornerShape(99.dp)
                else -> RoundedCornerShape(10.dp)
            }
            val pickerBg = if (isBento) BentoSurfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHigh
            val pickerBorder = if (isBento) BorderStroke(1.2.dp, BentoBorder) else null

            // Timeframe Segmented Picker (7G | 14G | 30G)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pickerShape)
                    .background(pickerBg)
                    .then(if (pickerBorder != null) Modifier.border(pickerBorder.width, pickerBorder.brush, pickerShape) else Modifier)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(7 to "7 Giorni", 14 to "14 Giorni", 30 to "30 Giorni").forEach { (days, label) ->
                    val isSelected = selectedHorizonDays == days
                    val pillShape = if (isBento) RoundedCornerShape(99.dp) else RoundedCornerShape(8.dp)
                    val pillBg = if (isSelected) {
                        when {
                            isNeo -> NeoBrutalAmber
                            isBento -> BentoBerryPink
                            else -> MaterialTheme.colorScheme.primary
                        }
                    } else Color.Transparent
                    val pillFg = if (isSelected) {
                        when {
                            isNeo -> Color.Black
                            isBento -> Color.White
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    } else if (isBento) BentoCocoa.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(pillBg)
                            .then(
                                if (isSelected && isNeo) Modifier.border(1.5.dp, Color.Black, pillShape)
                                else Modifier
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                soundManager.playSound("click")
                                selectedHorizonDays = days
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = pillFg
                        )
                    }
                }
            }

            // ==========================================
            // 1. APPLE HIGHLIGHTS BENTO (2x2 Grid)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val calorieDiffLabel = if (loggedDaysIntakes.isEmpty()) {
                        "Nessun dato"
                    } else if (avgDifference != 0) {
                        if (avgDifference > 0) "-$avgDifference vs target" else "+${-avgDifference} vs target"
                    } else "A target"
                    AppleHighlightTile(
                        title = "Media Calorie",
                        primaryValue = if (loggedDaysIntakes.isNotEmpty()) "$avgIntake kcal" else "-- kcal",
                        subtitle = if (loggedDaysIntakes.isNotEmpty()) "Target: $avgTarget ($calorieDiffLabel)" else "Nessun giorno registrato",
                        icon = Icons.Default.LocalFireDepartment,
                        iconBg = AppleOrange,
                        modifier = Modifier.weight(1f)
                    )

                    val netLabel = if (loggedDaysIntakes.isEmpty()) {
                        "-- kcal"
                    } else if (netDifference >= 0) {
                        "-$netDifference kcal"
                    } else {
                        "+${-netDifference} kcal"
                    }
                    val estKg = kotlin.math.abs(netDifference) / 7700f
                    val estKgStr = String.format(Locale.US, "%.1f", estKg)
                    val netSubtitle = when {
                        loggedDaysIntakes.isEmpty() -> "Nessun giorno registrato"
                        netDifference > 0 -> "Deficit (~$estKgStr kg)"
                        netDifference < 0 -> "Surplus (~$estKgStr kg)"
                        else -> "In pareggio"
                    }
                    AppleHighlightTile(
                        title = "Bilancio Periodo",
                        primaryValue = netLabel,
                        subtitle = netSubtitle,
                        icon = if (netDifference >= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        iconBg = if (loggedDaysIntakes.isEmpty()) AppleOrange else (if (netDifference >= 0) Color(0xFF22C55E) else AppleRed),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stepPct = if (stepGoal > 0) ((todaySteps.toFloat() / stepGoal) * 100).roundToInt() else 0
                    AppleHighlightTile(
                        title = "Passi Oggi",
                        primaryValue = "$todaySteps",
                        subtitle = "$stepPct% dell'obiettivo ($stepGoal)",
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        iconBg = ApplePurple,
                        modifier = Modifier.weight(1f)
                    )

                    val weightSubtitle = if (latestWeight > 0f) {
                        "Target: 70.0 kg"
                    } else "Nessun dato registrato"
                    AppleHighlightTile(
                        title = "Peso Attuale",
                        primaryValue = if (latestWeight > 0f) "$latestWeight kg" else "-- kg",
                        subtitle = weightSubtitle,
                        icon = Icons.Default.MonitorWeight,
                        iconBg = AppleBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // 2. UNIFIED INTERACTIVE CHART (Calories & Macros)
            // ==========================================
            AnimatedVisibility(visible = showCalorieChart) {
                UnifiedIntakeExplorerCard(
                    dailyIntakes = dailyIntakes,
                    targetCalories = defaultTargetCalories,
                    targetProtein = defaultTargetProtein,
                    targetCarbs = defaultTargetCarbs,
                    targetFat = defaultTargetFat
                )
            }

            // ==========================================
            // 3. MACRO DISTRIBUTION & DAILY AVERAGES
            // ==========================================
            AnimatedVisibility(visible = showMacroBreakdown) {
                MacroDistributionCard(
                    dailyIntakes = dailyIntakes,
                    todayProtein = todayProtein,
                    todayCarbs = todayCarbs,
                    todayFat = todayFat,
                    targetProtein = defaultTargetProtein,
                    targetCarbs = defaultTargetCarbs,
                    targetFat = defaultTargetFat
                )
            }

            // ==========================================
            // 4. MEAL CONSISTENCY & STREAK
            // ==========================================
            AnimatedVisibility(visible = showStreakCard) {
                ConsistencyStreakCard(
                    allMeals = allMeals,
                    targetCalories = defaultTargetCalories
                )
            }

            // ==========================================
            // 5. WEIGHT PROGRESSION (Apple Health Curve)
            // ==========================================
            AnimatedVisibility(visible = showWeightChart) {
                AppleWeightProgressionCard(
                    weights = weights,
                    targetWeightKg = 70.0f,
                    onLogWeightClick = { showLogWeightDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun UnifiedIntakeExplorerCard(
    dailyIntakes: List<DayCalorieData>,
    targetCalories: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFat: Int
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    var selectedMetric by remember { mutableStateOf(ChartMetricMode.CALORIES) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    AnalyticsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Andamento Nutrizionale",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Tocca una barra per i dettagli",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sub-Metric Segmented Selector (Calorie | Proteine | Carbo | Grassi) - Fully Responsive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ChartMetricMode.values().forEach { mode ->
                    val isSel = mode == selectedMetric
                    val bg = if (isSel) {
                        if (isNeo) NeoBrutalAmber else MaterialTheme.colorScheme.primary
                    } else Color.Transparent
                    val fg = if (isSel) {
                        if (isNeo) Color.Black else MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .then(
                                if (isSel && isNeo) Modifier.border(1.5.dp, Color.Black, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable {
                                selectedMetric = mode
                                selectedDayIndex = null
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = fg,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Target & Selected Day Info Banner
            val inspectedDay = selectedDayIndex?.let { dailyIntakes.getOrNull(it) } ?: dailyIntakes.lastOrNull()
            val (currentVal, targetVal, unit, metricColor) = when (selectedMetric) {
                ChartMetricMode.CALORIES -> Quad(
                    inspectedDay?.intake?.toFloat() ?: 0f,
                    (inspectedDay?.target ?: targetCalories).toFloat(),
                    "kcal",
                    AppleOrange
                )
                ChartMetricMode.PROTEIN -> Quad(
                    inspectedDay?.protein ?: 0f,
                    targetProtein.toFloat(),
                    "g",
                    MaterialTheme.macroColors.protein
                )
                ChartMetricMode.CARBS -> Quad(
                    inspectedDay?.carbs ?: 0f,
                    targetCarbs.toFloat(),
                    "g",
                    MaterialTheme.macroColors.carbs
                )
                ChartMetricMode.FAT -> Quad(
                    inspectedDay?.fat ?: 0f,
                    targetFat.toFloat(),
                    "g",
                    MaterialTheme.macroColors.fat
                )
            }

            Surface(
                color = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(10.dp),
                border = if (isNeo) BorderStroke(1.5.dp, Color.Black) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = inspectedDay?.let { "${it.dayLabel} · ${it.date.takeLast(5)}" } ?: "Oggi",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val delta = currentVal - targetVal
                        val deltaStr = if (delta >= 0) "+${delta.roundToInt()}" else "${delta.roundToInt()}"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${currentVal.roundToInt()} $unit",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = metricColor
                            )
                            Text(
                                text = "($deltaStr vs target)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Target ${targetVal.roundToInt()} $unit",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Interactive Bar Chart Canvas
            val textMeasurer = rememberTextMeasurer()
            val onSurface = MaterialTheme.colorScheme.onSurface
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            val itemCount = dailyIntakes.size.coerceAtLeast(1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(dailyIntakes, selectedMetric) {
                        detectTapGestures { offset ->
                            val stepX = size.width / itemCount
                            val idx = (offset.x / stepX).toInt().coerceIn(0, itemCount - 1)
                            selectedDayIndex = idx
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomLabelHeight = 22.dp.toPx()
                val topLabelHeight = 16.dp.toPx()
                val chartHeight = canvasHeight - bottomLabelHeight - topLabelHeight

                val maxVal = maxOf(
                    targetVal * 1.25f,
                    (dailyIntakes.maxOfOrNull {
                        when (selectedMetric) {
                            ChartMetricMode.CALORIES -> maxOf(it.intake.toFloat(), it.target.toFloat())
                            ChartMetricMode.PROTEIN -> it.protein
                            ChartMetricMode.CARBS -> it.carbs
                            ChartMetricMode.FAT -> it.fat
                        }
                    } ?: targetVal) * 1.15f
                ).coerceAtLeast(10f)

                val stepX = canvasWidth / itemCount
                val barWidth = (stepX * 0.50f).coerceIn(12.dp.toPx(), 32.dp.toPx())

                // Dashed target reference line
                val targetY = topLabelHeight + chartHeight * (1f - (targetVal / maxVal))
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                dailyIntakes.forEachIndexed { index, dayData ->
                    val centerX = stepX * index + stepX / 2f
                    val dayValue = when (selectedMetric) {
                        ChartMetricMode.CALORIES -> dayData.intake.toFloat()
                        ChartMetricMode.PROTEIN -> dayData.protein
                        ChartMetricMode.CARBS -> dayData.carbs
                        ChartMetricMode.FAT -> dayData.fat
                    }

                    val isSelected = selectedDayIndex == index

                    // Background track bar (Apple Health style)
                    val trackColor = if (isSelected) {
                        onSurfaceVariant.copy(alpha = 0.22f)
                    } else {
                        onSurfaceVariant.copy(alpha = 0.08f)
                    }
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(centerX - barWidth / 2f, topLabelHeight),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )

                    if (dayValue > 0f) {
                        val barHeight = ((dayValue / maxVal) * chartHeight).coerceIn(4.dp.toPx(), chartHeight)
                        val barTopY = topLabelHeight + (chartHeight - barHeight)

                        val barColor = if (isSelected) {
                            if (isNeo) NeoBrutalAmber else metricColor
                        } else {
                            metricColor.copy(alpha = 0.85f)
                        }

                        // Bar
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(centerX - barWidth / 2f, barTopY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )

                        if (isNeo) {
                            drawRoundRect(
                                color = Color.Black,
                                topLeft = Offset(centerX - barWidth / 2f, barTopY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    } else {
                        // Subtle baseline indicator when no intake
                        drawRoundRect(
                            color = onSurfaceVariant.copy(alpha = 0.20f),
                            topLeft = Offset(centerX - barWidth / 2f, topLabelHeight + chartHeight - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    // Day label
                    val dayLabel = dayData.dayLabel.ifEmpty { dayData.date.takeLast(2) }
                    val labelLayout = textMeasurer.measure(
                        text = dayLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = if (isSelected) onSurface else onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(centerX - labelLayout.size.width / 2f, canvasHeight - bottomLabelHeight + 4.dp.toPx())
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun MacroDistributionCard(
    dailyIntakes: List<DayCalorieData>,
    todayProtein: Float,
    todayCarbs: Float,
    todayFat: Float,
    targetProtein: Int,
    targetCarbs: Int,
    targetFat: Int
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val proteinCals = todayProtein * 4f
    val carbsCals = todayCarbs * 4f
    val fatCals = todayFat * 9f
    val totalCals = (proteinCals + carbsCals + fatCals).coerceAtLeast(1f)

    val proteinPct = (proteinCals / totalCals * 100).toInt()
    val carbsPct = (carbsCals / totalCals * 100).toInt()
    val fatPct = (fatCals / totalCals * 100).toInt()

    val loggedDays = remember(dailyIntakes) { dailyIntakes.filter { it.intake > 0 } }
    val avgProtein = if (loggedDays.isNotEmpty()) loggedDays.map { it.protein }.average().toFloat() else 0f
    val avgCarbs = if (loggedDays.isNotEmpty()) loggedDays.map { it.carbs }.average().toFloat() else 0f
    val avgFat = if (loggedDays.isNotEmpty()) loggedDays.map { it.fat }.average().toFloat() else 0f

    AnalyticsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Distribuzione Macronutrienti",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Oggi vs Media",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Proportional Ratio Bar
            if (todayProtein > 0f || todayCarbs > 0f || todayFat > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(carbsPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.carbs))
                    Box(modifier = Modifier.weight(proteinPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.protein))
                    Box(modifier = Modifier.weight(fatPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.fat))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun pasto registrato oggi",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3 Macro Detail Columns - Equal Responsive Width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroStatColumn(
                    name = "Carboidrati",
                    pct = "$carbsPct%",
                    today = "${todayCarbs.roundToInt()}g",
                    avg = "Media: ${avgCarbs.roundToInt()}g",
                    target = "Target: ${targetCarbs}g",
                    color = MaterialTheme.macroColors.carbs,
                    modifier = Modifier.weight(1f)
                )
                MacroStatColumn(
                    name = "Proteine",
                    pct = "$proteinPct%",
                    today = "${todayProtein.roundToInt()}g",
                    avg = "Media: ${avgProtein.roundToInt()}g",
                    target = "Target: ${targetProtein}g",
                    color = MaterialTheme.macroColors.protein,
                    modifier = Modifier.weight(1f)
                )
                MacroStatColumn(
                    name = "Grassi",
                    pct = "$fatPct%",
                    today = "${todayFat.roundToInt()}g",
                    avg = "Media: ${avgFat.roundToInt()}g",
                    target = "Target: ${targetFat}g",
                    color = MaterialTheme.macroColors.fat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroStatColumn(
    name: String,
    pct: String,
    today: String,
    avg: String,
    target: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            today,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
        Text(
            pct,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            avg,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ConsistencyStreakCard(
    allMeals: List<MealEntry>,
    targetCalories: Int
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val today = remember { SessionManager.formatDate(Date()) }
    val last7Days = remember(today) {
        (6 downTo 0).map { SessionManager.getPreviousDay(today, it) }
    }

    val daysWithMeals = remember(allMeals, last7Days) {
        last7Days.map { date ->
            val count = allMeals.count { it.dateString == date }
            date to count
        }
    }
    val loggedCount = daysWithMeals.count { it.second > 0 }

    AnalyticsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Costanza Pasti Settimanale",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$loggedCount su 7 giorni registrati",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (loggedCount >= 5) Color(0xFF22C55E).copy(alpha = 0.15f) else AppleOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (loggedCount >= 5) "🔥 In Costanza" else "⚡ Da Migliorare",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (loggedCount >= 5) Color(0xFF22C55E) else AppleOrange
                        )
                    }
                }
            }

            // 7 Days Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sdf = SimpleDateFormat("EEE", Locale.US)
                daysWithMeals.forEach { (date, mealCount) ->
                    val isLogged = mealCount > 0
                    val dayLabel = try {
                        val d = SessionManager.parseDateString(date)
                        sdf.format(d).uppercase().take(2)
                    } catch (e: Exception) {
                        date.takeLast(2)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLogged) Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .then(
                                    if (isNeo) Modifier.border(1.5.dp, Color.Black, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLogged) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("-", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleWeightProgressionCard(
    weights: List<WeightLog>,
    targetWeightKg: Float = 70.0f,
    onLogWeightClick: () -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val sortedWeights = remember(weights) {
        weights.sortedWith(compareBy({ it.dateString }, { it.timestamp }))
    }

    AnalyticsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Progressione Peso Corporeo",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    sortedWeights.lastOrNull()?.let { last ->
                        val diff = last.weightKg - targetWeightKg
                        val diffStr = if (diff > 0) "+%.1f kg vs target".format(diff) else "%.1f kg vs target".format(diff)
                        Text(
                            text = "Ultimo: ${last.weightKg} kg ($diffStr)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleBlue
                        )
                    }
                }

                Button(
                    onClick = onLogWeightClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNeo) NeoBrutalAmber else AppleBlue,
                        contentColor = if (isNeo) Color.Black else Color.White
                    ),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registra", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (sortedWeights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nessuna pesata registrata. Premi '+ Registra' per iniziare.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val textMeasurer = rememberTextMeasurer()
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val paddingX = 24.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 20.dp.toPx()
                    val chartHeight = canvasHeight - paddingTop - paddingBottom
                    val chartWidth = canvasWidth - 2 * paddingX

                    val allWeights = sortedWeights.map { it.weightKg } + targetWeightKg
                    val minW = (allWeights.minOrNull() ?: 70f) - 1.5f
                    val maxW = (allWeights.maxOrNull() ?: 75f) + 1.5f
                    val rangeW = (maxW - minW).coerceAtLeast(0.5f)

                    // Target weight line
                    val targetY = paddingTop + chartHeight * (1f - (targetWeightKg - minW) / rangeW)
                    drawLine(
                        color = AppleOrange.copy(alpha = 0.6f),
                        start = Offset(0f, targetY),
                        end = Offset(canvasWidth, targetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    val points = sortedWeights.mapIndexed { idx, entry ->
                        val ratioX = if (sortedWeights.size > 1) idx.toFloat() / (sortedWeights.size - 1) else 0.5f
                        val x = paddingX + ratioX * chartWidth
                        val y = paddingTop + chartHeight * (1f - (entry.weightKg - minW) / rangeW)
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
                        val firstPt = points.first()
                        val lastPt = points.last()

                        // Gradient fill under curve
                        val fillPath = Path().apply {
                            moveTo(firstPt.x, paddingTop + chartHeight)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(lastPt.x, paddingTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(AppleBlue.copy(alpha = 0.35f), AppleBlue.copy(alpha = 0.03f)),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Line stroke
                        val linePath = Path().apply {
                            moveTo(firstPt.x, firstPt.y)
                            points.forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = linePath,
                            color = AppleBlue,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Data points
                        points.forEachIndexed { i, pt ->
                            drawCircle(color = AppleBlue, radius = 4.5.dp.toPx(), center = pt)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)

                            // Show label for first, last, or every few
                            if (i == 0 || i == points.size - 1 || points.size <= 5) {
                                val label = "${sortedWeights[i].weightKg}"
                                val l = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant))
                                drawText(l, topLeft = Offset(pt.x - l.size.width / 2f, (pt.y - l.size.height - 4.dp.toPx()).coerceAtLeast(0f)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizationBottomSheet(
    onDismiss: () -> Unit,
    prefsManager: com.example.util.PreferencesManager
) {
    val showCalorieChart by prefsManager.showCalorieChart.collectAsStateWithLifecycle()
    val showMacroBreakdown by prefsManager.showMacroBreakdown.collectAsStateWithLifecycle()
    val showWeightChart by prefsManager.showWeightChart.collectAsStateWithLifecycle()
    val showStreakCard by prefsManager.showStreakCard.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Personalizza Statistiche",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Attiva o disattiva i moduli visibili nella schermata.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            val items = listOf(
                Triple("Grafico Nutrizionale", "show_calorie_chart", showCalorieChart),
                Triple("Ripartizione Macro", "show_macro_breakdown", showMacroBreakdown),
                Triple("Progressione Peso", "show_weight_chart", showWeightChart),
                Triple("Costanza & Pasti", "show_streak_card", showStreakCard)
            )

            items.forEach { (label, key, checked) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { prefsManager.setCardVisibility(key, !checked) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Switch(
                        checked = checked,
                        onCheckedChange = { prefsManager.setCardVisibility(key, it) },
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
