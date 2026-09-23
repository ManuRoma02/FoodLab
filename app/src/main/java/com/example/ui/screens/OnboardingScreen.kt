package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.DailyTarget
import com.example.data.WeightLog
import com.example.ui.SessionManager
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.WidgetUpdater
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt

private data class OnboardingGoalOption(
    val key: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = LocalPreferencesManager.current
    val soundManager = LocalSoundManager.current
    val db = remember { AppDatabase.getDatabase(context) }

    var gender by remember { mutableStateOf("MALE") }
    var ageText by remember { mutableStateOf("28") }
    var heightText by remember { mutableStateOf("175") }
    var weightText by remember { mutableStateOf("75.0") }
    var activityLevel by remember { mutableStateOf("MODERATE") }
    var goal by remember { mutableStateOf("LOSE_WEIGHT") }

    val weightKg = weightText.replace(',', '.').toFloatOrNull() ?: 75f
    val heightCm = heightText.replace(',', '.').toFloatOrNull() ?: 175f
    val age = ageText.toIntOrNull() ?: 28

    // Mifflin-St Jeor Formula
    val bmr = if (gender == "MALE") {
        (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5f
    } else {
        (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161f
    }

    val activityMultiplier = when (activityLevel) {
        "SEDENTARY" -> 1.2f
        "LIGHT" -> 1.375f
        "MODERATE" -> 1.55f
        "VERY_ACTIVE" -> 1.725f
        else -> 1.2f
    }

    val tdee = bmr * activityMultiplier

    val targetCalories = when (goal) {
        "LOSE_WEIGHT" -> (tdee * 0.82f).roundToInt().coerceAtLeast(1200)
        "MAINTAIN" -> tdee.roundToInt().coerceAtLeast(1200)
        "GAIN_MUSCLE" -> (tdee * 1.10f).roundToInt().coerceAtLeast(1200)
        else -> tdee.roundToInt()
    }

    val targetProtein = (weightKg * 2.0f).roundToInt().coerceIn(60, 320)
    val fatCalories = targetCalories * 0.25f
    val targetFat = (fatCalories / 9f).roundToInt().coerceIn(30, 160)
    val remainingCaloriesForCarbs = (targetCalories - (targetProtein * 4) - (targetFat * 9)).coerceAtLeast(200)
    val targetCarbs = (remainingCaloriesForCarbs / 4f).roundToInt().coerceAtLeast(50)

    val todayStr = remember { SessionManager.formatDate(Date()) }

    fun saveAndFinish(useCustom: Boolean) {
        soundManager.playSound("coin")
        scope.launch {
            if (useCustom) {
                db.dailyTargetDao().insertTarget(
                    DailyTarget(
                        dateString = "FIXED",
                        targetCalories = targetCalories,
                        targetProtein = targetProtein,
                        targetCarbs = targetCarbs,
                        targetFat = targetFat
                    )
                )
                if (weightKg in 20f..400f) {
                    db.weightLogDao().insertWeight(
                        WeightLog(
                            dateString = todayStr,
                            weightKg = weightKg,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                db.dailyTargetDao().insertTarget(
                    DailyTarget(
                        dateString = "FIXED",
                        targetCalories = 2000,
                        targetProtein = 150,
                        targetCarbs = 220,
                        targetFat = 65
                    )
                )
            }
            preferencesManager.completeOnboarding()
            WidgetUpdater.updateAllWidgets(context)
            onFinishOnboarding()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Apple-style Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Benvenuto in FoodLab",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Personalizziamo il tuo fabbisogno calorico e i macronutrienti per i tuoi obiettivi fisici.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Quick start for users in a rush
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().clickable { saveAndFinish(useCustom = false) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hai fretta?", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("Inizia subito con 2.000 kcal standard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { saveAndFinish(useCustom = false) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Inizia Subito", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // 1. Biological Sex Selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SESSO BIOLOGICO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("MALE" to "Uomo 👨", "FEMALE" to "Donna 👩").forEach { (key, label) ->
                            val isSelected = gender == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { gender = key }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Physical Attributes (Age, Height, Weight)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("PARAMETRI CORPOREI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) ageText = it },
                            label = { Text("Età") },
                            suffix = { Text("anni") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) heightText = it },
                            label = { Text("Altezza") },
                            suffix = { Text("cm") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { if (it.length <= 6) weightText = it },
                        label = { Text("Peso Attuale") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Activity Level
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LIVELLO DI ATTIVITÀ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    val activities = listOf(
                        "SEDENTARY" to ("Sedentario" to "Lavoro da scrivania, poco movimento"),
                        "LIGHT" to ("Leggermente attivo" to "Camminate o 1-2 allenamenti a settimana"),
                        "MODERATE" to ("Moderatamente attivo" to "Sport o palestra 3-5 volte a settimana"),
                        "VERY_ACTIVE" to ("Molto attivo" to "Allenamenti intensi o lavoro fisico pesante")
                    )
                    activities.forEach { (key, info) ->
                        val isSelected = activityLevel == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activityLevel = key }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { activityLevel = key },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(info.first, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    Text(info.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Goal
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("OBIETTIVO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    val goals = listOf(
                        OnboardingGoalOption("LOSE_WEIGHT", "Perdita Peso (Deficit ~18%)", Icons.AutoMirrored.Filled.TrendingDown, Color(0xFF38BDF8)),
                        OnboardingGoalOption("MAINTAIN", "Mantenimento Peso (Pareggio)", Icons.Default.HorizontalRule, Color(0xFF22C55E)),
                        OnboardingGoalOption("GAIN_MUSCLE", "Massa Muscolare (Surplus +10%)", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFFF59E0B))
                    )
                    goals.forEach { item ->
                        val isSelected = goal == item.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) item.color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.dp, item.color) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goal = item.key }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(item.icon, contentDescription = null, tint = item.color)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(item.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // 5. Apple-style Live Target Projection Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "IL TUO PIANO NUTRIZIONALE CALCOLATO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$targetCalories",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KCAL / GIORNO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        OnboardingMacroBadge("PROTEINE", "${targetProtein}g", Color(0xFF22C55E))
                        OnboardingMacroBadge("CARBO", "${targetCarbs}g", Color(0xFFF59E0B))
                        OnboardingMacroBadge("GRASSI", "${targetFat}g", Color(0xFF38BDF8))
                    }
                }
            }

            // 6. Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { saveAndFinish(useCustom = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "INIZIA CON QUESTO OBIETTIVO",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                TextButton(
                    onClick = { saveAndFinish(useCustom = false) }
                ) {
                    Text(
                        "Salta per ora (usa 2.000 kcal standard)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingMacroBadge(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
