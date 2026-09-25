package com.example.ui.screens

import com.example.ui.SessionManager
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.foundation.appleSpring
import com.example.ui.foundation.AppleSprings
import com.example.ui.foundation.rememberAppleHaptics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.ApplePurple
import com.example.ui.theme.AppleCornerRadiusCard
import com.example.ui.theme.AppleCardShape
import com.example.ui.theme.macroColors


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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.util.LocalSoundManager
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
import com.example.ui.StatsViewModel
import com.example.ui.SettingsViewModel
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
import com.example.util.MealTypes

@Composable
fun CompactWeightChip(
    weightText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .applePressEffect(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            weightText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit weight",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun FoodLabScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .applePressEffect(pressedScale = 0.92f, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Torna indietro",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
    }
}

@Composable
fun FoodLabSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color.Black.copy(alpha = 0.16f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun FoodLabActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DashboardDateButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .applePressEffect(pressedScale = 0.9f, onClick = if (enabled) onClick else null)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            icon()
        }
    }
}

@Composable
fun SmoothProgressRail(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = appleSpring(
            dampingRatio = 0.75f,
            stiffness = 300f
        ),
        label = "SmoothProgressRail"
    )

    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
    }
}

@Composable
fun NutritionMetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
fun MacroSummaryPill(
    label: String,
    consumed: Int,
    target: Int,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text("${(progress * 100).coerceIn(0f, 999f).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SmoothProgressRail(progress = progress, color = color, modifier = Modifier.fillMaxWidth().height(8.dp))
        Text(
            "$consumed / $target g",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WeeklyProgressCell(
    label: String,
    progress: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress > 0f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        SmoothProgressRail(progress = progress, color = progressColor, modifier = Modifier.fillMaxWidth().height(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealItemRow(
    meal: MealEntry,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else {
                false
            }
        }
    )

    val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
    LaunchedEffect(isSwiping) {
        if (isSwiping) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete meal",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .applePressEffect(pressedScale = 0.985f, onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(accentColor, RoundedCornerShape(99.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                meal.foodName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                " (${meal.quantityGrams}g)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MacroBadge(
                                label = "C",
                                value = "${meal.carbs.roundToInt()}g",
                                color = MaterialTheme.macroColors.carbs
                            )
                            MacroBadge(
                                label = "P",
                                value = "${meal.protein.roundToInt()}g",
                                color = MaterialTheme.macroColors.protein
                            )
                            MacroBadge(
                                label = "F",
                                value = "${meal.fat.roundToInt()}g",
                                color = MaterialTheme.macroColors.fat
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "${meal.calories}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "KCAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = accentColor
                    )
                }
            }
        }
    )
}

@Composable
fun QuickFavoritesRow(
    favoriteFoods: List<FoodItem>,
    onQuickLog: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favoriteFoods.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Text(
                "QUICK FAVORITES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(favoriteFoods, key = { it.id }) { food ->
                val servingGrams = if (food.defaultServingSize > 0) food.defaultServingSize else 100
                val cals = food.caloriesPer100g * servingGrams / 100
                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .applePressEffect(pressedScale = 0.94f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            soundManager.playSound("power_up")
                            onQuickLog(food)
                        },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Quick add ${food.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            food.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${cals} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMacroLineChart(title: String, data: List<Float>, labels: List<String>, color: Color, suffix: String = "", onClick: () -> Unit = {}) {
    val maxVal = data.maxOrNull()?.let { if (it > 0) it else 100f } ?: 100f
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold)
    val reveal = remember(data) { androidx.compose.animation.core.Animatable(0f) }
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(data) {
        reveal.snapTo(0f)
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = appleSpring(
                dampingRatio = 0.75f,
                stiffness = 300f
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clickable { onClick() }
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                val pCount = data.size
                if (pCount > 0) {
                    val stepX = if (pCount > 1) size.width / (pCount - 1) else size.width / 2f
                    val topMargin = 12.dp.toPx()
                    val bottomMargin = 18.dp.toPx()
                    val availableHeight = size.height - topMargin - bottomMargin
                    val points = data.mapIndexed { idx, value ->
                        val ratio = if (maxVal > 0) value / maxVal else 0f
                        val x = if (pCount > 1) idx * stepX else size.width / 2f
                        val y = size.height - bottomMargin - (ratio * availableHeight)
                        Offset(x, y)
                    }

                    for (i in 0 until 4) {
                        val y = topMargin + (availableHeight / 3f) * i
                        drawLine(
                            color = outlineColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val revealPoints = ((pCount - 1) * reveal.value).coerceAtLeast(0f)
                    val wholeSegments = revealPoints.toInt().coerceAtMost((pCount - 1).coerceAtLeast(0))
                    val partial = revealPoints - wholeSegments

                    if (pCount > 1) {
                        for (i in 0 until wholeSegments) {
                            drawLine(
                                color = color.copy(alpha = 0.82f),
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        if (wholeSegments < pCount - 1 && partial > 0f) {
                            val start = points[wholeSegments]
                            val end = points[wholeSegments + 1]
                            val partialPoint = Offset(
                                x = start.x + (end.x - start.x) * partial,
                                y = start.y + (end.y - start.y) * partial
                            )
                            drawLine(
                                color = color.copy(alpha = 0.82f),
                                start = start,
                                end = partialPoint,
                                strokeWidth = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    val areaPath = Path().apply {
                        if (pCount == 1) {
                            moveTo(points[0].x, size.height - bottomMargin)
                            lineTo(points[0].x, points[0].y)
                            lineTo(points[0].x + 1f, points[0].y)
                            lineTo(points[0].x + 1f, size.height - bottomMargin)
                            close()
                        } else {
                            moveTo(points[0].x, size.height - bottomMargin)
                            lineTo(points[0].x, points[0].y)
                            for (i in 1..wholeSegments) {
                                lineTo(points[i].x, points[i].y)
                            }
                            if (wholeSegments < pCount - 1 && partial > 0f) {
                                val start = points[wholeSegments]
                                val end = points[wholeSegments + 1]
                                val partialPoint = Offset(
                                    x = start.x + (end.x - start.x) * partial,
                                    y = start.y + (end.y - start.y) * partial
                                )
                                lineTo(partialPoint.x, partialPoint.y)
                            }
                            lineTo(if (wholeSegments < pCount - 1 && partial > 0f) {
                                val start = points[wholeSegments]
                                val end = points[wholeSegments + 1]
                                start.x + (end.x - start.x) * partial
                            } else {
                                points[minOf(wholeSegments, points.lastIndex)].x
                            }, size.height - bottomMargin)
                            close()
                        }
                    }

                    drawPath(
                        path = areaPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f))
                        )
                    )

                    val visibleLabelStep = when {
                        pCount <= 4 -> 1
                        pCount <= 8 -> 2
                        else -> kotlin.math.ceil(pCount / 4f).toInt().coerceAtLeast(2)
                    }

                    points.take((wholeSegments + 1).coerceAtMost(points.size)).forEachIndexed { index, pt ->
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 2.dp.toPx(),
                            center = pt
                        )

                        val shouldLabel = index == 0 || index == points.lastIndex || index % visibleLabelStep == 0
                        if (!shouldLabel) return@forEachIndexed

                        val vText = if (suffix.isNotBlank()) "${data[index].toInt()}$suffix" else data[index].toInt().toString()
                        val measuredText = textMeasurer.measure(text = vText, style = valueStyle)
                        val chipPaddingX = 5.dp.toPx()
                        val chipPaddingY = 3.dp.toPx()
                        val chipWidth = measuredText.size.width + chipPaddingX * 2f
                        val chipHeight = measuredText.size.height + chipPaddingY * 2f
                        val preferredY = pt.y - chipHeight - 8.dp.toPx()
                        val chipX = (pt.x - chipWidth / 2f).coerceIn(2.dp.toPx(), size.width - chipWidth - 2.dp.toPx())
                        val chipY = preferredY.coerceIn(2.dp.toPx(), size.height - bottomMargin - chipHeight - 2.dp.toPx())
                        drawRoundRect(
                            color = surfaceColor.copy(alpha = 0.92f),
                            topLeft = Offset(chipX, chipY),
                            size = androidx.compose.ui.geometry.Size(chipWidth, chipHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawText(
                            textLayoutResult = measuredText,
                            topLeft = Offset(
                                x = chipX + chipPaddingX,
                                y = chipY + chipPaddingY
                            )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelStep = when {
                labels.size <= 5 -> 1
                labels.size <= 9 -> 2
                else -> kotlin.math.ceil(labels.size / 5f).toInt().coerceAtLeast(2)
            }
            labels.forEachIndexed { index, label ->
                Text(
                    text = if (index == 0 || index == labels.lastIndex || index % labelStep == 0) label.takeLast(2) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun HorizontalDatePicker(
    currentDate: String,
    today: String,
    streakCount: Int,
    weightText: String = "",
    onWeightClick: () -> Unit = {},
    onOpenStreakCalendar: () -> Unit = {},
    onDateChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateList = remember(currentDate, today) {
        val pivot = SessionManager.parseDateString(currentDate)
        val cal = Calendar.getInstance()
        (-3..3).map { offset ->
            cal.time = pivot
            cal.add(Calendar.DAY_OF_YEAR, offset)
            SessionManager.formatDate(cal.time)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Bar: Streak badge & Weight chip on Left, Calendar button on Far Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFEF08A).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFFEF08A).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .applePressEffect(pressedScale = 0.94f, onClick = onOpenStreakCalendar)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥 ${streakCount}D STREAK", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFEF08A))
                }

                if (weightText.isNotBlank()) {
                    CompactWeightChip(weightText = weightText, onClick = onWeightClick)
                }
            }

            IconButton(
                onClick = onOpenStreakCalendar,
                modifier = Modifier
                    .size(44.dp)
                    .applePressEffect(pressedScale = 0.90f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Streak Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        dateList.forEach { date ->
            val isSelected = date == currentDate
            val isToday = date == today
            val parsedDate = remember(date) { SessionManager.parseDateString(date) }
            val dayName = remember(parsedDate) { java.text.SimpleDateFormat("EEE", Locale.US).format(parsedDate).uppercase() }
            val dayNumber = remember(parsedDate) { java.text.SimpleDateFormat("dd", Locale.US).format(parsedDate) }

            val animatedBg by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayBg"
            )
            val animatedBorder by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayBorder"
            )
            val animatedDayTextColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayTextColor"
            )
            val animatedNumTextColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "numTextColor"
            )

            Column(
                modifier = Modifier
                    .width(44.dp)
                    .heightIn(min = 54.dp)
                    .applePressEffect(pressedScale = 0.92f) { onDateChange(date) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(animatedBg)
                    .border(1.dp, animatedBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = animatedDayTextColor
                )
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = animatedNumTextColor
                )
            }
        }
    }
    }
}

@Composable
fun AppleCalorieMacroRings(
    calorieProgress: Float,
    proteinProgress: Float,
    carbsProgress: Float,
    fatProgress: Float,
    calorieColor: Color = AppleOrange,
    proteinColor: Color = AppleBlue,
    carbsColor: Color = AppleGreen,
    fatColor: Color = ApplePurple,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 7.dp,
    ringGap: androidx.compose.ui.unit.Dp = 3.dp,
    centerContent: (@Composable () -> Unit)? = null
) {
    val animCalorieProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = calorieProgress.coerceIn(0f, 1f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animCalorieRing"
    )
    val animProteinProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = proteinProgress.coerceIn(0f, 1f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animProteinRing"
    )
    val animCarbsProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = carbsProgress.coerceIn(0f, 1f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animCarbsRing"
    )
    val animFatProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fatProgress.coerceIn(0f, 1f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animFatRing"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val gapPx = ringGap.toPx()
            val step = strokePx + gapPx
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (minOf(size.width, size.height) / 2f) - strokePx / 2f

            val rings = listOf(
                Triple(calorieColor, animCalorieProgress, baseRadius),
                Triple(proteinColor, animProteinProgress, baseRadius - step),
                Triple(carbsColor, animCarbsProgress, baseRadius - 2f * step),
                Triple(fatColor, animFatProgress, baseRadius - 3f * step)
            )

            rings.forEach { (color, progress, radius) ->
                if (radius > strokePx / 2f) {
                    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                    val diameter = radius * 2f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Draw muted background track
                    drawCircle(
                        color = color.copy(alpha = 0.16f),
                        radius = radius,
                        center = center,
                        style = stroke
                    )

                    // Draw animated progress arc
                    if (progress > 0.005f) {
                        drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }
            }
        }
        centerContent?.invoke()
    }
}

@Composable
private fun AppleMacroMetricLine(
    dotColor: Color,
    label: String,
    consumed: Int,
    target: Int
) {
    val progress = if (target > 0) (consumed.toFloat() / target).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        .size(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${consumed}g / ${target}g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SmoothProgressRail(
            progress = progress,
            color = dotColor,
            modifier = Modifier.fillMaxWidth().height(5.dp)
        )
    }
}

@Composable
fun UnifiedHeroHeader(
    remainingCalories: Int,
    targetCalories: Int,
    loggedCalories: Int,
    burnedCalories: Int,
    proteinConsumed: Float,
    proteinTarget: Float,
    carbsConsumed: Float,
    carbsTarget: Float,
    fatConsumed: Float,
    fatTarget: Float,
    steps: Int,
    stepsTarget: Int = 10000,
    isMascotTyping: Boolean = false,
    weightText: String,
    onWeightClick: () -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.22f),
                ambientColor = Color.Black.copy(alpha = 0.12f)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Section: Remaining Calories Display & Weight Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (remainingCalories >= 0) "$remainingCalories" else "${-remainingCalories}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                letterSpacing = (-1.2).sp
                            ),
                            color = if (remainingCalories < 0) AppleRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "KCAL",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (remainingCalories < 0) AppleRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (remainingCalories < 0) AppleRed.copy(alpha = 0.15f)
                                    else AppleGreen.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = if (remainingCalories >= 0) "RIMASTE" else "IN ECCESSO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (remainingCalories < 0) AppleRed else AppleGreen
                            )
                        }
                    }
                }
                CompactWeightChip(weightText = weightText, onClick = onWeightClick)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)

            // Middle Section: Mascot on Left, 3 Macro Metric Lines on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FoodLab Mascot in styled Apple glass container
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedFoodLabMascot(
                        state = when {
                            remainingCalories < 0 -> MascotState.OverTarget
                            isMascotTyping -> MascotState.Typing
                            else -> MascotState.Idle
                        },
                        modifier = Modifier.size(76.dp)
                    )
                }

                // 3 Macro Lines: Carboidrati, Proteine, Grassi
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppleMacroMetricLine(
                        dotColor = MaterialTheme.macroColors.carbs,
                        label = "Carboidrati",
                        consumed = carbsConsumed.roundToInt(),
                        target = carbsTarget.roundToInt()
                    )
                    AppleMacroMetricLine(
                        dotColor = MaterialTheme.macroColors.protein,
                        label = "Proteine",
                        consumed = proteinConsumed.roundToInt(),
                        target = proteinTarget.roundToInt()
                    )
                    AppleMacroMetricLine(
                        dotColor = MaterialTheme.macroColors.fat,
                        label = "Grassi",
                        consumed = fatConsumed.roundToInt(),
                        target = fatTarget.roundToInt()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)

            // Bottom Section: Integrated Steps Rail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .applePressEffect(pressedScale = 0.98f, onClick = onStepClick)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = "Passi",
                        tint = AppleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "PASSI GIORNALIERI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "$steps / $stepsTarget passi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(modifier = Modifier.width(90.dp)) {
                    SmoothProgressRail(
                        progress = (steps.toFloat() / stepsTarget.coerceAtLeast(1)).coerceIn(0f, 1f),
                        color = AppleBlue,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }
    }
}

private data class MealCategoryInfo(
    val typeKey: String,
    val title: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun UnifiedMealsCard(
    mealsByType: Map<String, List<MealEntry>>,
    favoriteFoods: List<FoodItem> = emptyList(),
    onAddMealClick: (String) -> Unit,
    onMealClick: (MealEntry) -> Unit,
    onQuickLog: ((FoodItem) -> Unit)? = null,
    onDeleteMeal: ((MealEntry) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val mealTypes = listOf(
        MealCategoryInfo(MealTypes.BREAKFAST, "Colazione 🌅", Icons.Default.EggAlt, Color(0xFF22C55E)),
        MealCategoryInfo(MealTypes.LUNCH, "Pranzo ☀️", Icons.Default.LunchDining, Color(0xFF3B82F6)),
        MealCategoryInfo(MealTypes.DINNER, "Cena 🌙", Icons.Default.DinnerDining, Color(0xFFA855F7)),
        MealCategoryInfo(MealTypes.SNACK, "Snack 🍎", Icons.Default.Icecream, Color(0xFFF59E0B))
    )
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PASTI DI OGGI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (favoriteFoods.isNotEmpty() && onQuickLog != null) {
            QuickFavoritesRow(
                favoriteFoods = favoriteFoods,
                onQuickLog = onQuickLog
            )
        }

        mealTypes.forEach { (typeKey, title, icon, accentColor) ->
            val meals = mealsByType[typeKey] ?: emptyList()
            val totalCals = meals.sumOf { it.calories }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section Contextual Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (meals.isNotEmpty()) {
                            Text(
                                "(${meals.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (meals.isNotEmpty()) "$totalCals KCAL" else "0 KCAL",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (meals.isNotEmpty()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .applePressEffect(pressedScale = 0.92f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddMealClick(typeKey)
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Text("Aggiungi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
                            }
                        }
                    }
                }

                // Cupertino Inset Grouped Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color.Black.copy(alpha = 0.18f),
                            ambientColor = Color.Black.copy(alpha = 0.10f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    if (meals.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            meals.forEachIndexed { index, meal ->
                                MealItemRow(
                                    meal = meal,
                                    accentColor = accentColor,
                                    onClick = { onMealClick(meal) },
                                    onDelete = { onDeleteMeal?.invoke(meal) }
                                )
                                if (index < meals.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 28.dp, end = 14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .applePressEffect(pressedScale = 0.98f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddMealClick(typeKey)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Nessun alimento registrato • Tocca per aggiungere",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun MonthlyStreakDialog(
    allMeals: List<MealEntry>,
    streakCount: Int,
    allTargets: List<DailyTarget> = emptyList(),
    defaultTarget: Int = 2000,
    onDateSelected: ((String) -> Unit)? = null,
    onDeleteDate: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val soundManager = LocalSoundManager.current
    val haptic = LocalHapticFeedback.current
    val todayStr = remember { SessionManager.formatDate(java.util.Date()) }
    var selectedDate by remember { mutableStateOf(todayStr) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val formattedDateLabel = remember(selectedDate) {
        try {
            val d = SessionManager.parseDateString(selectedDate)
            val sdf = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN)
            sdf.format(d).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
        } catch (_: Exception) {
            selectedDate
        }
    }

    // Last 28 days (4 full rows of 7 days)
    val last28Days = remember(todayStr) {
        (27 downTo 0).map { daysAgo ->
            SessionManager.getPreviousDay(todayStr, daysAgo)
        }
    }
    val mealsByDate = remember(allMeals) {
        allMeals.groupBy { it.dateString }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Calendario Streak",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 2. Flame Streak Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "🔥",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Column {
                            Text(
                                "Streak: $streakCount ${if (streakCount == 1) "Giorno" else "Giorni"} Consecutivi!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                "Tocca un giorno sul calendario per visualizzare il resoconto completo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Calendar Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Weekday headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM").forEach { dayHeader ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // 4 Weeks x 7 Days Grid
                    last28Days.chunked(7).forEach { rowDates ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowDates.forEach { date ->
                                val meals = mealsByDate[date] ?: emptyList()
                                val totalCals = meals.sumOf { it.calories }
                                val isLogged = totalCals > 0
                                val isSelected = date == selectedDate
                                val dayNumber = date.takeLast(2).toIntOrNull()?.toString() ?: date.takeLast(2)

                                val dayTarget = resolveCalendarDayTarget(date, allTargets, defaultTarget)

                                val (cellTextColor, cellBgColor, cellBorderColor) = when {
                                    !isLogged -> Triple(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                    totalCals <= dayTarget -> Triple(
                                        Color(0xFF22C55E), // Verde (Dentro le calorie)
                                        Color(0xFF22C55E).copy(alpha = 0.20f),
                                        Color(0xFF22C55E).copy(alpha = 0.80f)
                                    )
                                    totalCals <= dayTarget + 200 -> Triple(
                                        Color(0xFFF59E0B), // Giallo / Ambra (Poco oltre)
                                        Color(0xFFF59E0B).copy(alpha = 0.20f),
                                        Color(0xFFF59E0B).copy(alpha = 0.80f)
                                    )
                                    else -> Triple(
                                        Color(0xFFEF4444), // Rosso (Tanto oltre)
                                        Color(0xFFEF4444).copy(alpha = 0.20f),
                                        Color(0xFFEF4444).copy(alpha = 0.80f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellBgColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else cellBorderColor,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            soundManager.playSound("click")
                                            selectedDate = date
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected || isLogged) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else cellTextColor
                                        )
                                        if (isLogged) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else cellTextColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Color Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF22C55E), label = "In target")
                    LegendItem(color = Color(0xFFF59E0B), label = "Poco oltre")
                    LegendItem(color = Color(0xFFEF4444), label = "Tanto oltre")
                    LegendItem(color = MaterialTheme.colorScheme.outlineVariant, label = "Vuoto")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 5. Selected Day Detailed Summary Card
                val selMeals = mealsByDate[selectedDate] ?: emptyList()
                val selCalories = selMeals.sumOf { it.calories }
                val selProtein = selMeals.fold(0f) { acc, m -> acc + m.protein }
                val selCarbs = selMeals.fold(0f) { acc, m -> acc + m.carbs }
                val selFat = selMeals.fold(0f) { acc, m -> acc + m.fat }
                val selTarget = resolveCalendarDayTarget(selectedDate, allTargets, defaultTarget)
                val selIsLogged = selCalories > 0

                val formattedDateLabel = remember(selectedDate) {
                    try {
                        val d = SessionManager.parseDateString(selectedDate)
                        val sdf = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN)
                        sdf.format(d).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
                    } catch (_: Exception) {
                        selectedDate
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            Column {
                                Text(
                                    formattedDateLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (selectedDate == todayStr) "Oggi" else "Storico giornaliero",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status Chip
                            val (badgeText, badgeColor, badgeBg) = when {
                                !selIsLogged -> Triple(
                                    "NON REGISTRATO",
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                selCalories <= selTarget -> Triple(
                                    "IN TARGET",
                                    Color(0xFF22C55E),
                                    Color(0xFF22C55E).copy(alpha = 0.15f)
                                )
                                selCalories <= selTarget + 200 -> Triple(
                                    "+${selCalories - selTarget} KCAL",
                                    Color(0xFFF59E0B),
                                    Color(0xFFF59E0B).copy(alpha = 0.15f)
                                )
                                else -> Triple(
                                    "+${selCalories - selTarget} KCAL OVER",
                                    Color(0xFFEF4444),
                                    Color(0xFFEF4444).copy(alpha = 0.15f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }
                        }

                        if (selIsLogged) {
                            // Calorie Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Calorie Consumate:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$selCalories / $selTarget kcal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Macro Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MiniNutrientChip("Carboidrati", "${selCarbs.roundToInt()}g", MaterialTheme.macroColors.carbs, Modifier.weight(1f))
                                MiniNutrientChip("Proteine", "${selProtein.roundToInt()}g", MaterialTheme.macroColors.protein, Modifier.weight(1f))
                                MiniNutrientChip("Grassi", "${selFat.roundToInt()}g", MaterialTheme.macroColors.fat, Modifier.weight(1f))
                            }

                            // Grouped Meals Breakdown
                            val mealsByType = selMeals.groupBy { MealTypes.canonicalize(it.mealType) }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Pasti Registrati (${selMeals.size}):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                mealsByType.forEach { (type, typeMeals) ->
                                    val cals = typeMeals.sumOf { it.calories }
                                    val names = typeMeals.joinToString(", ") { it.foodName }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            "• ${type.uppercase()}: $names",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "$cals kcal",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = mealAccentColor(type)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Nessun pasto inserito in questo giorno.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (onDeleteDate != null && selIsLogged) {
                            OutlinedButton(
                                onClick = { showConfirmClearDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "SVUOTA GIORNATA",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (onDateSelected != null && selectedDate != todayStr) {
                            Button(
                                onClick = { onDateSelected(selectedDate) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("VAI A QUESTO GIORNO", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // 6. Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("CHIUDI", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (showConfirmClearDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmClearDialog = false },
                title = { Text("Svuotare giornata?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = { Text("Vuoi davvero eliminare tutti i pasti registrati per $formattedDateLabel? Il giorno tornerà non registrato (0 kcal).", style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            soundManager.playSound("click")
                            onDeleteDate?.invoke(selectedDate)
                            showConfirmClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Elimina", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmClearDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}

fun mealAccentColor(mealType: String): Color = when (mealType.lowercase(Locale.getDefault())) {
    "breakfast", "colazione" -> Color(0xFF22C55E)
    "lunch", "pranzo" -> Color(0xFF3B82F6)
    "snack" -> Color(0xFFF59E0B)
    "dinner", "cena" -> Color(0xFFA855F7)
    else -> Color(0xFF22C55E)
}

fun mealIcon(mealType: String): ImageVector = when (mealType.lowercase(Locale.getDefault())) {
    "breakfast", "colazione" -> Icons.Default.EggAlt
    "lunch", "pranzo" -> Icons.Default.LunchDining
    "snack" -> Icons.Default.Icecream
    "dinner", "cena" -> Icons.Default.DinnerDining
    else -> Icons.Default.Restaurant
}

@Composable
fun MacroPreviewCard(
    calories: Int,
    protein: Float,
    carbs: Float,
    fat: Float,
    accent: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("MACRONUTRIENTI", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$calories kcal", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = accent)
        }
        MacroDistributionBar(carbs = carbs, protein = protein, fat = fat)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientPill("Carboidrati", carbs, MaterialTheme.macroColors.carbs, Modifier.weight(1f))
            NutrientPill("Proteine", protein, MaterialTheme.macroColors.protein, Modifier.weight(1f))
            NutrientPill("Grassi", fat, MaterialTheme.macroColors.fat, Modifier.weight(1f))
        }
    }
}

@Composable
fun MacroDistributionBar(
    carbs: Float,
    protein: Float,
    fat: Float,
    modifier: Modifier = Modifier
) {
    val animCarbs by androidx.compose.animation.core.animateFloatAsState(
        targetValue = carbs.coerceAtLeast(0f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animCarbs"
    )
    val animProtein by androidx.compose.animation.core.animateFloatAsState(
        targetValue = protein.coerceAtLeast(0f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animProtein"
    )
    val animFat by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fat.coerceAtLeast(0f),
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "animFat"
    )
    val total = (animCarbs + animProtein + animFat).coerceAtLeast(0f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (total <= 0f) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
        } else {
            if (animCarbs > 0f) Box(modifier = Modifier.fillMaxHeight().weight(animCarbs / total).background(MaterialTheme.macroColors.carbs))
            if (animProtein > 0f) Box(modifier = Modifier.fillMaxHeight().weight(animProtein / total).background(MaterialTheme.macroColors.protein))
            if (animFat > 0f) Box(modifier = Modifier.fillMaxHeight().weight(animFat / total).background(MaterialTheme.macroColors.fat))
        }
    }
}

@Composable
fun NutrientPill(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(String.format(java.util.Locale.getDefault(), "%.1fg", value), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label.uppercase(java.util.Locale.getDefault()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniNutrientChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label.take(4).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun resolveCalendarDayTarget(date: String, targets: List<com.example.data.DailyTarget>, defaultTarget: Int = 2000): Int {
    val specific = targets.firstOrNull { it.dateString == date }
    if (specific != null) return specific.targetCalories

    val calendar = Calendar.getInstance().apply {
        time = SessionManager.parseDateString(date)
    }
    val weekdayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        Calendar.SUNDAY -> "SUNDAY"
        else -> ""
    }
    val weekdayTarget = targets.firstOrNull { it.dateString == weekdayName }
    if (weekdayTarget != null) return weekdayTarget.targetCalories

    return targets.firstOrNull { it.dateString == "FIXED" }?.targetCalories ?: defaultTarget
}
