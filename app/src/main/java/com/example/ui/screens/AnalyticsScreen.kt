package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.WeightLog
import com.example.data.MealEntry
import com.example.ui.SessionManager
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    db: AppDatabase = AppDatabase.getDatabase(LocalContext.current),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current
    val prefsManager = LocalPreferencesManager.current

    val weights by db.weightLogDao().getAllWeightsDesc().collectAsStateWithLifecycle(initialValue = emptyList())
    val allMeals by db.mealEntryDao().getAllMeals().collectAsStateWithLifecycle(initialValue = emptyList())
    val allTargets by db.dailyTargetDao().getAllTargets().collectAsStateWithLifecycle(initialValue = emptyList())
    val fixedTarget by db.dailyTargetDao().getFixedTarget().collectAsStateWithLifecycle(initialValue = null)

    // Visibility StateFlows
    val showCalorieDeficit by prefsManager.showCalorieDeficit.collectAsStateWithLifecycle()
    val showCalorieChart by prefsManager.showCalorieChart.collectAsStateWithLifecycle()
    val showMacroBreakdown by prefsManager.showMacroBreakdown.collectAsStateWithLifecycle()
    val showProteinChart by prefsManager.showProteinChart.collectAsStateWithLifecycle()
    val showCarbsChart by prefsManager.showCarbsChart.collectAsStateWithLifecycle()
    val showFatChart by prefsManager.showFatChart.collectAsStateWithLifecycle()
    val showWeightChart by prefsManager.showWeightChart.collectAsStateWithLifecycle()
    val showStepsChart by prefsManager.showStepsChart.collectAsStateWithLifecycle()
    val showStreakCard by prefsManager.showStreakCard.collectAsStateWithLifecycle()
    val stepGoal by prefsManager.stepGoal.collectAsStateWithLifecycle()

    var showCustomizationSheet by remember { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }

    val todayStr = remember { SessionManager.formatDate(Date()) }
    val todaySteps: Int = remember(todayStr) { com.example.util.StepCounterStore.savedSteps(context, todayStr) }
    val last7Days = remember(todayStr) {
        (6 downTo 0).map { daysAgo ->
            SessionManager.getPreviousDay(todayStr, daysAgo)
        }
    }

    val defaultTargetCalories = fixedTarget?.targetCalories ?: 2000
    val defaultTargetProtein = fixedTarget?.targetProtein ?: 150
    val defaultTargetCarbs = fixedTarget?.targetCarbs ?: 220
    val defaultTargetFat = fixedTarget?.targetFat ?: 65

    // Compute 7-day intake data
    val dailyIntakes = remember(allMeals, allTargets, last7Days, defaultTargetCalories) {
        val sdf = SimpleDateFormat("EEE", Locale.US)
        last7Days.map { date ->
            val mealsForDate = allMeals.filter { it.dateString == date }
            val cals = mealsForDate.sumOf { it.calories }
            val prot = mealsForDate.fold(0f) { acc, m -> acc + m.protein }
            val carbs = mealsForDate.fold(0f) { acc, m -> acc + m.carbs }
            val fat = mealsForDate.fold(0f) { acc, m -> acc + m.fat }
            val weekdayName = try {
                val calendar = Calendar.getInstance().apply {
                    time = SessionManager.parseDateString(date)
                }
                when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "MONDAY"
                    Calendar.TUESDAY -> "TUESDAY"
                    Calendar.WEDNESDAY -> "WEDNESDAY"
                    Calendar.THURSDAY -> "THURSDAY"
                    Calendar.FRIDAY -> "FRIDAY"
                    Calendar.SATURDAY -> "SATURDAY"
                    Calendar.SUNDAY -> "SUNDAY"
                    else -> "FIXED"
                }
            } catch (e: Exception) {
                "FIXED"
            }
            val target = allTargets.firstOrNull { it.dateString == date }?.targetCalories
                ?: allTargets.firstOrNull { it.dateString == weekdayName }?.targetCalories
                ?: defaultTargetCalories
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

    // Today's macros
    val todayMeals = remember(allMeals, todayStr) {
        allMeals.filter { it.dateString == todayStr }
    }
    val todayProtein = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.protein } }
    val todayCarbs = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.carbs } }
    val todayFat = remember(todayMeals) { todayMeals.fold(0f) { acc, m -> acc + m.fat } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Progressi e Statistiche",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    soundManager.playSound("click")
                    showLogWeightDialog = true
                }) {
                    Icon(Icons.Default.MonitorWeight, contentDescription = "Log Weight", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    soundManager.playSound("click")
                    showCustomizationSheet = true
                }) {
                    Icon(Icons.Default.Tune, contentDescription = "Customize Screen", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Scrollable Cards List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Calorie Deficit Card
            AnimatedVisibility(visible = showCalorieDeficit) {
                CalorieDeficitSummaryCard(dailyIntakes = dailyIntakes)
            }

            // 2. Calorie Bar Chart Card
            AnimatedVisibility(visible = showCalorieChart) {
                CalorieBarChartCard(dailyIntakes = dailyIntakes)
            }

            // 3. Overall Macro Breakdown Card
            AnimatedVisibility(visible = showMacroBreakdown) {
                MacroDistributionCard(
                    dailyIntakes = dailyIntakes,
                    protein = todayProtein,
                    carbs = todayCarbs,
                    fat = todayFat,
                    targetProtein = defaultTargetProtein,
                    targetCarbs = defaultTargetCarbs,
                    targetFat = defaultTargetFat
                )
            }

            // 4. Individual Protein Trend Chart Card (Green)
            AnimatedVisibility(visible = showProteinChart) {
                IndividualMacroTrendCard(
                    title = "Trend Proteine (7 giorni)",
                    macroName = "Proteine",
                    unit = "g",
                    lineColor = Color(0xFF22C55E),
                    targetValue = defaultTargetProtein.toFloat(),
                    dailyData = dailyIntakes.map { it.date to it.protein }
                )
            }

            // 5. Individual Carbs Trend Chart Card (Amber)
            AnimatedVisibility(visible = showCarbsChart) {
                IndividualMacroTrendCard(
                    title = "Trend Carboidrati (7 giorni)",
                    macroName = "Carboidrati",
                    unit = "g",
                    lineColor = Color(0xFFF59E0B),
                    targetValue = defaultTargetCarbs.toFloat(),
                    dailyData = dailyIntakes.map { it.date to it.carbs }
                )
            }

            // 6. Individual Fat Trend Chart Card (Cyan)
            AnimatedVisibility(visible = showFatChart) {
                IndividualMacroTrendCard(
                    title = "Trend Grassi (7 giorni)",
                    macroName = "Grassi",
                    unit = "g",
                    lineColor = Color(0xFF06B6D4),
                    targetValue = defaultTargetFat.toFloat(),
                    dailyData = dailyIntakes.map { it.date to it.fat }
                )
            }

            // 7. Weight Progression Line Chart
            AnimatedVisibility(visible = showWeightChart) {
                WeightProgressionCard(
                    weights = weights,
                    onLogWeightClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager.playSound("click")
                        showLogWeightDialog = true
                    }
                )
            }

            // 8. Activity & Steps Card
            AnimatedVisibility(visible = showStepsChart) {
                ActivityStepsCard(stepGoal = stepGoal, currentSteps = todaySteps)
            }

            // 9. Streak & Mood Journal Grid Card
            AnimatedVisibility(visible = showStreakCard) {
                SlimeCalendarGrid(allMeals = allMeals, targetCalories = defaultTargetCalories)
            }

            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    if (showLogWeightDialog) {
        val currentInitialWeight = formatWeightInput(weights.firstOrNull()?.weightKg)
        LogWeightDialog(
            initialWeight = currentInitialWeight,
            onDismiss = { showLogWeightDialog = false },
            onSave = { weightKg ->
                scope.launch {
                    db.weightLogDao().insertWeight(
                        WeightLog(
                            dateString = todayStr,
                            weightKg = weightKg,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    com.example.util.WidgetUpdater.updateAllWidgets(context)
                }
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
}

private data class DayCalorieData(
    val date: String,
    val intake: Int,
    val target: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val dayLabel: String = ""
)

@Composable
private fun CalorieDeficitSummaryCard(dailyIntakes: List<DayCalorieData>) {
    val totalIntake = dailyIntakes.sumOf { it.intake }
    val totalTarget = dailyIntakes.sumOf { it.target }
    val avgIntake = if (dailyIntakes.isNotEmpty()) totalIntake / dailyIntakes.size else 0
    val avgTarget = if (dailyIntakes.isNotEmpty()) totalTarget / dailyIntakes.size else 0
    val netDifference = totalTarget - totalIntake // Positive = Deficit, Negative = Surplus

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bilancio Energetico (7 Giorni)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (netDifference >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (netDifference >= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (netDifference >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (netDifference >= 0) "Deficit" else "Surplus",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (netDifference >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "Media Assunti", value = "$avgIntake kcal/d", color = MaterialTheme.colorScheme.onSurface)
                MetricColumn(label = "Media Obiettivo", value = "$avgTarget kcal/d", color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricColumn(
                    label = "Saldo 7 Giorni",
                    value = if (netDifference >= 0) "-$netDifference kcal" else "+${-netDifference} kcal",
                    color = if (netDifference >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            val estKgChange = netDifference / 7700f
            Text(
                text = if (netDifference >= 0) {
                    "Variazione stimata massa grassa: ~${String.format(Locale.getDefault(), "%.2f", estKgChange)} kg in 7 giorni"
                } else {
                    "Surplus stimato massa grassa: ~${String.format(Locale.getDefault(), "%.2f", -estKgChange)} kg in 7 giorni"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun CalorieBarChartCard(dailyIntakes: List<DayCalorieData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Calorie ultimi 7 giorni vs Obiettivo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            val textMeasurer = rememberTextMeasurer()
            val primaryColor = MaterialTheme.colorScheme.primary
            val warningColor = Color(0xFFF59E0B)
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomLabelHeight = 24.dp.toPx()
                val topLabelHeight = 20.dp.toPx()
                val chartHeight = canvasHeight - bottomLabelHeight - topLabelHeight

                val maxCalorie = maxOf(
                    2600f,
                    (dailyIntakes.maxOfOrNull { maxOf(it.intake, it.target) }?.toFloat() ?: 2000f) * 1.15f
                )

                val itemCount = dailyIntakes.size.coerceAtLeast(1)
                val stepX = canvasWidth / itemCount
                val barWidth = (stepX * 0.45f).coerceAtMost(36.dp.toPx())

                val avgTarget = if (dailyIntakes.isNotEmpty()) dailyIntakes.map { it.target }.average().toFloat() else 2000f
                val targetY = topLabelHeight + chartHeight * (1f - (avgTarget / maxCalorie))
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                dailyIntakes.forEachIndexed { index, dayData ->
                    val centerX = stepX * index + stepX / 2f
                    val intakeHeight = (dayData.intake / maxCalorie) * chartHeight
                    val barTopY = topLabelHeight + (chartHeight - intakeHeight)

                    val isOverTarget = dayData.intake > dayData.target
                    val barColor = if (isOverTarget) warningColor else primaryColor

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, barTopY),
                        size = Size(barWidth, intakeHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    if (dayData.intake > 0) {
                        val valueLayout = textMeasurer.measure(
                            text = "${dayData.intake}",
                            style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = valueLayout,
                            topLeft = Offset(centerX - valueLayout.size.width / 2f, (barTopY - valueLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f))
                        )
                    }

                    val dayLabel = dayData.dayLabel.ifEmpty { dayData.date.takeLast(2) }

                    val dayLayout = textMeasurer.measure(
                        text = dayLabel,
                        style = TextStyle(fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Medium)
                    )
                    drawText(
                        textLayoutResult = dayLayout,
                        topLeft = Offset(centerX - dayLayout.size.width / 2f, canvasHeight - bottomLabelHeight + 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroDistributionCard(
    dailyIntakes: List<DayCalorieData>,
    protein: Float,
    carbs: Float,
    fat: Float,
    targetProtein: Int,
    targetCarbs: Int,
    targetFat: Int
) {
    val proteinCals = protein * 4f
    val carbsCals = carbs * 4f
    val fatCals = fat * 9f
    val totalCals = (proteinCals + carbsCals + fatCals).coerceAtLeast(1f)

    val proteinPct = (proteinCals / totalCals * 100).toInt()
    val carbsPct = (carbsCals / totalCals * 100).toInt()
    val fatPct = (fatCals / totalCals * 100).toInt()

    val avgProtein = if (dailyIntakes.isNotEmpty()) dailyIntakes.map { it.protein }.average().toFloat() else 0f
    val avgCarbs = if (dailyIntakes.isNotEmpty()) dailyIntakes.map { it.carbs }.average().toFloat() else 0f
    val avgFat = if (dailyIntakes.isNotEmpty()) dailyIntakes.map { it.fat }.average().toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Distribuzione Macro di Oggi",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "100% Totale",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Segregated Cumulated Horizontal Bar (Barre percentuali staccate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val pWeight = proteinPct.toFloat().coerceAtLeast(0.1f)
                val cWeight = carbsPct.toFloat().coerceAtLeast(0.1f)
                val fWeight = fatPct.toFloat().coerceAtLeast(0.1f)

                Box(
                    modifier = Modifier
                        .weight(pWeight)
                        .fillMaxHeight()
                        .background(Color(0xFF22C55E))
                )
                Box(
                    modifier = Modifier
                        .weight(cWeight)
                        .fillMaxHeight()
                        .background(Color(0xFFF59E0B))
                )
                Box(
                    modifier = Modifier
                        .weight(fWeight)
                        .fillMaxHeight()
                        .background(Color(0xFF06B6D4))
                )
            }

            // Percentages legend pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroPillLegend("Proteine", "$proteinPct%", Color(0xFF22C55E))
                MacroPillLegend("Carboidrati", "$carbsPct%", Color(0xFFF59E0B))
                MacroPillLegend("Grassi", "$fatPct%", Color(0xFF06B6D4))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Summary of Daily Averages (Riepilogo delle medie giornaliere)
            Text(
                "Medie giornaliere (7 giorni)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "Media Proteine",
                    value = "${avgProtein.roundToInt()}g / ${targetProtein}g",
                    color = Color(0xFF22C55E)
                )
                MetricColumn(
                    label = "Media Carboidrati",
                    value = "${avgCarbs.roundToInt()}g / ${targetCarbs}g",
                    color = Color(0xFFF59E0B)
                )
                MetricColumn(
                    label = "Media Grassi",
                    value = "${avgFat.roundToInt()}g / ${targetFat}g",
                    color = Color(0xFF06B6D4)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            MacroProgressRow(
                name = "Proteine",
                currentGrams = protein.toInt(),
                targetGrams = targetProtein,
                percentage = proteinPct,
                color = Color(0xFF22C55E)
            )

            MacroProgressRow(
                name = "Carboidrati",
                currentGrams = carbs.toInt(),
                targetGrams = targetCarbs,
                percentage = carbsPct,
                color = Color(0xFFF59E0B)
            )

            MacroProgressRow(
                name = "Grassi",
                currentGrams = fat.toInt(),
                targetGrams = targetFat,
                percentage = fatPct,
                color = Color(0xFF06B6D4)
            )
        }
    }
}

@Composable
private fun MacroPillLegend(name: String, percentage: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$name $percentage",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroProgressRow(
    name: String,
    currentGrams: Int,
    targetGrams: Int,
    percentage: Int,
    color: Color
) {
    val progress = (currentGrams.toFloat() / targetGrams.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$name: ${currentGrams}g / ${targetGrams}g ($percentage% cals)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun IndividualMacroTrendCard(
    title: String,
    macroName: String,
    unit: String,
    lineColor: Color,
    targetValue: Float,
    dailyData: List<Pair<String, Float>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Obiettivo: ${targetValue.toInt()}$unit",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = lineColor
                )
            }

            val textMeasurer = rememberTextMeasurer()
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingX = 20.dp.toPx()
                val paddingTop = 20.dp.toPx()
                val paddingBottom = 24.dp.toPx()
                val chartHeight = canvasHeight - paddingTop - paddingBottom
                val chartWidth = canvasWidth - 2 * paddingX

                val maxVal = maxOf(
                    targetValue * 1.25f,
                    (dailyData.maxOfOrNull { it.second } ?: 100f) * 1.15f
                ).coerceAtLeast(10f)

                val points = dailyData.mapIndexed { idx, (date, valGrams) ->
                    val ratioX = if (dailyData.size > 1) idx.toFloat() / (dailyData.size - 1) else 0.5f
                    val x = paddingX + ratioX * chartWidth
                    val y = paddingTop + chartHeight * (1f - (valGrams / maxVal))
                    Triple(Offset(x, y), date, valGrams)
                }

                // Target reference line
                val targetY = paddingTop + chartHeight * (1f - (targetValue / maxVal))
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                if (points.isNotEmpty()) {
                    // Line Path
                    val linePath = Path().apply {
                        moveTo(points[0].first.x, points[0].first.y)
                        points.forEach { lineTo(it.first.x, it.first.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw points and values
                    points.forEach { (pt, dateStr, valGrams) ->
                        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 2.5f.dp.toPx(), center = pt)

                        val valueText = "${valGrams.toInt()}$unit"
                        val valLayout = textMeasurer.measure(
                            text = valueText,
                            style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = valLayout,
                            topLeft = Offset(pt.x - valLayout.size.width / 2f, (pt.y - valLayout.size.height - 4.dp.toPx()).coerceAtLeast(0f))
                        )

                        val dayLabel = try {
                            val dateObj = SessionManager.parseDateString(dateStr)
                            SimpleDateFormat("EEE", Locale.US).format(dateObj).uppercase()
                        } catch (e: Exception) {
                            dateStr.takeLast(2)
                        }

                        val dayLayout = textMeasurer.measure(
                            text = dayLabel,
                            style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant, fontWeight = FontWeight.Medium)
                        )
                        drawText(
                            textLayoutResult = dayLayout,
                            topLeft = Offset(pt.x - dayLayout.size.width / 2f, canvasHeight - paddingBottom + 4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityStepsCard(
    stepGoal: Int,
    currentSteps: Int = 0
) {
    val estimatedKcal = (currentSteps * 0.04f).toInt().coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Attività e Passi Giornalieri",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn(label = "Obiettivo Passi", value = "${String.format(Locale.getDefault(), "%,d", stepGoal)}", color = MaterialTheme.colorScheme.onSurface)
                MetricColumn(label = "Passi Oggi", value = "${String.format(Locale.getDefault(), "%,d", currentSteps)}", color = Color(0xFFF59E0B))
                MetricColumn(label = "Stima Calorie", value = "~$estimatedKcal kcal", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SlimeCalendarGrid(
    allMeals: List<MealEntry>,
    targetCalories: Int
) {
    val todayStr = remember { SessionManager.formatDate(Date()) }
    val last14Days = remember(todayStr) {
        (13 downTo 0).map { daysAgo ->
            SessionManager.getPreviousDay(todayStr, daysAgo)
        }
    }

    val mealsByDate = remember(allMeals) {
        allMeals.groupBy { it.dateString }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Registro e Serie (14 Giorni)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val loggedDaysCount = last14Days.count { (mealsByDate[it] ?: emptyList()).isNotEmpty() }
                Text(
                    text = if (loggedDaysCount > 0) "🔥 $loggedDaysCount giorni registrati" else "Nessun dato registrato",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (loggedDaysCount > 0) Color(0xFFFEF08A) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last14Days.take(7).forEach { date ->
                    DayStatusCell(date = date, meals = mealsByDate[date] ?: emptyList(), targetCalories = targetCalories)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last14Days.takeLast(7).forEach { date ->
                    DayStatusCell(date = date, meals = mealsByDate[date] ?: emptyList(), targetCalories = targetCalories)
                }
            }
        }
    }
}

@Composable
private fun DayStatusCell(
    date: String,
    meals: List<MealEntry>,
    targetCalories: Int
) {
    val totalCals = meals.sumOf { it.calories }
    val isLogged = totalCals > 0
    val isTargetHit = isLogged && totalCals in (targetCalories * 0.85f).toInt()..(targetCalories * 1.15f).toInt()
    val dayLabel = try {
        val dateObj = SessionManager.parseDateString(date)
        java.text.SimpleDateFormat("dd", Locale.US).format(dateObj)
    } catch (e: Exception) {
        date.takeLast(2)
    }

    val cellColor = when {
        isTargetHit -> Color(0xFF22C55E)
        isLogged -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cellColor.copy(alpha = 0.2f))
                .border(1.dp, cellColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isLogged) cellColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeightProgressionCard(
    weights: List<WeightLog>,
    targetWeightKg: Float = 70.0f,
    onLogWeightClick: () -> Unit
) {
    val sortedWeights = remember(weights) {
        weights.sortedWith(compareBy({ it.dateString }, { it.timestamp }))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Andamento Peso",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    sortedWeights.lastOrNull()?.let { last ->
                        val diff = last.weightKg - targetWeightKg
                        val diffStr = if (diff > 0) "+${String.format(Locale.US, "%.1f", diff)} kg dall'obiettivo"
                                      else "${String.format(Locale.US, "%.1f", diff)} kg dall'obiettivo"
                        Text(
                            text = "Attuale: ${last.weightKg} kg ($diffStr)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onLogWeightClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registra Peso")
                }
            }

            if (sortedWeights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MonitorWeight,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Nessun peso registrato.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onLogWeightClick,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Registra il primo peso")
                        }
                    }
                }
            } else {
                val textMeasurer = rememberTextMeasurer()
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val paddingX = 28.dp.toPx()
                    val paddingTop = 28.dp.toPx()
                    val paddingBottom = 28.dp.toPx()
                    val chartHeight = canvasHeight - paddingTop - paddingBottom
                    val chartWidth = canvasWidth - 2 * paddingX

                    val allWeights = sortedWeights.map { it.weightKg } + targetWeightKg
                    val minW = (allWeights.minOrNull() ?: 70f) - 1.5f
                    val maxW = (allWeights.maxOrNull() ?: 75f) + 1.5f
                    val rangeW = (maxW - minW).coerceAtLeast(0.5f)

                    // Target weight dashed reference line
                    val targetY = paddingTop + chartHeight * (1f - (targetWeightKg - minW) / rangeW)
                    drawLine(
                        color = tertiaryColor.copy(alpha = 0.7f),
                        start = Offset(0f, targetY),
                        end = Offset(canvasWidth, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    val targetLabelLayout = textMeasurer.measure(
                        text = "Obiettivo: ${targetWeightKg}kg",
                        style = TextStyle(fontSize = 10.sp, color = tertiaryColor, fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textLayoutResult = targetLabelLayout,
                        topLeft = Offset(canvasWidth - paddingX - targetLabelLayout.size.width, (targetY - targetLabelLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f))
                    )

                    data class WeightPoint(val offset: Offset, val entry: WeightLog)
                    val points = sortedWeights.mapIndexed { idx, entry ->
                        val ratioX = if (sortedWeights.size > 1) idx.toFloat() / (sortedWeights.size - 1) else 0.5f
                        val x = paddingX + ratioX * chartWidth
                        val y = paddingTop + chartHeight * (1f - (entry.weightKg - minW) / rangeW)
                        WeightPoint(Offset(x, y), entry)
                    }

                    if (points.isNotEmpty()) {
                        val firstPt = points[0].offset
                        val lastPt = points[points.size - 1].offset

                        val fillPath = Path().apply {
                            moveTo(firstPt.x, paddingTop + chartHeight)
                            points.forEach { lineTo(it.offset.x, it.offset.y) }
                            lineTo(lastPt.x, paddingTop + chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.40f), primaryColor.copy(alpha = 0.05f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(firstPt.x, firstPt.y)
                            points.forEach { lineTo(it.offset.x, it.offset.y) }
                        }
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5.dp.toPx())
                        )

                        // Data points with outer halo, solid ring, inner white dot, and text label
                        points.forEach { wp ->
                            val pt = wp.offset
                            val entry = wp.entry
                            // Outer halo
                            drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = pt)
                            // Solid primary dot
                            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = pt)
                            // Inner white contrast dot
                            drawCircle(color = Color.White, radius = 2.5f.dp.toPx(), center = pt)

                            val labelText = "${entry.weightKg} kg"
                            val textLayout = textMeasurer.measure(
                                text = labelText,
                                style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(pt.x - textLayout.size.width / 2f, (pt.y - textLayout.size.height - 6.dp.toPx()).coerceAtLeast(0f))
                            )
                        }

                        val startDateLabel = points[0].entry.dateString.takeLast(5)
                        val endDateLabel = points[points.size - 1].entry.dateString.takeLast(5)

                        val startLayout = textMeasurer.measure(startDateLabel, TextStyle(fontSize = 10.sp, color = onSurfaceVariant))
                        val endLayout = textMeasurer.measure(endDateLabel, TextStyle(fontSize = 10.sp, color = onSurfaceVariant))

                        drawText(startLayout, topLeft = Offset(paddingX, canvasHeight - paddingBottom + 4.dp.toPx()))
                        drawText(endLayout, topLeft = Offset(canvasWidth - paddingX - endLayout.size.width.toFloat(), canvasHeight - paddingBottom + 4.dp.toPx()))
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
    val showCalorieDeficit by prefsManager.showCalorieDeficit.collectAsStateWithLifecycle()
    val showCalorieChart by prefsManager.showCalorieChart.collectAsStateWithLifecycle()
    val showMacroBreakdown by prefsManager.showMacroBreakdown.collectAsStateWithLifecycle()
    val showProteinChart by prefsManager.showProteinChart.collectAsStateWithLifecycle()
    val showCarbsChart by prefsManager.showCarbsChart.collectAsStateWithLifecycle()
    val showFatChart by prefsManager.showFatChart.collectAsStateWithLifecycle()
    val showWeightChart by prefsManager.showWeightChart.collectAsStateWithLifecycle()
    val showStepsChart by prefsManager.showStepsChart.collectAsStateWithLifecycle()
    val showStreakCard by prefsManager.showStreakCard.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Personalizza Dashboard",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Mostra o nascondi le card dei grafici secondo le tue preferenze.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            val items = listOf(
                Triple("Deficit Calorico", "show_calorie_deficit", showCalorieDeficit),
                Triple("Grafico Calorie", "show_calorie_chart", showCalorieChart),
                Triple("Ripartizione Macro", "show_macro_breakdown", showMacroBreakdown),
                Triple("Grafico Proteine", "show_protein_chart", showProteinChart),
                Triple("Grafico Carboidrati", "show_carbs_chart", showCarbsChart),
                Triple("Grafico Grassi", "show_fat_chart", showFatChart),
                Triple("Grafico Peso", "show_weight_chart", showWeightChart),
                Triple("Grafico Passi", "show_steps_chart", showStepsChart),
                Triple("Streak & Obiettivi", "show_streak_card", showStreakCard)
            )

            items.forEach { (label, key, checked) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { prefsManager.setCardVisibility(key, !checked) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = checked,
                        onCheckedChange = { prefsManager.setCardVisibility(key, it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
