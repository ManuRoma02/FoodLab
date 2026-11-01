package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyTarget
import com.example.ui.SettingsViewModel
import com.example.ui.foundation.appleSpring
import com.example.ui.theme.*
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.NotificationScheduler
import com.example.util.ThemeSkin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
private fun IosSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (isNeo) FontWeight.Black else FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            fontSize = 11.sp
        ),
        color = if (isNeo) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(start = 6.dp, bottom = 4.dp, top = 6.dp)
    )
}

@Composable
private fun IosGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val shape = if (isNeo) RoundedCornerShape(12.dp) else RoundedCornerShape(18.dp)
    val border = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    val bg = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.5.dp, color = Color.Black, cornerRadius = 12.dp)
                else Modifier
            ),
        shape = shape,
        border = border,
        colors = CardDefaults.cardColors(containerColor = bg),
        content = content
    )
}

@Composable
private fun IosIconTile(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val shape = if (isNeo) RoundedCornerShape(6.dp) else RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (isNeo) Modifier.border(1.5.dp, Color.Black, shape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isNeo && backgroundColor == Color(0xFFFFE500)) Color.Black else iconTint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun IosSettingsRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val haptic = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current

    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .applePressEffect(pressedScale = 0.98f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onClick()
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onClick()
            }
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            IosIconTile(icon = icon, backgroundColor = iconBg)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isNeo) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        trailing()
    }
}

@Composable
private fun IosRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        thickness = 0.5.dp
    )
}

@Composable
private fun IosTargetControlRow(
    label: String,
    value: Float,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float = 1f,
    accentColor: Color,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    var textValue by remember(value) { mutableStateOf(value.roundToInt().toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IosIconTile(icon = icon, backgroundColor = accentColor)
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isNeo) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${value.roundToInt()} $unit",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNeo) Color.Black else accentColor
                        )
                    )
                }
            }

            // Stepper Box (- [Valore] +)
            val stepperShape = if (isNeo) RoundedCornerShape(6.dp) else RoundedCornerShape(10.dp)
            val stepperBorder = if (isNeo) BorderStroke(1.5.dp, Color.Black) else BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            val stepperBg = if (isNeo) Color(0xFFF7F5ED) else MaterialTheme.colorScheme.surfaceContainerHigh

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(stepperShape)
                    .background(stepperBg)
                    .border(stepperBorder.width, stepperBorder.brush, stepperShape)
                    .height(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            val next = (value - stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                            textValue = next.roundToInt().toString()
                            onValueChange(next)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }

                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
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
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            val next = (value + stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                            textValue = next.roundToInt().toString()
                            onValueChange(next)
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
                .height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = if (isNeo) Color.Black else accentColor,
                activeTrackColor = if (isNeo) accentColor else accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToOnboarding: () -> Unit = {}
) {
    val soundManager = LocalSoundManager.current
    val context = LocalContext.current
    val preferencesManager = LocalPreferencesManager.current
    val haptic = LocalHapticFeedback.current
    val isNeo = MaterialTheme.isNeoBrutalist

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
    val scope = rememberCoroutineScope()

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
    DisposableEffect(lifecycleOwner) {
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showWipeDialog by remember { mutableStateOf(false) }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Cancellare tutti i dati?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Text(
                    "Questa azione eliminerà definitivamente tutti i pasti registrati, i cibi personalizzati, lo storico peso e ripristinerà gli obiettivi predefiniti. Non è possibile annullare.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ELIMINA ORA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("ANNULLA", style = MaterialTheme.typography.labelMedium)
                }
            },
            shape = RoundedCornerShape(16.dp),
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
        NotificationScheduler.scheduleReminders(context, granted)
    }

    val activeTheme by preferencesManager.themeSkin.collectAsStateWithLifecycle()
    val syncStepsEnabled by preferencesManager.syncSteps.collectAsStateWithLifecycle()
    val soundEffectsEnabled by preferencesManager.soundEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsStateWithLifecycle()

    // Dynamic Macro Split
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FoodLabScreenHeader(
                title = "Impostazioni",
                subtitle = "Obiettivi, tema e privacy"
            )

            // ==========================================
            // 1. OBIETTIVI NUTRIZIONALI (Apple Inset Group)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosSectionHeader(title = "Obiettivi Nutrizionali")

                IosGroupCard {
                    // TDEE Wizard Row
                    IosSettingsRow(
                        icon = Icons.Default.Calculate,
                        iconBg = AppleBlue,
                        title = "Calcolo Fabbisogno (TDEE)",
                        subtitle = "Ricalcola calorie e macro con la guida",
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToOnboarding
                    )

                    IosRowDivider()

                    // Day of Week Selector
                    val dayOptions = listOf("FIXED", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                    val dayLabels = mapOf(
                        "FIXED" to "Tutti",
                        "MONDAY" to "Lun",
                        "TUESDAY" to "Mar",
                        "WEDNESDAY" to "Mer",
                        "THURSDAY" to "Gio",
                        "FRIDAY" to "Ven",
                        "SATURDAY" to "Sab",
                        "SUNDAY" to "Dom"
                    )

                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "GIORNO OBIETTIVO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dayOptions.size) { idx ->
                                val opt = dayOptions[idx]
                                val selected = opt == selectedTargetKey
                                val chipShape = if (isNeo) RoundedCornerShape(6.dp) else RoundedCornerShape(8.dp)
                                val chipBg = if (isNeo) {
                                    if (selected) Color(0xFFFFE500) else Color(0xFFF7F5ED)
                                } else {
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                val chipText = if (isNeo) Color.Black else (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

                                Box(
                                    modifier = Modifier
                                        .clip(chipShape)
                                        .background(chipBg)
                                        .then(
                                            if (isNeo) Modifier.border(1.5.dp, Color.Black, chipShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedTargetKey = opt
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabels[opt] ?: opt,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = chipText
                                    )
                                }
                            }
                        }
                    }

                    IosRowDivider()

                    // Macro Distribution Ratio Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (isExact100) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isExact100) Color(0xFF22C55E) else AppleOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isExact100) "Bilancio 100% (${calories.roundToInt()} kcal)" else "Macro: $totalMacroPct% (${totalMacroCals.roundToInt()}/${calories.roundToInt()} kcal)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isExact100) Color(0xFF22C55E) else AppleOrange
                                )
                            }
                            Text(
                                text = "${carbsPct}% C • ${proteinPct}% P • ${fatPct}% F",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Proportional Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.weight(carbsPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.carbs))
                            Box(modifier = Modifier.weight(proteinPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.protein))
                            Box(modifier = Modifier.weight(fatPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(MaterialTheme.macroColors.fat))
                        }
                    }

                    IosRowDivider()

                    // Target Sliders
                    IosTargetControlRow(
                        label = "Calorie Giornaliere",
                        value = calories,
                        unit = "kcal",
                        valueRange = 1000f..4500f,
                        stepSize = 50f,
                        accentColor = AppleOrange,
                        icon = Icons.Default.LocalFireDepartment,
                        onValueChange = { calories = it }
                    )

                    IosRowDivider()

                    IosTargetControlRow(
                        label = "Carboidrati",
                        value = carbs,
                        unit = "g",
                        valueRange = 30f..500f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.carbs,
                        icon = Icons.Default.Grain,
                        onValueChange = { carbs = it }
                    )

                    IosRowDivider()

                    IosTargetControlRow(
                        label = "Proteine",
                        value = protein,
                        unit = "g",
                        valueRange = 30f..350f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.protein,
                        icon = Icons.Default.EggAlt,
                        onValueChange = { protein = it }
                    )

                    IosRowDivider()

                    IosTargetControlRow(
                        label = "Grassi",
                        value = fat,
                        unit = "g",
                        valueRange = 15f..200f,
                        stepSize = 5f,
                        accentColor = MaterialTheme.macroColors.fat,
                        icon = Icons.Default.Opacity,
                        onValueChange = { fat = it }
                    )

                    IosRowDivider()

                    // Save Button
                    Box(modifier = Modifier.padding(14.dp)) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSound("click")
                                viewModel.updateDailyTarget(
                                    dateString = selectedTargetKey,
                                    calories = calories.roundToInt(),
                                    protein = protein.roundToInt(),
                                    carbs = carbs.roundToInt(),
                                    fat = fat.roundToInt()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNeo) Color(0xFFFFE500) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isNeo) Color.Black else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(12.dp),
                            border = if (isNeo) BorderStroke(2.dp, Color.Black) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .then(
                                    if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 8.dp)
                                    else Modifier
                                )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "SALVA OBIETTIVO ($selectedTargetKey)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. ATTIVITÀ & SALUTE (Health & Step Activity)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosSectionHeader(title = "Attività & Salute")

                IosGroupCard {
                    // Daily Steps Slider
                    IosTargetControlRow(
                        label = "Obiettivo Passi",
                        value = steps,
                        unit = "passi",
                        valueRange = 2000f..30000f,
                        stepSize = 500f,
                        accentColor = ApplePurple,
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        onValueChange = {
                            steps = it
                            preferencesManager.setStepGoal(it.roundToInt())
                        }
                    )

                    IosRowDivider()

                    // Google Health Connect
                    val isAvailable = hcAvailability == com.example.util.HealthConnectAvailability.AVAILABLE
                    IosSettingsRow(
                        icon = Icons.Default.Favorite,
                        iconBg = AppleRed,
                        title = "Health Connect",
                        subtitle = if (isHcAuthorized) "Sincronizzazione attiva" else "Sincronizza smartwatch e Google Fit",
                        trailing = {
                            val badgeText = when {
                                !isAvailable -> "Non Disp."
                                isHcAuthorized -> "Attivo"
                                else -> "Disconnesso"
                            }
                            val badgeColor = when {
                                isHcAuthorized -> Color(0xFF22C55E)
                                !isAvailable -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    )

                    if (isHcAuthorized) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Passi", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${hcDailyData.steps}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calorie Bruciate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${hcDailyData.caloriesBurned.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppleOrange)
                                }
                                if (hcDailyData.latestWeightKg != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Ultimo Peso", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("%.1f kg".format(hcDailyData.latestWeightKg), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppleBlue)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                                Toast.makeText(context, "Dati sincronizzati!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Errore di sincronizzazione", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sincronizza Ora", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                TextButton(
                                    onClick = { hcManager.openSettings(context) },
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Gestisci", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else if (isAvailable) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        hcPermissionLauncher.launch(hcManager.permissions)
                                    } catch (e: Exception) {
                                        hcManager.openSettings(context)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppleRed, contentColor = Color.White)
                            ) {
                                Text("CONNETTI GOOGLE HEALTH CONNECT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. ASPETTO & FEEDBACK (Theme, Sounds, Alerts)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosSectionHeader(title = "Aspetto & Feedback")

                IosGroupCard {
                    // Theme Skin Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosIconTile(icon = Icons.Default.Palette, backgroundColor = AppleYellow)
                            Text(
                                "Tema dell'Applicazione",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isNeo) FontWeight.Bold else FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Theme Selection Pills
                        val themes = ThemeSkin.values()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            themes.forEach { theme ->
                                val isSelected = theme == activeTheme
                                val pillBg = if (isSelected) {
                                    if (isNeo) Color(0xFFFFE500) else MaterialTheme.colorScheme.primary
                                } else Color.Transparent
                                val pillFg = if (isSelected) {
                                    if (isNeo) Color.Black else MaterialTheme.colorScheme.onPrimary
                                } else MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .then(
                                            if (isSelected && isNeo) Modifier.border(1.5.dp, Color.Black, RoundedCornerShape(8.dp))
                                            else Modifier
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            soundManager.playSound("click")
                                            preferencesManager.setThemeSkin(theme)
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(themePreviewColor(theme))
                                        )
                                        Text(
                                            text = theme.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = pillFg
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IosRowDivider()

                    // Sound Effects Switch
                    IosSettingsRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        iconBg = AppleBlue,
                        title = "Effetti Sonori",
                        subtitle = "Feedback audio alle interazioni",
                        trailing = {
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    soundManager.playSound("click")
                                    preferencesManager.setSoundEnabled(it)
                                },
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isNeo) Color(0xFF00E599) else AppleGreen
                                )
                            )
                        }
                    )

                    IosRowDivider()

                    // Step Sensor Switch
                    IosSettingsRow(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        iconBg = AppleGreen,
                        title = "Sensore Passi",
                        subtitle = "Contapassi integrato in background",
                        trailing = {
                            Switch(
                                checked = syncStepsEnabled,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    soundManager.playSound("click")
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                    } else {
                                        preferencesManager.setSyncSteps(checked)
                                    }
                                },
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isNeo) Color(0xFF00E599) else AppleGreen
                                )
                            )
                        }
                    )

                    IosRowDivider()

                    // Meal Reminders Switch
                    IosSettingsRow(
                        icon = Icons.Default.Notifications,
                        iconBg = AppleOrange,
                        title = "Promemoria Notifiche",
                        subtitle = "Avvisi per la registrazione dei pasti",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    soundManager.playSound("click")
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        preferencesManager.setNotificationsEnabled(checked)
                                        NotificationScheduler.scheduleReminders(context, checked)
                                    }
                                },
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = if (isNeo) Color(0xFF00E599) else AppleGreen
                                )
                            )
                        }
                    )
                }
            }

            // ==========================================
            // 4. PRIVACY & DATI (GDPR & Local-First)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosSectionHeader(title = "Privacy & Archiviazione Dati")

                IosGroupCard {
                    // Local First Badge
                    IosSettingsRow(
                        icon = Icons.Default.Shield,
                        iconBg = AppleGreen,
                        title = "100% Locale & Offline",
                        subtitle = "Nessun cloud: i dati risiedono solo su questo telefono",
                        trailing = {
                            Surface(
                                color = AppleGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "GDPR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = AppleGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    )

                    IosRowDivider()

                    // Export CSV
                    IosSettingsRow(
                        icon = Icons.Default.FileDownload,
                        iconBg = AppleBlue,
                        title = "Esporta Dati in CSV",
                        subtitle = "Esporta storico pasti e peso in formato standard",
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            viewModel.exportData(context)
                        }
                    )

                    IosRowDivider()

                    // Reset Data Row
                    IosSettingsRow(
                        icon = Icons.Default.DeleteForever,
                        iconBg = AppleRed,
                        title = "Ripristina Applicazione",
                        subtitle = "Cancella pasti, cibi personali e storico peso",
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showWipeDialog = true
                        }
                    )
                }
            }

            // ==========================================
            // 5. AGGIORNAMENTI & INFO APP
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IosSectionHeader(title = "Aggiornamenti & Informazioni")
                OtaUpdateCard()
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
