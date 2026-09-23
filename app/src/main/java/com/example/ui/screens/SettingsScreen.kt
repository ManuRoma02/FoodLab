package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyTarget
import com.example.ui.SettingsViewModel
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.ReminderScheduler
import com.example.util.ThemeSkin
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToOnboarding: () -> Unit = {}
) {
    val soundManager = LocalSoundManager.current
    val context = LocalContext.current
    val preferencesManager = LocalPreferencesManager.current
    val allTargets by viewModel.allTargets.collectAsStateWithLifecycle()
    var selectedTargetKey by remember { mutableStateOf("FIXED") }

    val currentSelectedTarget = remember(allTargets, selectedTargetKey) {
        allTargets.find { it.dateString == selectedTargetKey } ?: DailyTarget(selectedTargetKey)
    }

    var calories by remember(currentSelectedTarget) { mutableFloatStateOf(currentSelectedTarget.targetCalories.toFloat()) }
    var protein by remember(currentSelectedTarget) { mutableFloatStateOf(currentSelectedTarget.targetProtein.toFloat()) }
    var carbs by remember(currentSelectedTarget) { mutableFloatStateOf(currentSelectedTarget.targetCarbs.toFloat()) }
    var fat by remember(currentSelectedTarget) { mutableFloatStateOf(currentSelectedTarget.targetFat.toFloat()) }

    val stepGoal by preferencesManager.stepGoal.collectAsStateWithLifecycle()
    var steps by remember(stepGoal) { mutableFloatStateOf(stepGoal.toFloat()) }

    var showWipeDialog by remember { mutableStateOf(false) }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Cancellare tutti i dati?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Text(
                    "Questa azione eliminerà definitivamente tutti i pasti salvati, i cibi personalizzati, lo storico del peso e ripristinerà gli obiettivi predefiniti. I dati locali non potranno essere recuperati.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        soundManager.playSound("coin")
                        viewModel.wipeAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Text("ELIMINA DEFINITIVAMENTE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("ANNULLA", style = MaterialTheme.typography.labelMedium)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> preferencesManager.setSyncSteps(granted) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        preferencesManager.setNotificationsEnabled(granted)
        ReminderScheduler.sync(context, granted)
    }

    val activeTheme by preferencesManager.themeSkin.collectAsStateWithLifecycle()
    val syncStepsEnabled by preferencesManager.syncSteps.collectAsStateWithLifecycle()
    val soundEffectsEnabled by preferencesManager.soundEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsStateWithLifecycle()

    // Dynamic Macro Split Calculations
    val proteinCals = protein * 4f
    val carbsCals = carbs * 4f
    val fatCals = fat * 9f
    val totalMacroCals = proteinCals + carbsCals + fatCals
    val totalMacroPct = if (calories > 0) ((totalMacroCals / calories) * 100).roundToInt() else 0
    val proteinPct = if (calories > 0) ((proteinCals / calories) * 100).roundToInt() else 0
    val carbsPct = if (calories > 0) ((carbsCals / calories) * 100).roundToInt() else 0
    val fatPct = if (calories > 0) ((fatCals / calories) * 100).roundToInt() else 0
    val isExact100 = totalMacroPct in 99..101

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FoodLabScreenHeader(
                title = "Impostazioni",
                subtitle = "Obiettivi, tema e privacy"
            )

            // 1. MACRO TARGETS SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "OBIETTIVI NUTRIZIONALI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Apple-style TDEE Recalculation Button
                    Button(
                        onClick = {
                            soundManager.playSound("click")
                            onNavigateToOnboarding()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CALCOLA IL TUO FABBISOGNO (TDEE)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    val options = listOf("FIXED", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                    val dayNamesItalian = mapOf(
                        "FIXED" to "TUTTI",
                        "MONDAY" to "LUN",
                        "TUESDAY" to "MAR",
                        "WEDNESDAY" to "MER",
                        "THURSDAY" to "GIO",
                        "FRIDAY" to "VEN",
                        "SATURDAY" to "SAB",
                        "SUNDAY" to "DOM"
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options.size) { index ->
                            val option = options[index]
                            val isSelected = option == selectedTargetKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { selectedTargetKey = option }
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = dayNamesItalian[option] ?: option.take(3),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Dynamic Macro Breakdown Badge
                    Surface(
                        color = if (isExact100) Color(0xFF15803D).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isExact100) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isExact100) "100% Totale" else "$totalMacroPct% Totale",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "($proteinPct% P • $carbsPct% C • $fatPct% F)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isExact100) {
                                        "Somma macro bilanciata! Le calorie da P, C e F coprono esattamente il 100% dell'obiettivo calorico (${totalMacroCals.roundToInt()} kcal)."
                                    } else if (totalMacroPct > 100) {
                                        "Avviso: Le calorie dei macro (${totalMacroCals.roundToInt()} kcal) superano l'obiettivo impostato (${calories.roundToInt()} kcal)."
                                    } else {
                                        "Avviso: Le calorie dei macro (${totalMacroCals.roundToInt()} kcal) sono inferiori all'obiettivo impostato (${calories.roundToInt()} kcal)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ModernTargetSlider(
                        label = "CALORIE",
                        value = calories,
                        valueRange = 1000f..4000f,
                        stepSize = 50f,
                        accentColor = MaterialTheme.colorScheme.primary,
                        unit = "KCAL",
                        onValueChange = { calories = it }
                    )
                    ModernTargetSlider(
                        label = "PROTEINE",
                        value = protein,
                        valueRange = 50f..300f,
                        stepSize = 5f,
                        accentColor = Color(0xFF22C55E),
                        unit = "g",
                        onValueChange = { protein = it }
                    )
                    ModernTargetSlider(
                        label = "CARBOIDRATI",
                        value = carbs,
                        valueRange = 50f..400f,
                        stepSize = 5f,
                        accentColor = Color(0xFFEAB308),
                        unit = "g",
                        onValueChange = { carbs = it }
                    )
                    ModernTargetSlider(
                        label = "GRASSI",
                        value = fat,
                        valueRange = 20f..200f,
                        stepSize = 5f,
                        accentColor = Color(0xFF06B6D4),
                        unit = "g",
                        onValueChange = { fat = it }
                    )

                    Button(
                        onClick = {
                            soundManager.playSound("click")
                            viewModel.updateDailyTarget(
                                dateString = selectedTargetKey,
                                calories = calories.roundToInt(),
                                protein = protein.roundToInt(),
                                carbs = carbs.roundToInt(),
                                fat = fat.roundToInt()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SALVA OBIETTIVI", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 2. ACTIVITY TARGETS SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "OBIETTIVI ATTIVITÀ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ModernTargetSlider(
                        label = "OBIETTIVO PASSI GIORNALIERO",
                        value = steps,
                        valueRange = 2000f..30000f,
                        stepSize = 500f,
                        accentColor = Color(0xFF8B5CF6),
                        unit = "passi",
                        onValueChange = {
                            steps = it
                            preferencesManager.setStepGoal(it.roundToInt())
                        }
                    )
                }
            }

            // 3. THEME & PREFERENCES SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "TEMA & SUONI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val themeOptions = ThemeSkin.values()
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(themeOptions.size) { index ->
                            val option = themeOptions[index]
                            val isSelected = option == activeTheme
                            val animatedBg by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "themeBg"
                            )
                            val animatedText by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "themeText"
                            )
                            val animatedBorder by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "themeBorder"
                            )
                            Box(
                                modifier = Modifier
                                    .applePressEffect(pressedScale = 0.93f) {
                                        soundManager.playSound("click")
                                        preferencesManager.setThemeSkin(option)
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(animatedBg)
                                    .border(1.dp, animatedBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = animatedText
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    ModernSwitchTile(
                        title = "Effetti sonori",
                        subtitle = "Feedback audio ai tocchi (disattivato di default)",
                        checked = soundEffectsEnabled,
                        onCheckedChange = { preferencesManager.setSoundEnabled(it) }
                    )

                    ModernSwitchTile(
                        title = "Sincronizzazione contapassi",
                        subtitle = "Rileva i passi quotidiani dal sensore",
                        checked = syncStepsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                preferencesManager.setSyncSteps(checked)
                            }
                        }
                    )

                    ModernSwitchTile(
                        title = "Promemoria giornalieri",
                        subtitle = "Notifiche per la registrazione dei pasti",
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                preferencesManager.setNotificationsEnabled(checked)
                                ReminderScheduler.sync(context, checked)
                            }
                        }
                    )
                }
            }

            // 4. PRIVACY & DATA GOVERNANCE SECTION (Privacy by Design / GDPR)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "PRIVACY & DATA GOVERNANCE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Local-first zero telemetry badge (GDPR Art. 13)
                    Surface(
                        color = Color(0xFF22C55E).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "100% Locale & Offline: Tutti i tuoi dati personali (pasti, peso, passi e obiettivi nutrizionali) risiedono esclusivamente nel database SQLite del tuo dispositivo. Zero telemetria cloud o tracciamento di terze parti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Export Data Button (GDPR Art. 20 Data Portability)
                    Button(
                        onClick = {
                            soundManager.playSound("click")
                            viewModel.exportData(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("ESPORTA DATI (CSV / PORTABILITÀ)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // Wipe All Data Button (GDPR Art. 17 Right to Erasure)
                    Button(
                        onClick = {
                            soundManager.playSound("error")
                            showWipeDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("CANCELLA TUTTI I DATI (RESET)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun ModernTargetSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float = 1f,
    accentColor: Color,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.roundToInt().toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = accentColor)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val newValue = (value - stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                        textValue = newValue.roundToInt().toString()
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input
                        val parsed = input.toFloatOrNull()
                        if (parsed != null) {
                            onValueChange(parsed.coerceIn(valueRange.start, valueRange.endInclusive))
                        }
                    },
                    modifier = Modifier
                        .width(72.dp)
                        .height(44.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = {
                        val newValue = (value + stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                        textValue = newValue.roundToInt().toString()
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = {
                textValue = it.roundToInt().toString()
                onValueChange(it)
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun ModernSwitchTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .applePressEffect(pressedScale = 0.98f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onCheckedChange(!checked)
            }
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}

