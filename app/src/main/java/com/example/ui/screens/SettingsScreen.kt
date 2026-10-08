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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import com.example.ui.theme.macroColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import kotlinx.coroutines.launch
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

    val hcManager = remember { (context.applicationContext as com.example.FoodLabApplication).healthConnectManager }
    val isHcAuthorized by hcManager.isAuthorized.collectAsStateWithLifecycle()
    val hcDailyData by hcManager.dailyData.collectAsStateWithLifecycle()
    val hcAvailability = remember { hcManager.checkAvailability() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        scope.launch {
            val isAuthorized = hcManager.checkPermissionsGranted()
            if (isAuthorized || granted.isNotEmpty()) {
                hcManager.syncDailyData()
                Toast.makeText(context, "Google Health Connect connesso!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permessi Health Connect non concessi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    hcManager.checkPermissionsGranted()
                    if (hcManager.isAuthorized.value) {
                        hcManager.syncDailyData()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FoodLabScreenHeader(
                title = "Impostazioni",
                subtitle = "Obiettivi, tema e privacy"
            )

            // 1. MACRO TARGETS SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "OBIETTIVI NUTRIZIONALI",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CALCOLA IL TUO FABBISOGNO (TDEE)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(options.size) { index ->
                            val option = options[index]
                            val isSelected = option == selectedTargetKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { selectedTargetKey = option }
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = dayNamesItalian[option] ?: option.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Dynamic Macro Breakdown Badge
                    Surface(
                        color = if (isExact100) Color(0xFF15803D).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExact100) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isExact100) "100% Totale" else "$totalMacroPct% Totale",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isExact100) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "($carbsPct% C • $proteinPct% P • $fatPct% F)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isExact100) {
                                        "Somma macro bilanciata! Le calorie da C, P e F coprono esattamente il 100% dell'obiettivo calorico (${totalMacroCals.roundToInt()} kcal)."
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
                        label = "CARBOIDRATI",
                        value = carbs,
                        valueRange = 50f..400f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.carbs,
                        unit = "g",
                        onValueChange = { carbs = it }
                    )
                    ModernTargetSlider(
                        label = "PROTEINE",
                        value = protein,
                        valueRange = 50f..300f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.protein,
                        unit = "g",
                        onValueChange = { protein = it }
                    )
                    ModernTargetSlider(
                        label = "GRASSI",
                        value = fat,
                        valueRange = 20f..200f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.fat,
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SALVA OBIETTIVI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 2. ACTIVITY TARGETS SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "OBIETTIVI ATTIVITÀ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
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

            // 2b. GOOGLE HEALTH CONNECT INTEGRATION SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "GOOGLE HEALTH CONNECT",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Status Badge
                        val (statusText, statusBg, statusFg) = when {
                            hcAvailability != com.example.util.HealthConnectAvailability.AVAILABLE ->
                                Triple("NON DISPONIBILE", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
                            isHcAuthorized ->
                                Triple("CONNESSO", Color(0xFF22C55E).copy(alpha = 0.15f), Color(0xFF22C55E))
                            else ->
                                Triple("DISCONNESSO", MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.outline)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusFg
                            )
                        }
                    }

                    Text(
                        text = "Sincronizza passi, calorie attive/totali e peso corporeo da smartwatch, Google Fit, Fitbit o Samsung Health.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when {
                        hcAvailability == com.example.util.HealthConnectAvailability.UPDATE_REQUIRED -> {
                            Button(
                                onClick = {
                                    soundManager.playSound("click")
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Impossibile aprire Play Store", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("AGGIORNA HEALTH CONNECT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        hcAvailability == com.example.util.HealthConnectAvailability.NOT_SUPPORTED -> {
                            Text(
                                "Health Connect non è supportato su questo dispositivo (richiede Android 9+).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        isHcAuthorized -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Passi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${hcDailyData.steps}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calorie bruciate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${hcDailyData.caloriesBurned.roundToInt()} kcal",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (hcDailyData.latestWeightKg != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Ultimo Peso", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "%.1f kg".format(hcDailyData.latestWeightKg),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    soundManager.playSound("click")
                                    scope.launch {
                                        val res = hcManager.syncDailyData()
                                        if (res.isSuccess) {
                                            val data = res.getOrThrow()
                                            val todaySteps = com.example.util.StepCounterStore.savedSteps(context)
                                            if (data.steps.toInt() > todaySteps) {
                                                com.example.util.StepCounterStore.setSteps(context, data.steps.toInt())
                                            }
                                            com.example.util.WidgetUpdater.updateAllWidgets(context)
                                            Toast.makeText(context, "Sincronizzazione completata!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Errore durante la sincronizzazione", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("SINCRONIZZA ORA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            TextButton(
                                onClick = {
                                    soundManager.playSound("click")
                                    hcManager.openSettings(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Gestisci permessi Health Connect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    soundManager.playSound("click")
                                    try {
                                        hcPermissionLauncher.launch(hcManager.permissions)
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsScreen", "Error launching Health Connect permission dialog", e)
                                        hcManager.openSettings(context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("CONNETTI HEALTH CONNECT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            TextButton(
                                onClick = {
                                    soundManager.playSound("click")
                                    hcManager.openSettings(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Apri impostazioni Health Connect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 3. THEME & PREFERENCES SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "TEMA & SUONI",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val themeOptions = ThemeSkin.values()
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(animatedBg)
                                    .border(1.dp, animatedBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(com.example.ui.theme.themePreviewColor(option))
                                    )
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = animatedText
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    ModernSwitchTile(
                        title = "Effetti sonori",
                        subtitle = "Feedback audio ai tocchi",
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
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "PRIVACY & DATA GOVERNANCE",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Local-first zero telemetry badge (GDPR Art. 13)
                    Surface(
                        color = MaterialTheme.macroColors.protein.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.macroColors.protein.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.macroColors.protein,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "100% Locale & Offline: Tutti i tuoi dati personali risiedono esclusivamente nel database SQLite del tuo dispositivo. Zero telemetria cloud o tracciamento.",
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("ESPORTA DATI (CSV / PORTABILITÀ)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("CANCELLA TUTTI I DATI (RESET)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(112.dp))
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
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Compact Apple-style Stepper Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .height(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            val newValue = (value - stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                            textValue = newValue.roundToInt().toString()
                            onValueChange(newValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }

                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            textValue = input
                            val parsed = input.toFloatOrNull()
                            if (parsed != null) {
                                onValueChange(parsed.coerceIn(valueRange.start, valueRange.endInclusive))
                            }
                        },
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                        modifier = Modifier.width(42.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            val newValue = (value + stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                            textValue = newValue.roundToInt().toString()
                            onValueChange(newValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = {
                textValue = it.roundToInt().toString()
                onValueChange(it)
            },
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onCheckedChange(it)
            },
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}

