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
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.macroColors
import com.example.ui.theme.isNeoBrutalist
import com.example.ui.theme.isBentoCraft
import com.example.ui.theme.isNothingOs
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceRaised
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingBorderVisible
import com.example.ui.theme.NothingTextDisabled
import com.example.ui.theme.NothingTextSecondary
import com.example.ui.theme.NothingTextPrimary
import com.example.ui.theme.NothingTextDisplay
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingDotMatrixText
import com.example.ui.theme.NothingSegmentedProgressBar
import com.example.ui.theme.NothingTechnicalChip
import com.example.ui.theme.nothingDotGridBackground
import com.example.ui.theme.BentoCocoa
import com.example.ui.theme.BentoBerryPink
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceContainerHigh
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.NeoBrutalAmber
import com.example.ui.theme.NeoBrutalCoral
import com.example.ui.theme.NeoBrutalMint
import com.example.ui.theme.NeoBrutalCyan
import com.example.ui.theme.NeoBrutalSurfaceContainerHigh
import com.example.ui.theme.NeoBrutalSurfaceVariant
import com.example.ui.theme.neoHardShadow
import com.example.ui.theme.neoBorder
import com.example.ui.theme.foodLabCardModifier
import com.example.ui.theme.foodLabCardShape
import com.example.ui.theme.foodLabCardBorder
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
import com.example.ui.SettingsViewModel
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
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.input.pointer.pointerInput
import com.example.util.MealTypes

@Composable
fun CompactWeightChip(
    weightText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
    val chipShape = when {
        isNothing -> RoundedCornerShape(99.dp)
        isNeo -> RoundedCornerShape(8.dp)
        isBento -> RoundedCornerShape(99.dp)
        else -> RoundedCornerShape(22.dp)
    }
    val chipBorder = when {
        isNothing -> BorderStroke(1.dp, NothingBorderVisible)
        isNeo -> BorderStroke(1.5.dp, Color.Black)
        isBento -> BorderStroke(1.5.dp, BentoBorder)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
    val chipBg = when {
        isNothing -> NothingSurfaceRaised
        isNeo -> Color.White
        isBento -> BentoSurfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val chipContentColor = when {
        isNothing -> NothingTextDisplay
        isNeo -> Color.Black
        isBento -> BentoCocoa
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 8.dp)
                else Modifier
            )
            .applePressEffect(pressedScale = 0.94f, onClick = onClick)
            .clip(chipShape)
            .background(chipBg)
            .border(chipBorder.width, chipBorder.brush, chipShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = weightText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isNothing) FontWeight.Bold else if (isNeo || isBento) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
            ),
            color = if (isNothing) NothingTextDisplay else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit weight",
            tint = if (isNothing) NothingRed else chipContentColor,
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
    val isNeo = MaterialTheme.isNeoBrutalist
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
                val backShape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(12.dp)
                val backBorder = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                val backBg = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainerHigh

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .then(
                            if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 8.dp)
                            else Modifier
                        )
                        .clip(backShape)
                        .background(backBg)
                        .border(backBorder.width, backBorder.brush, backShape)
                        .applePressEffect(pressedScale = 0.92f, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Torna indietro",
                        tint = if (isNeo) Color.Black else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = if (isNeo) 0.sp else (-0.3).sp
                    ),
                    color = if (isNeo) Color.Black else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isNeo) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isNeo) Color(0xFF333333) else MaterialTheme.colorScheme.onSurfaceVariant,
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
fun BentoStickerBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    rotation: Float = -2.5f,
    backgroundColor: Color = BentoBerryPink,
    textColor: Color = Color.White
) {
    Surface(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
            }
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(99.dp),
                spotColor = BentoCocoa.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(99.dp),
        color = backgroundColor,
        border = BorderStroke(1.2.dp, BentoBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    fontSize = 8.5.sp
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun BakeryAwningStripes(
    modifier: Modifier = Modifier,
    stripeColor: Color = BentoBerryPink.copy(alpha = 0.65f),
    bgColor: Color = BentoSurfaceContainerHigh,
    stripeWidthDp: Float = 10f
) {
    Canvas(modifier = modifier) {
        val stripeWidthPx = stripeWidthDp.dp.toPx()
        val totalWidth = size.width
        val height = size.height

        drawRect(color = bgColor)

        var x = 0f
        var drawStripe = true
        while (x < totalWidth) {
            if (drawStripe) {
                drawRect(
                    color = stripeColor,
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(stripeWidthPx.coerceAtMost(totalWidth - x), height)
                )
            }
            x += stripeWidthPx
            drawStripe = !drawStripe
        }
    }
}

@Composable
fun FoodLabSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val cardShape = when {
        isNeo -> RoundedCornerShape(12.dp)
        isBento -> RoundedCornerShape(22.dp)
        else -> RoundedCornerShape(22.dp)
    }
    val cardBorder = when {
        isNeo -> BorderStroke(2.dp, Color.Black)
        isBento -> BorderStroke(1.8.dp, BentoBorder)
        else -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
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
                if (isNeo) Modifier.neoHardShadow(offset = 4.dp, color = Color.Black, cornerRadius = 12.dp)
                else if (isBento) Modifier.shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    spotColor = BentoCocoa.copy(alpha = 0.12f),
                    ambientColor = BentoCocoa.copy(alpha = 0.08f)
                )
                else Modifier.shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    spotColor = Color.Black.copy(alpha = 0.16f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = cardBorder
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val buttonShape = when {
        isNeo -> RoundedCornerShape(8.dp)
        isBento -> RoundedCornerShape(99.dp)
        else -> RoundedCornerShape(14.dp)
    }
    val borderColor = when {
        isNeo -> Color.Black
        isBento -> BentoBorder
        else -> if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    }
    val borderWidth = when {
        isNeo -> 2.dp
        isBento -> 1.5.dp
        else -> 1.dp
    }
    val containerColor = when {
        isNeo -> if (danger) Color(0xFFFF8A80) else NeoBrutalAmber
        isBento -> if (danger) Color(0xFFFF8A80) else BentoBerryPink
        else -> if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isNeo -> Color.Black
        isBento -> Color.White
        else -> if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
    }

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
            .height(44.dp)
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 8.dp)
                else if (isBento) Modifier.shadow(elevation = 3.dp, shape = buttonShape, spotColor = BentoCocoa.copy(alpha = 0.15f))
                else Modifier
            )
            .applePressEffect(pressedScale = 0.92f, onClick = onClick)
            .border(borderWidth, borderColor, buttonShape),
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isNeo || isBento) FontWeight.Black else FontWeight.Bold),
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val btnShape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(14.dp)
    val btnBorder = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 8.dp)
                else Modifier
            )
            .applePressEffect(pressedScale = 0.9f, onClick = if (enabled) onClick else null)
            .clip(btnShape)
            .background(if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(btnBorder.width, btnBorder.brush, btnShape)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides (if (isNeo) Color.Black else MaterialTheme.colorScheme.onSurface)) {
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val railShape = if (isNeo) RoundedCornerShape(4.dp) else RoundedCornerShape(99.dp)
    val railTrackBg = if (isNeo) Color(0xFFE2DBCB) else MaterialTheme.colorScheme.surfaceContainerLow
    val railBorder = if (isNeo) Modifier.border(1.dp, Color.Black, railShape) else Modifier

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
            .then(railBorder)
            .clip(railShape)
            .background(railTrackBg)
            .padding(if (isNeo) 0.5.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedProgress
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                .clip(railShape)
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
    modifier: Modifier = Modifier,
    dragDropState: MealDragDropState? = null,
    textColor: Color? = null,
    secondaryTextColor: Color? = null,
    rowBackgroundColor: Color? = null,
    macroContentColor: Color? = null,
    macroContainerColor: Color? = null,
    macroBorderColor: Color? = null
) {
    val haptic = LocalHapticFeedback.current
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val effectiveTextColor = textColor ?: if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
    val effectiveSecondaryColor = secondaryTextColor ?: if (isBento) BentoCocoa.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
    val effectiveRowBg = rowBackgroundColor ?: if (isBento) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isBeingDragged = dragDropState?.isDragging == true && dragDropState?.draggedMeal?.id == meal.id

    val isAnyDragging = dragDropState?.isDragging == true

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
        enableDismissFromEndToStart = !isBeingDragged && !isAnyDragging,
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
                    .graphicsLayer {
                        alpha = if (isBeingDragged) 0.30f else 1.0f
                    }
                    .background(effectiveRowBg)
                    .onGloballyPositioned { coords ->
                        itemCoordinates = coords
                    }
                    .then(
                        if (dragDropState != null) {
                            Modifier.pointerInput(meal.id, isSwiping) {
                                if (isSwiping) return@pointerInput
                                try {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { localOffset ->
                                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                                                dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                                return@detectDragGesturesAfterLongPress
                                            }
                                            itemCoordinates?.let { coords ->
                                                val rootTopLeft = coords.positionInRoot()
                                                val touchInRoot = rootTopLeft + localOffset
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragDropState.onDragStart(
                                                    meal = meal,
                                                    sourceMealType = meal.mealType,
                                                    itemTopLeftInRoot = rootTopLeft,
                                                    itemWidthPx = coords.size.width.toFloat(),
                                                    itemHeightPx = coords.size.height.toFloat(),
                                                    touchPositionInRoot = touchInRoot
                                                )
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDropState.onDrag(dragAmount)
                                        },
                                        onDragEnd = {
                                            dragDropState.onDragEnd { droppedMeal, targetType ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragDropState.onDropListener?.invoke(droppedMeal, targetType)
                                            }
                                        },
                                        onDragCancel = {
                                            dragDropState.onDragCancel()
                                        }
                                    )
                                } finally {
                                    if (dragDropState.isDragging && dragDropState.draggedMeal?.id == meal.id) {
                                        dragDropState.onDragCancel()
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
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
                                color = effectiveTextColor
                            )
                            Text(
                                " (${meal.quantityGrams}g)",
                                style = MaterialTheme.typography.labelSmall,
                                color = effectiveSecondaryColor
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MacroBadge(
                                label = "C",
                                value = "${meal.carbs.roundToInt()}g",
                                color = MaterialTheme.macroColors.carbs,
                                contentColor = macroContentColor,
                                containerColor = macroContainerColor,
                                borderColor = macroBorderColor
                            )
                            MacroBadge(
                                label = "P",
                                value = "${meal.protein.roundToInt()}g",
                                color = MaterialTheme.macroColors.protein,
                                contentColor = macroContentColor,
                                containerColor = macroContainerColor,
                                borderColor = macroBorderColor
                            )
                            MacroBadge(
                                label = "F",
                                value = "${meal.fat.roundToInt()}g",
                                color = MaterialTheme.macroColors.fat,
                                contentColor = macroContentColor,
                                containerColor = macroContainerColor,
                                borderColor = macroBorderColor
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
                        color = effectiveTextColor
                    )
                    Text(
                        "KCAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = if (isBento && textColor != null) secondaryTextColor ?: textColor else accentColor
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
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
                tint = if (isNothing) NothingRed else if (isBento) BentoBerryPink else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Text(
                if (isNothing) "// QUICK FAVORITES" else "QUICK FAVORITES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                    letterSpacing = if (isNothing) 1.0.sp else 0.sp
                ),
                color = if (isNothing) NothingTextDisplay else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isBento) {
                BentoStickerBadge(
                    text = "Favorites",
                    icon = Icons.Default.Cookie,
                    backgroundColor = BentoSurfaceContainerHigh,
                    textColor = BentoCocoa
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(favoriteFoods, key = { it.id }) { food ->
                val servingGrams = if (food.defaultServingSize > 0) food.defaultServingSize else 100
                val cals = food.caloriesPer100g * servingGrams / 100
                val favShape = when {
                    isNothing -> RoundedCornerShape(999.dp)
                    isBento -> RoundedCornerShape(99.dp)
                    else -> RoundedCornerShape(22.dp)
                }
                val favBorder = when {
                    isNothing -> BorderStroke(1.dp, NothingBorderVisible)
                    isBento -> BorderStroke(1.2.dp, BentoBorder)
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
                val favBg = when {
                    isNothing -> NothingSurfaceRaised
                    isBento -> BentoSurface
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }

                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .then(
                            if (isNothing) Modifier
                            else if (isBento) Modifier.shadow(elevation = 2.dp, shape = favShape, spotColor = BentoCocoa.copy(alpha = 0.12f))
                            else Modifier
                        )
                        .applePressEffect(pressedScale = 0.94f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            soundManager.playSound("power_up")
                            onQuickLog(food)
                        },
                    shape = favShape,
                    color = favBg,
                    border = favBorder
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Quick add ${food.name}",
                            tint = if (isNothing) NothingRed else if (isBento) BentoBerryPink else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            food.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isNothing || isBento) FontWeight.Bold else FontWeight.SemiBold,
                                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                            ),
                            color = if (isNothing) NothingTextPrimary else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isNothing) "[ ${cals} KCAL ]" else "${cals} kcal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isNothing || isBento) FontWeight.Medium else FontWeight.Normal,
                                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                            ),
                            color = if (isNothing) NothingTextSecondary else if (isBento) BentoBerryPink else MaterialTheme.colorScheme.onSurfaceVariant
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

    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
    val containerShape = when {
        isNothing -> RoundedCornerShape(14.dp)
        isNeo -> RoundedCornerShape(12.dp)
        isBento -> RoundedCornerShape(22.dp)
        else -> RoundedCornerShape(20.dp)
    }
    val containerBorder = when {
        isNothing -> BorderStroke(1.dp, NothingBorderVisible)
        isNeo -> BorderStroke(2.dp, Color.Black)
        isBento -> BorderStroke(1.8.dp, BentoBorder)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
    val containerBg = when {
        isNothing -> NothingSurface
        isNeo -> Color.White
        isBento -> BentoSurface
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isNothing) Modifier
                else if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 12.dp)
                else if (isBento) Modifier.shadow(
                    elevation = 4.dp,
                    shape = containerShape,
                    spotColor = BentoCocoa.copy(alpha = 0.12f),
                    ambientColor = BentoCocoa.copy(alpha = 0.08f)
                )
                else Modifier
            )
            .clip(containerShape)
            .background(containerBg)
            .border(containerBorder.width, containerBorder.brush, containerShape)
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
                val streakBg = when {
                    isNothing -> NothingSurfaceRaised
                    isNeo -> NeoBrutalAmber
                    isBento -> BentoBerryPink
                    else -> AppleOrange.copy(alpha = 0.18f)
                }
                val streakBorder = when {
                    isNothing -> BorderStroke(1.dp, NothingRed)
                    isNeo -> BorderStroke(1.5.dp, Color.Black)
                    isBento -> BorderStroke(1.5.dp, BentoBorder)
                    else -> BorderStroke(1.dp, AppleOrange.copy(alpha = 0.4f))
                }
                val streakFg = when {
                    isNothing -> NothingRed
                    isNeo -> Color.Black
                    isBento -> Color.White
                    else -> AppleOrange
                }
                val streakShape = when {
                    isNothing -> RoundedCornerShape(999.dp)
                    isNeo -> RoundedCornerShape(8.dp)
                    isBento -> RoundedCornerShape(99.dp)
                    else -> RoundedCornerShape(16.dp)
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(streakShape)
                        .background(streakBg)
                        .border(streakBorder.width, streakBorder.brush, streakShape)
                        .applePressEffect(pressedScale = 0.94f, onClick = onOpenStreakCalendar)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = streakFg,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${streakCount}D STREAK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                            ),
                            color = streakFg
                        )
                    }
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
                    tint = when {
                        isNothing -> NothingTextDisplay
                        isNeo -> Color.Black
                        isBento -> BentoBerryPink
                        else -> MaterialTheme.colorScheme.primary
                    },
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

            val targetBg = when {
                isNothing -> if (isSelected) NothingTextDisplay else if (isToday) NothingSurfaceRaised else Color.Transparent
                isNeo -> if (isSelected) NeoBrutalAmber else if (isToday) Color(0xFFF3EFE6) else Color.White
                isBento -> if (isSelected) BentoBerryPink else if (isToday) BentoSurfaceContainerHigh else BentoSurface
                else -> if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
            }
            val targetBorder = when {
                isNothing -> if (isSelected) NothingTextDisplay else if (isToday) NothingRed else Color.Transparent
                isNeo -> if (isSelected || isToday) Color.Black else Color.Black.copy(alpha = 0.25f)
                isBento -> if (isSelected || isToday) BentoBorder else BentoBorder.copy(alpha = 0.20f)
                else -> if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
            }
            val targetBorderWidth = when {
                isNothing -> 1.dp
                isNeo -> if (isSelected || isToday) 1.5.dp else 1.dp
                isBento -> if (isSelected || isToday) 1.5.dp else 1.dp
                else -> 1.dp
            }

            val targetDayTextColor = when {
                isNothing -> if (isSelected) NothingBlack else if (isToday) NothingRed else NothingTextSecondary
                isNeo -> if (isSelected) Color.Black else if (isToday) Color.Black else Color(0xFF555555)
                isBento -> if (isSelected) Color.White else if (isToday) BentoCocoa else BentoCocoa.copy(alpha = 0.7f)
                else -> if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            }
            val targetNumTextColor = when {
                isNothing -> if (isSelected) NothingBlack else NothingTextPrimary
                isNeo -> Color.Black
                isBento -> if (isSelected) Color.White else BentoCocoa
                else -> if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            }

            val animatedBg by androidx.compose.animation.animateColorAsState(
                targetValue = targetBg,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayBg"
            )
            val animatedBorder by androidx.compose.animation.animateColorAsState(
                targetValue = targetBorder,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayBorder"
            )
            val animatedDayTextColor by androidx.compose.animation.animateColorAsState(
                targetValue = targetDayTextColor,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dayTextColor"
            )
            val animatedNumTextColor by androidx.compose.animation.animateColorAsState(
                targetValue = targetNumTextColor,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "numTextColor"
            )

            val selectedDayScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 350f
                ),
                label = "dayScale"
            )
            val selectedDayBob by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) -2f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 350f
                ),
                label = "dayBob"
            )

            val capsuleShape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(14.dp)

            Column(
                modifier = Modifier
                    .width(44.dp)
                    .heightIn(min = 54.dp)
                    .graphicsLayer {
                        scaleX = selectedDayScale
                        scaleY = selectedDayScale
                        translationY = selectedDayBob
                    }
                    .applePressEffect(pressedScale = 0.92f) { onDateChange(date) }
                    .clip(capsuleShape)
                    .background(animatedBg)
                    .border(targetBorderWidth, animatedBorder, capsuleShape)
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
    proteinColor: Color = Color(0xFF22C55E),
    carbsColor: Color = Color(0xFFF59E0B),
    fatColor: Color = Color(0xFF06B6D4),
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
private fun BentoMacroCell(
    label: String,
    consumed: Int,
    target: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (consumed.toFloat() / target).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BentoCocoa.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = BentoCocoa,
                    maxLines = 1
                )
            }

            Text(
                text = "${consumed}g / ${target}g",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = BentoCocoa.copy(alpha = 0.85f),
                maxLines = 1
            )

            SmoothProgressRail(
                progress = progress,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            )
        }
    }
}

@Composable
fun CompactStepChip(
    steps: Int,
    stepsTarget: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNothing = MaterialTheme.isNothingOs
    val chipShape = RoundedCornerShape(99.dp)
    val chipBorder = if (isNothing) BorderStroke(1.dp, NothingBorderVisible) else BorderStroke(1.5.dp, BentoBorder)
    val chipBg = if (isNothing) NothingSurfaceRaised else BentoSurfaceContainerHigh
    val iconTint = if (isNothing) NothingRed else BentoBerryPink
    val textColor = if (isNothing) NothingTextDisplay else BentoCocoa

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .applePressEffect(pressedScale = 0.94f, onClick = onClick)
            .clip(chipShape)
            .background(chipBg)
            .border(chipBorder.width, chipBorder.brush, chipShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = "Passi",
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$steps",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
            ),
            color = textColor
        )
    }
}

@Composable
private fun BentoHeroHeader(
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
    stepsTarget: Int,
    isMascotTyping: Boolean,
    weightText: String,
    onWeightClick: () -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "dashboard_hero_card" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Unified Bento Calorie Hero Card (warm BentoSurface with BentoBorder)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = BentoCocoa.copy(alpha = 0.12f),
                    ambientColor = BentoCocoa.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            border = BorderStroke(2.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top status bar inside hero card: Badge on left, Compact Steps & Weight Chips on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (remainingCalories < 0) Color(0xFFEF4444) else BentoBerryPink)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (remainingCalories >= 0) "RIMASTE" else "IN ECCESSO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactStepChip(
                            steps = steps,
                            stepsTarget = stepsTarget,
                            onClick = onStepClick
                        )
                        CompactWeightChip(
                            weightText = weightText,
                            onClick = onWeightClick
                        )
                    }
                }

                // Middle Section: Calories & Breakdown on Left, Mascot organic on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (remainingCalories >= 0) "$remainingCalories" else "${-remainingCalories}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 40.sp,
                                    letterSpacing = (-1.5).sp
                                ),
                                color = BentoCocoa
                            )
                            Text(
                                text = "KCAL",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = BentoBerryPink,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Text(
                            text = "Obiettivo $targetCalories kcal • Consumate $loggedCalories • Bruciate $burnedCalories",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            color = BentoCocoa.copy(alpha = 0.70f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Compact macro distribution indicator
                        MacroDistributionBar(
                            carbs = carbsConsumed,
                            protein = proteinConsumed,
                            fat = fatConsumed,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                        )
                    }

                    // Mascot living organically on the surface (no ugly container box!)
                    AnimatedFoodLabMascot(
                        state = when {
                            remainingCalories < 0 -> MascotState.OverTarget
                            isMascotTyping -> MascotState.Typing
                            else -> MascotState.Idle
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }

        // Bento Row 2: 3 Separate Macro Bento Pillar Cards (Carbs, Protein, Fat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoMacroCell(
                label = "Carboidrati",
                consumed = carbsConsumed.roundToInt(),
                target = carbsTarget.roundToInt(),
                color = MaterialTheme.macroColors.carbs,
                bgColor = Color(0xFFFFFBEB),
                modifier = Modifier.weight(1f)
            )

            BentoMacroCell(
                label = "Proteine",
                consumed = proteinConsumed.roundToInt(),
                target = proteinTarget.roundToInt(),
                color = MaterialTheme.macroColors.protein,
                bgColor = Color(0xFFF0FDF4),
                modifier = Modifier.weight(1f)
            )

            BentoMacroCell(
                label = "Grassi",
                consumed = fatConsumed.roundToInt(),
                target = fatTarget.roundToInt(),
                color = MaterialTheme.macroColors.fat,
                bgColor = Color(0xFFF0F9FF),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NothingMacroPillar(
    label: String,
    code: String,
    consumed: Int,
    target: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) consumed.toFloat() / target.toFloat() else 0f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NothingSurface)
            .border(1.dp, NothingBorderVisible, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = NothingTextSecondary
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp
                    ),
                    color = NothingTextDisabled
                )
            }
            Text(
                text = "${consumed}g / ${target}g",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.2.sp
                ),
                color = accentColor
            )
            // 8-segment mini bar
            NothingSegmentedProgressBar(
                progress = progress,
                segments = 8,
                segmentHeight = 4.dp,
                segmentGap = 1.5.dp,
                activeColor = accentColor,
                emptyColor = NothingBorder,
                overflowColor = NothingRed
            )
        }
    }
}

@Composable
private fun NothingHeroHeader(
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
    stepsTarget: Int,
    isMascotTyping: Boolean,
    weightText: String,
    onWeightClick: () -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isOverLimit = remainingCalories < 0
    val displayRemaining = if (remainingCalories >= 0) remainingCalories else -remainingCalories
    val progress = if (targetCalories > 0) loggedCalories.toFloat() / targetCalories.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "dashboard_hero_card" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Nothing Industrial Instrument Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NothingBorderVisible, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NothingSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .nothingDotGridBackground(dotColor = NothingBorderVisible.copy(alpha = 0.25f))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Instrument Bar: Status / System telemetry + Step & Weight Chips
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
                                .clip(CircleShape)
                                .background(if (isOverLimit) NothingRed else NothingTextDisplay)
                        )
                        Text(
                            text = "SYS // CALORIE.MATRIX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.0.sp
                            ),
                            color = NothingTextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingTechnicalChip(
                            label = "PASSI",
                            value = "$steps",
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onStepClick()
                            }
                        )
                        NothingTechnicalChip(
                            label = "PESO",
                            value = weightText,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onWeightClick()
                            }
                        )
                    }
                }

                // Primary Display: Dot-matrix LED numbers + KCAL + RIMASTE / IN ECCESSO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$displayRemaining",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 44.sp,
                                    lineHeight = 44.sp,
                                    letterSpacing = (-1.5).sp
                                ),
                                color = if (isOverLimit) NothingRed else NothingTextDisplay
                            )
                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    text = "KCAL",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.0.sp
                                    ),
                                    color = if (isOverLimit) NothingRed else NothingTextDisplay
                                )
                                Text(
                                    text = if (isOverLimit) "IN ECCESSO" else "RIMASTE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (isOverLimit) NothingRed else NothingTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "GOAL $targetCalories // LOGGED $loggedCalories // BURNED -$burnedCalories",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = NothingTextSecondary
                        )
                    }

                    // Mechanical Mascot status indicator
                    AnimatedFoodLabMascot(
                        state = when {
                            isOverLimit -> MascotState.OverTarget
                            isMascotTyping -> MascotState.Typing
                            else -> MascotState.Idle
                        },
                        modifier = Modifier
                            .size(70.dp)
                            .padding(start = 4.dp)
                    )
                }

                // Nothing Signature Segmented Progress Bar (discrete LED segments with 2px gaps)
                NothingSegmentedProgressBar(
                    progress = progress,
                    segments = 24,
                    segmentHeight = 8.dp,
                    segmentGap = 2.dp,
                    activeColor = NothingTextDisplay,
                    emptyColor = NothingBorder,
                    overflowColor = NothingRed
                )
            }
        }

        // Row of 3 Macro Pillar Cards (Space Mono uppercase, segmented indicators)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NothingMacroPillar(
                label = "Proteine",
                code = "P // 01",
                consumed = proteinConsumed.roundToInt(),
                target = proteinTarget.roundToInt(),
                accentColor = NothingRed,
                modifier = Modifier.weight(1f)
            )
            NothingMacroPillar(
                label = "Carboidrati",
                code = "C // 02",
                consumed = carbsConsumed.roundToInt(),
                target = carbsTarget.roundToInt(),
                accentColor = NothingTextDisplay,
                modifier = Modifier.weight(1f)
            )
            NothingMacroPillar(
                label = "Grassi",
                code = "F // 03",
                consumed = fatConsumed.roundToInt(),
                target = fatTarget.roundToInt(),
                accentColor = NothingTextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NeoBrutalMacroCard(
    label: String,
    tag: String,
    consumed: Float,
    target: Float,
    bannerColor: Color,
    bannerTextColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (consumed / target.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val cardShape = RoundedCornerShape(10.dp)
    
    Column(
        modifier = modifier
            .neoHardShadow(offset = 4.dp, color = Color.Black, cornerRadius = 10.dp)
            .clip(cardShape)
            .background(Color.White)
            .border(2.5.dp, Color.Black, cardShape)
    ) {
        // Top solid banner with tag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerColor)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                color = bannerTextColor
            )
        }
        
        // Divider line under banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black)
        )
        
        // Body with numbers & chunky stepped meter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${consumed.roundToInt()}g",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                ),
                color = Color.Black
            )
            Text(
                text = "/ ${target.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = Color(0xFF555555)
            )
            
            // 4 Chunky Stepped Blocks Meter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = progress >= (i * 0.25f + 0.05f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(if (isFilled) bannerColor else Color(0xFFE5DEC9))
                            .border(1.dp, Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeoBrutalStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .clip(chipShape)
            .background(NeoBrutalSurfaceVariant)
            .border(1.5.dp, Color.Black, chipShape)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.3.sp
            ),
            color = Color(0xFF444444)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            ),
            color = Color.Black
        )
    }
}

@Composable
private fun NeoBrutalHeroHeader(
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
    stepsTarget: Int,
    isMascotTyping: Boolean,
    weightText: String,
    onWeightClick: () -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "dashboard_hero_card" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Ribbon: Angled Sticker Tag on Left, Steps & Weight on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = -1.5f }
                    .neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeoBrutalAmber)
                    .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "⚡ TARGET DEL GIORNO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.Black
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactStepChip(
                    steps = steps,
                    stepsTarget = stepsTarget,
                    onClick = onStepClick
                )
                CompactWeightChip(
                    weightText = weightText,
                    onClick = onWeightClick
                )
            }
        }

        // 2. High-Impact Asymmetric Calorie Billboard
        val billboardShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoHardShadow(offset = 6.dp, color = Color.Black, cornerRadius = 14.dp)
                .clip(billboardShape)
                .background(Color.White)
                .border(3.dp, Color.Black, billboardShape)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (remainingCalories >= 0) "$remainingCalories" else "${-remainingCalories}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp,
                                    letterSpacing = (-1.5).sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = "KCAL",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                ),
                                color = if (remainingCalories < 0) NeoBrutalCoral else Color.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Raw Brutalist Stamp Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (remainingCalories >= 0) NeoBrutalCoral else Color.Black)
                                .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (remainingCalories >= 0) "KCAL RIMASTE" else "IN ECCESSO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Mascot framed inside a playful brutalist polaroid/stamp
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeoBrutalAmber.copy(alpha = 0.35f))
                            .border(2.5.dp, Color.Black, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedFoodLabMascot(
                            state = when {
                                remainingCalories < 0 -> MascotState.OverTarget
                                isMascotTyping -> MascotState.Typing
                                else -> MascotState.Idle
                            },
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }

                // Chunky Segmented Brutalist Progress Bar
                val progressFraction = (loggedCalories.toFloat() / targetCalories.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val barShape = RoundedCornerShape(6.dp)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(barShape)
                            .background(NeoBrutalSurfaceContainerHigh)
                            .border(2.dp, Color.Black, barShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(if (remainingCalories < 0) NeoBrutalCoral else NeoBrutalAmber)
                        )
                    }

                    // Bottom Telemetry Chips in a Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeoBrutalStatChip("OBIETTIVO", "$targetCalories", Modifier.weight(1f))
                        NeoBrutalStatChip("ASSUNTE", "$loggedCalories", Modifier.weight(1f))
                        NeoBrutalStatChip("BRUCIATE", "+$burnedCalories", Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. Row of 3 Standalone Brutalist Macro Punch-Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeoBrutalMacroCard(
                label = "Proteine",
                tag = "PROTEINE",
                consumed = proteinConsumed,
                target = proteinTarget,
                bannerColor = NeoBrutalCoral,
                bannerTextColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            NeoBrutalMacroCard(
                label = "Carboidrati",
                tag = "CARBOIDRATI",
                consumed = carbsConsumed,
                target = carbsTarget,
                bannerColor = NeoBrutalAmber,
                bannerTextColor = Color.Black,
                modifier = Modifier.weight(1f)
            )
            NeoBrutalMacroCard(
                label = "Grassi",
                tag = "GRASSI",
                consumed = fatConsumed,
                target = fatTarget,
                bannerColor = NeoBrutalCyan,
                bannerTextColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
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
    val isNothing = MaterialTheme.isNothingOs
    if (isNothing) {
        NothingHeroHeader(
            remainingCalories = remainingCalories,
            targetCalories = targetCalories,
            loggedCalories = loggedCalories,
            burnedCalories = burnedCalories,
            proteinConsumed = proteinConsumed,
            proteinTarget = proteinTarget,
            carbsConsumed = carbsConsumed,
            carbsTarget = carbsTarget,
            fatConsumed = fatConsumed,
            fatTarget = fatTarget,
            steps = steps,
            stepsTarget = stepsTarget,
            isMascotTyping = isMascotTyping,
            weightText = weightText,
            onWeightClick = onWeightClick,
            onStepClick = onStepClick,
            modifier = modifier
        )
        return
    }

    val isBento = MaterialTheme.isBentoCraft
    if (isBento) {
        BentoHeroHeader(
            remainingCalories = remainingCalories,
            targetCalories = targetCalories,
            loggedCalories = loggedCalories,
            burnedCalories = burnedCalories,
            proteinConsumed = proteinConsumed,
            proteinTarget = proteinTarget,
            carbsConsumed = carbsConsumed,
            carbsTarget = carbsTarget,
            fatConsumed = fatConsumed,
            fatTarget = fatTarget,
            steps = steps,
            stepsTarget = stepsTarget,
            isMascotTyping = isMascotTyping,
            weightText = weightText,
            onWeightClick = onWeightClick,
            onStepClick = onStepClick,
            modifier = modifier
        )
        return
    }

    val isNeo = MaterialTheme.isNeoBrutalist
    if (isNeo) {
        NeoBrutalHeroHeader(
            remainingCalories = remainingCalories,
            targetCalories = targetCalories,
            loggedCalories = loggedCalories,
            burnedCalories = burnedCalories,
            proteinConsumed = proteinConsumed,
            proteinTarget = proteinTarget,
            carbsConsumed = carbsConsumed,
            carbsTarget = carbsTarget,
            fatConsumed = fatConsumed,
            fatTarget = fatTarget,
            steps = steps,
            stepsTarget = stepsTarget,
            isMascotTyping = isMascotTyping,
            weightText = weightText,
            onWeightClick = onWeightClick,
            onStepClick = onStepClick,
            modifier = modifier
        )
        return
    }
    val cardShape = if (isNeo) RoundedCornerShape(14.dp) else RoundedCornerShape(24.dp)
    val cardBorder = if (isNeo) BorderStroke(2.5.dp, Color.Black) else BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "dashboard_hero_card" }
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 5.dp, color = Color.Black, cornerRadius = 14.dp)
                else Modifier.shadow(
                    elevation = 8.dp,
                    shape = cardShape,
                    spotColor = Color.Black.copy(alpha = 0.22f),
                    ambientColor = Color.Black.copy(alpha = 0.12f)
                )
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = cardBorder
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
                                .clip(if (isNeo) RoundedCornerShape(6.dp) else RoundedCornerShape(99.dp))
                                .background(
                                    if (isNeo) {
                                        if (remainingCalories < 0) Color(0xFFFF8A80) else Color(0xFFB3FCE0)
                                    } else {
                                        if (remainingCalories < 0) AppleRed.copy(alpha = 0.15f)
                                        else AppleGreen.copy(alpha = 0.15f)
                                    }
                                )
                                .then(
                                    if (isNeo) Modifier.border(1.5.dp, Color.Black, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = if (remainingCalories >= 0) "RIMASTE" else "IN ECCESSO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isNeo) Color.Black else (if (remainingCalories < 0) AppleRed else AppleGreen)
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
                val mascotShape = if (isNeo) RoundedCornerShape(10.dp) else RoundedCornerShape(22.dp)
                val mascotBorder = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                val mascotBg = if (isNeo) NeoBrutalAmber else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)

                // FoodLab Mascot in styled container
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .then(
                            if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 10.dp)
                            else Modifier
                        )
                        .clip(mascotShape)
                        .background(mascotBg)
                        .border(mascotBorder.width, mascotBorder.brush, mascotShape),
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
            val stepsShape = if (isNeo) RoundedCornerShape(10.dp) else RoundedCornerShape(16.dp)
            val stepsBorder = if (isNeo) BorderStroke(1.5.dp, Color.Black) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            val stepsBg = if (isNeo) Color(0xFFF7F5ED) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .then(
                        if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 10.dp)
                        else Modifier
                    )
                    .applePressEffect(pressedScale = 0.98f, onClick = onStepClick)
                    .clip(stepsShape)
                    .background(stepsBg)
                    .border(
                        stepsBorder.width,
                        stepsBorder.brush,
                        stepsShape
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

private val DEFAULT_MEAL_CATEGORIES = listOf(
    MealCategoryInfo(MealTypes.BREAKFAST, "Colazione", Icons.Default.EggAlt, Color(0xFF22C55E)),
    MealCategoryInfo(MealTypes.LUNCH, "Pranzo", Icons.Default.LunchDining, Color(0xFF3B82F6)),
    MealCategoryInfo(MealTypes.DINNER, "Cena", Icons.Default.DinnerDining, Color(0xFFA855F7)),
    MealCategoryInfo(MealTypes.SNACK, "Snack", Icons.Default.Icecream, Color(0xFFF59E0B))
)

private data class BentoMealTheme(
    val mealBg: Color,
    val itemTextColor: Color,
    val itemSecondaryColor: Color,
    val dividerColor: Color,
    val macroContentColor: Color,
    val macroContainerColor: Color?,
    val macroBorderColor: Color?,
    val emptyTextColor: Color,
    val emptyIconColor: Color,
    val addBtnBg: Color,
    val addBtnTint: Color,
    val headerIconBg: Color,
    val headerIconTint: Color,
    val badge: @Composable () -> Unit
)

@Composable
fun UnifiedMealsCard(
    mealsByType: Map<String, List<MealEntry>>,
    favoriteFoods: List<FoodItem> = emptyList(),
    onAddMealClick: (String) -> Unit,
    onMealClick: (MealEntry) -> Unit,
    onQuickLog: ((FoodItem) -> Unit)? = null,
    onDeleteMeal: ((MealEntry) -> Unit)? = null,
    modifier: Modifier = Modifier,
    dragDropState: MealDragDropState? = null
) {
    val haptic = LocalHapticFeedback.current
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isNothing) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                }
                Text(
                    "PASTI DI OGGI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isNothing) 1.2.sp else 0.3.sp,
                        fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                    ),
                    color = if (isNothing) NothingTextDisplay else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (favoriteFoods.isNotEmpty() && onQuickLog != null) {
            QuickFavoritesRow(
                favoriteFoods = favoriteFoods,
                onQuickLog = onQuickLog
            )
        }

        DEFAULT_MEAL_CATEGORIES.forEach { (typeKey, title, icon, accentColor) ->
            val meals = mealsByType[typeKey] ?: emptyList()
            val totalCals = meals.sumOf { it.calories }
            val isHoveredTarget = dragDropState?.hoveredMealType == typeKey && dragDropState?.sourceMealType != typeKey

            val bentoStyle = when (typeKey) {
                MealTypes.BREAKFAST -> BentoMealTheme(
                    mealBg = BentoSurface,
                    itemTextColor = BentoCocoa,
                    itemSecondaryColor = BentoCocoa.copy(alpha = 0.70f),
                    dividerColor = BentoBorder.copy(alpha = 0.12f),
                    macroContentColor = BentoCocoa,
                    macroContainerColor = null,
                    macroBorderColor = null,
                    emptyTextColor = BentoCocoa.copy(alpha = 0.75f),
                    emptyIconColor = BentoBerryPink,
                    addBtnBg = BentoBerryPink,
                    addBtnTint = Color.White,
                    headerIconBg = BentoSurfaceContainerHigh,
                    headerIconTint = BentoCocoa,
                    badge = { BentoStickerBadge("Small Batch", icon = Icons.Default.Cookie, backgroundColor = BentoSurfaceContainerHigh, textColor = BentoCocoa) }
                )
                MealTypes.LUNCH -> BentoMealTheme(
                    mealBg = BentoCocoa,
                    itemTextColor = Color.White,
                    itemSecondaryColor = Color(0xFFF6EDE2).copy(alpha = 0.85f),
                    dividerColor = Color.White.copy(alpha = 0.15f),
                    macroContentColor = Color.White,
                    macroContainerColor = Color.White.copy(alpha = 0.16f),
                    macroBorderColor = Color.White.copy(alpha = 0.25f),
                    emptyTextColor = Color.White.copy(alpha = 0.85f),
                    emptyIconColor = BentoBerryPink,
                    addBtnBg = BentoBerryPink,
                    addBtnTint = Color.White,
                    headerIconBg = BentoCocoa,
                    headerIconTint = Color.White,
                    badge = { BentoStickerBadge("100% Real", icon = Icons.Default.EggAlt, backgroundColor = BentoBerryPink, textColor = Color.White) }
                )
                MealTypes.DINNER -> BentoMealTheme(
                    mealBg = BentoBerryPink,
                    itemTextColor = Color.White,
                    itemSecondaryColor = Color(0xFFFFE4EC).copy(alpha = 0.88f),
                    dividerColor = Color.White.copy(alpha = 0.20f),
                    macroContentColor = Color.White,
                    macroContainerColor = Color.White.copy(alpha = 0.18f),
                    macroBorderColor = Color.White.copy(alpha = 0.30f),
                    emptyTextColor = Color.White.copy(alpha = 0.90f),
                    emptyIconColor = Color.White,
                    addBtnBg = BentoCocoa,
                    addBtnTint = Color.White,
                    headerIconBg = BentoBerryPink,
                    headerIconTint = Color.White,
                    badge = { BentoStickerBadge("Heart-Made", icon = Icons.Default.Favorite, backgroundColor = BentoCocoa, textColor = Color.White) }
                )
                else -> BentoMealTheme(
                    mealBg = BentoCocoa,
                    itemTextColor = Color.White,
                    itemSecondaryColor = Color(0xFFF6EDE2).copy(alpha = 0.85f),
                    dividerColor = Color.White.copy(alpha = 0.15f),
                    macroContentColor = Color.White,
                    macroContainerColor = Color.White.copy(alpha = 0.16f),
                    macroBorderColor = Color.White.copy(alpha = 0.25f),
                    emptyTextColor = Color.White.copy(alpha = 0.85f),
                    emptyIconColor = BentoBerryPink,
                    addBtnBg = BentoBerryPink,
                    addBtnTint = Color.White,
                    headerIconBg = BentoCocoa,
                    headerIconTint = Color.White,
                    badge = { BentoStickerBadge("Handcrafted", icon = Icons.Default.Icecream, backgroundColor = BentoBerryPink, textColor = Color.White) }
                )
            }

            val cardBorderColor by animateColorAsState(
                targetValue = if (isHoveredTarget) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "targetMealBorderColor"
            )
            val cardBorderWidth by animateDpAsState(
                targetValue = if (isHoveredTarget) 2.dp else 1.dp,
                animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
                label = "targetMealBorderWidth"
            )

            DisposableEffect(typeKey) {
                onDispose {
                    dragDropState?.unregisterMealBounds(typeKey)
                }
            }

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
                        if (isNothing) {
                            val mealIndexCode = when (typeKey) {
                                MealTypes.BREAKFAST -> "01 // "
                                MealTypes.LUNCH -> "02 // "
                                MealTypes.DINNER -> "03 // "
                                else -> "04 // "
                            }
                            Text(
                                mealIndexCode,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingTextDisabled
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(if (isBento || isNothing) RoundedCornerShape(99.dp) else RoundedCornerShape(8.dp))
                                .background(if (isNothing) NothingSurfaceRaised else if (isBento) bentoStyle.headerIconBg else accentColor.copy(alpha = 0.16f))
                                .then(if (isNothing) Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(99.dp)) else if (isBento) Modifier.border(1.2.dp, BentoBorder, RoundedCornerShape(99.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = if (isNothing) (if (meals.isNotEmpty()) NothingRed else NothingTextDisplay) else if (isBento) bentoStyle.headerIconTint else accentColor, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                            ),
                            color = if (isNothing) NothingTextDisplay else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
                        )
                        if (meals.isNotEmpty()) {
                            if (isNothing) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                )
                            }
                            Text(
                                "(${meals.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNothing) NothingTextSecondary else if (isBento) BentoCocoa.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isBento) {
                            bentoStyle.badge()
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (meals.isNotEmpty()) "$totalCals KCAL" else "0 KCAL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                            ),
                            color = if (isNothing) (if (meals.isNotEmpty()) NothingTextDisplay else NothingTextDisabled) else if (isBento) (if (meals.isNotEmpty()) BentoBerryPink else BentoCocoa.copy(alpha = 0.6f)) else (if (meals.isNotEmpty()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        val addBtnShape = when {
                            isNothing -> RoundedCornerShape(999.dp)
                            isNeo -> RoundedCornerShape(8.dp)
                            isBento -> RoundedCornerShape(99.dp)
                            else -> RoundedCornerShape(20.dp)
                        }
                        val addBtnBg = when {
                            isNothing -> NothingTextDisplay
                            isNeo -> NeoBrutalAmber
                            isBento -> bentoStyle.addBtnBg
                            else -> accentColor.copy(alpha = 0.15f)
                        }
                        val addBtnBorder = when {
                            isNothing -> BorderStroke(1.dp, NothingBorderVisible)
                            isNeo -> BorderStroke(1.5.dp, Color.Black)
                            isBento -> BorderStroke(1.5.dp, BentoBorder)
                            else -> BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                        }
                        val addBtnTint = when {
                            isNothing -> NothingBlack
                            isNeo -> Color.Black
                            isBento -> bentoStyle.addBtnTint
                            else -> accentColor
                        }

                        Box(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .then(
                                    if (isNothing) Modifier
                                    else if (isNeo) Modifier.neoHardShadow(offset = 2.5.dp, color = Color.Black, cornerRadius = 8.dp)
                                    else if (isBento) Modifier.shadow(elevation = 3.dp, shape = addBtnShape, spotColor = BentoCocoa.copy(alpha = 0.15f))
                                    else Modifier
                                )
                                .applePressEffect(pressedScale = 0.92f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddMealClick(typeKey)
                                }
                                .clip(addBtnShape)
                                .background(addBtnBg)
                                .border(addBtnBorder.width, addBtnBorder.brush, addBtnShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = addBtnTint, modifier = Modifier.size(14.dp))
                                Text("Aggiungi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isNothing || isNeo || isBento) FontWeight.Black else FontWeight.Bold, fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null), color = addBtnTint)
                            }
                        }
                    }
                }

                // Inset Grouped Container
                val mealCardShape = when {
                    isNothing -> RoundedCornerShape(14.dp)
                    isNeo -> RoundedCornerShape(12.dp)
                    isBento -> RoundedCornerShape(22.dp)
                    else -> RoundedCornerShape(24.dp)
                }
                val mealBorderWidth = when {
                    isNothing -> 1.dp
                    isNeo -> 2.dp
                    isBento -> 1.8.dp
                    else -> cardBorderWidth
                }
                val mealBorderColor = when {
                    isNothing -> if (isHoveredTarget) NothingRed else NothingBorderVisible
                    isNeo -> Color.Black
                    isBento -> if (isHoveredTarget) accentColor else if (bentoStyle.mealBg == BentoCocoa) Color(0xFF5A3D31) else BentoBorder
                    else -> cardBorderColor
                }
                val mealContainer = when {
                    isNothing -> NothingSurface
                    isNeo -> Color.White
                    isBento -> bentoStyle.mealBg
                    else -> MaterialTheme.colorScheme.surfaceContainer
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            dragDropState?.registerMealBounds(typeKey, coordinates.boundsInRoot())
                        }
                        .then(
                            if (isNothing) Modifier
                            else if (isNeo) Modifier.neoHardShadow(offset = 4.dp, color = Color.Black, cornerRadius = 12.dp)
                            else if (isBento) Modifier.shadow(
                                elevation = if (isHoveredTarget) 10.dp else 4.dp,
                                shape = mealCardShape,
                                spotColor = if (isHoveredTarget) accentColor.copy(alpha = 0.35f) else BentoCocoa.copy(alpha = 0.12f),
                                ambientColor = BentoCocoa.copy(alpha = 0.08f)
                            )
                            else Modifier.shadow(
                                elevation = if (isHoveredTarget) 10.dp else 4.dp,
                                shape = mealCardShape,
                                spotColor = if (isHoveredTarget) accentColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f),
                                ambientColor = Color.Black.copy(alpha = 0.10f)
                            )
                        )
                        .border(
                            mealBorderWidth,
                            mealBorderColor,
                            mealCardShape
                        ),
                    shape = mealCardShape,
                    colors = CardDefaults.cardColors(containerColor = mealContainer)
                ) {
                    if (meals.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            meals.forEachIndexed { index, meal ->
                                key(meal.id) {
                                    var isDeleted by remember(meal.id) { mutableStateOf(false) }
                                    AnimatedVisibility(
                                        visible = !isDeleted,
                                        exit = scaleOut(
                                            targetScale = 0.85f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + shrinkVertically(
                                            animationSpec = appleSpring(dampingRatio = 0.85f, stiffness = 400f)
                                        ) + fadeOut(
                                            animationSpec = tween(durationMillis = 150)
                                        )
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            MealItemRow(
                                                meal = meal,
                                                accentColor = accentColor,
                                                onClick = { onMealClick(meal) },
                                                onDelete = {
                                                    isDeleted = true
                                                    onDeleteMeal?.invoke(meal)
                                                },
                                                dragDropState = dragDropState,
                                                textColor = if (isNothing) NothingTextPrimary else if (isBento) bentoStyle.itemTextColor else null,
                                                secondaryTextColor = if (isNothing) NothingTextSecondary else if (isBento) bentoStyle.itemSecondaryColor else null,
                                                rowBackgroundColor = if (isNothing || isBento) Color.Transparent else null,
                                                macroContentColor = if (isNothing) NothingTextDisplay else if (isBento) bentoStyle.macroContentColor else null,
                                                macroContainerColor = if (isNothing) NothingSurfaceRaised else if (isBento) bentoStyle.macroContainerColor else null,
                                                macroBorderColor = if (isNothing) NothingBorderVisible else if (isBento) bentoStyle.macroBorderColor else null
                                            )
                                            if (index < meals.lastIndex) {
                                                HorizontalDivider(
                                                    color = if (isNothing) NothingBorder else if (isBento) bentoStyle.dividerColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(start = 28.dp, end = 14.dp)
                                                )
                                            }
                                        }
                                    }
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
                                tint = if (isNothing) NothingRed else if (isBento) bentoStyle.emptyIconColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Nessun alimento registrato • Tocca per aggiungere",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isNothing || isBento) FontWeight.Medium else FontWeight.Normal, fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null),
                                color = if (isNothing) NothingTextDisabled else if (isBento) bentoStyle.emptyTextColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (isBento && typeKey == MealTypes.SNACK) {
                        BakeryAwningStripes(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                        )
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
    val isNeo = MaterialTheme.isNeoBrutalist
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
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

fun mealAccentColor(mealType: String): Color = when (com.example.util.MealTypes.canonicalize(mealType)) {
    com.example.util.MealTypes.BREAKFAST -> Color(0xFF22C55E)
    com.example.util.MealTypes.LUNCH -> Color(0xFF3B82F6)
    com.example.util.MealTypes.SNACK -> Color(0xFFF59E0B)
    com.example.util.MealTypes.DINNER -> Color(0xFFA855F7)
    else -> Color(0xFF22C55E)
}

fun mealIcon(mealType: String): ImageVector = when (com.example.util.MealTypes.canonicalize(mealType)) {
    com.example.util.MealTypes.BREAKFAST -> Icons.Default.EggAlt
    com.example.util.MealTypes.LUNCH -> Icons.Default.LunchDining
    com.example.util.MealTypes.SNACK -> Icons.Default.Icecream
    com.example.util.MealTypes.DINNER -> Icons.Default.DinnerDining
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val cardShape = if (isNeo) RoundedCornerShape(12.dp) else RoundedCornerShape(22.dp)
    val cardBorder = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    val cardBg = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 4.dp, color = Color.Black, cornerRadius = 12.dp)
                else Modifier
            )
            .clip(cardShape)
            .background(cardBg)
            .border(cardBorder.width, cardBorder.brush, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("MACRONUTRIENTI", style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$calories kcal", style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold), color = accent)
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
    val isNeo = MaterialTheme.isNeoBrutalist
    val pillShape = if (isNeo) RoundedCornerShape(8.dp) else RoundedCornerShape(16.dp)
    val pillBorder = if (isNeo) BorderStroke(1.5.dp, Color.Black) else BorderStroke(1.dp, color.copy(alpha = 0.35f))
    val pillBg = if (isNeo) color.copy(alpha = 0.22f) else color.copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .background(pillBg, pillShape)
            .border(pillBorder.width, pillBorder.brush, pillShape)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            String.format(java.util.Locale.getDefault(), "%.1fg", value),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isNeo) FontWeight.Black else FontWeight.Bold),
            color = if (isNeo) Color.Black else color,
            maxLines = 1,
            softWrap = false
        )
        Text(
            label.uppercase(java.util.Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isNeo) FontWeight.Bold else FontWeight.SemiBold),
            color = if (isNeo) Color(0xFF333333) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
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
    return com.example.util.TargetResolver.resolveTargetCalories(date, targets, defaultTarget)
}
