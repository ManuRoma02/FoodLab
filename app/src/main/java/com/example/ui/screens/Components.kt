package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.macroColors
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.Routes
import com.example.ui.foundation.appleSpring
import com.example.util.LocalSoundManager
import kotlinx.coroutines.launch


import com.example.ui.theme.isNeoBrutalist
import com.example.ui.theme.isBentoCraft
import com.example.ui.theme.BentoCocoa
import com.example.ui.theme.BentoBerryPink
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.NeoBrutalAmber
import com.example.ui.theme.neoHardShadow
import com.example.ui.theme.neoBorder
import com.example.ui.theme.foodLabCardModifier

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val selectedIndex = when {
        currentRoute == Routes.Dashboard -> 0
        currentRoute == Routes.FoodsDatabase ||
                currentRoute?.startsWith("foods_database_detail") == true ||
                currentRoute == Routes.CreateFood -> 1
        currentRoute == Routes.Stats || currentRoute == Routes.Analytics -> 2
        currentRoute == Routes.Settings -> 3
        else -> 0
    }

    val navShape = when {
        isNeo -> RoundedCornerShape(14.dp)
        isBento -> RoundedCornerShape(26.dp)
        else -> RoundedCornerShape(32.dp)
    }
    val navColor = when {
        isNeo -> Color.White
        isBento -> BentoSurface
        else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
    }
    val navBorder = when {
        isNeo -> BorderStroke(2.5.dp, Color.Black)
        isBento -> BorderStroke(2.dp, BentoBorder)
        else -> BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isNeo) Modifier.neoHardShadow(offset = 5.dp, color = Color.Black, cornerRadius = 14.dp)
                    else if (isBento) Modifier.shadow(
                        elevation = 8.dp,
                        shape = navShape,
                        spotColor = BentoCocoa.copy(alpha = 0.16f),
                        ambientColor = BentoCocoa.copy(alpha = 0.08f)
                    )
                    else Modifier.shadow(
                        elevation = 16.dp,
                        shape = navShape,
                        spotColor = Color.Black.copy(alpha = 0.40f),
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                ),
            shape = navShape,
            color = navColor,
            border = navBorder
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / 4

                val animatedIndex by animateFloatAsState(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 380f
                    ),
                    label = "nav_sliding_indicator_index"
                )

                // Sliding active pill behind the navigation items with cartoon bouncy spring
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(48.dp)
                        .graphicsLayer {
                            translationX = animatedIndex * tabWidth.toPx()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isNeo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeoBrutalAmber)
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                        )
                    } else if (isBento) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(BentoBerryPink)
                                .border(1.5.dp, BentoBorder, RoundedCornerShape(20.dp))
                        )
                    } else {
                        // Pill container background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                        // Sliding active bar at bottom of the active tab
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .width(22.dp)
                                .height(3.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(99.dp)
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavBarItem(
                        icon = Icons.Default.DateRange,
                        label = "Diario",
                        isSelected = selectedIndex == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playSound("click")
                        onNavigateToRoute(Routes.Dashboard)
                    }
                    NavBarItem(
                        icon = Icons.Default.Restaurant,
                        label = "Alimenti",
                        isSelected = selectedIndex == 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playSound("click")
                        onNavigateToRoute(Routes.FoodsDatabase)
                    }
                    NavBarItem(
                        icon = Icons.Default.BarChart,
                        label = "Progressi",
                        isSelected = selectedIndex == 2,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playSound("click")
                        onNavigateToRoute(Routes.Stats)
                    }
                    NavBarItem(
                        icon = Icons.Default.Settings,
                        label = "Impostazioni",
                        isSelected = selectedIndex == 3,
                        modifier = Modifier.weight(1f)
                    ) {
                        soundManager.playSound("click")
                        onNavigateToRoute(Routes.Settings)
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible NavController overload that delegates to the hoisted [BottomNavBar].
 */
@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    BottomNavBar(
        currentRoute = currentRoute,
        onNavigateToRoute = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        modifier = modifier
    )
}

@Composable
fun FoodLabNavigationRail(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    NavigationRail(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Diario") },
            label = { Text("Diario", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Dashboard,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Dashboard) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "Alimenti") },
            label = { Text("Alimenti", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.FoodsDatabase || currentRoute?.startsWith("foods_database_detail") == true || currentRoute == Routes.CreateFood,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.FoodsDatabase) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Progressi") },
            label = { Text("Progressi", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Stats || currentRoute == Routes.Analytics,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Stats) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
            label = { Text("Impostazioni", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Settings,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Settings) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val primaryColor = when {
        isNeo -> Color.Black
        isBento -> Color.White
        else -> MaterialTheme.colorScheme.primary
    }
    val unselectedColor = when {
        isNeo -> Color(0xFF333333)
        isBento -> BentoCocoa.copy(alpha = 0.70f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
    }

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else unselectedColor,
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "nav_item_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.16f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 350f
        ),
        label = "nav_icon_scale"
    )
    val iconBob by animateFloatAsState(
        targetValue = if (isSelected) -3f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 350f
        ),
        label = "nav_icon_bob"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 44.dp)
            .clip(if (isNeo) RoundedCornerShape(10.dp) else RoundedCornerShape(20.dp))
            .applePressEffect(pressedScale = 0.92f, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        translationY = iconBob
                    }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) (if (isNeo) FontWeight.Black else FontWeight.SemiBold) else (if (isNeo) FontWeight.Bold else FontWeight.Normal),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.2.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

fun Modifier.applePressEffect(
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    var isPressed by remember { mutableStateOf(false) }

    // Cartoon squash-and-stretch: down-press compresses along Y and expands along X;
    // release springs back with juicy medium-bouncy overshoot.
    // Animatable values run on the GPU RenderThread via graphicsLayer, completely skipping
    // Recomposition and Layout passes for peak 120 FPS buttery-smooth performance.
    val scaleY = remember { Animatable(1.0f) }
    val scaleX = remember { Animatable(1.0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            launch {
                scaleY.animateTo(
                    targetValue = pressedScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 600f
                    )
                )
            }
            launch {
                scaleX.animateTo(
                    targetValue = 1.0f + (1.0f - pressedScale) * 0.40f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 600f
                    )
                )
            }
        } else {
            launch {
                scaleY.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 380f
                    )
                )
            }
            launch {
                scaleX.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 380f
                    )
                )
            }
        }
    }

    val scaleModifier = this.graphicsLayer {
        this.scaleX = scaleX.value
        this.scaleY = scaleY.value
    }

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val interactionPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(interactionPressed) {
            isPressed = interactionPressed
        }

        scaleModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnClick?.invoke()
            }
        )
    } else {
        // Passive observation mode: monitors pointer state via PointerEventPass.Final
        // without intercepting or consuming scroll gestures or conflicting with child clickables
        scaleModifier.pointerInput(Unit) {
            awaitPointerEventScope {
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            val isScroll = event.changes.any { it.isConsumed }
                            isPressed = !isScroll
                        } else {
                            isPressed = false
                        }
                    }
                } finally {
                    isPressed = false
                }
            }
        }
    }
}

@Composable
fun ImprovedTagButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val targetBg = when {
        isNeo -> if (isSelected) NeoBrutalAmber else Color.White
        isBento -> if (isSelected) BentoBerryPink else BentoSurface
        else -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
    }
    val targetText = when {
        isNeo -> Color.Black
        isBento -> if (isSelected) Color.White else BentoCocoa
        else -> if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val targetBorder = when {
        isNeo -> Color.Black
        isBento -> if (isSelected) BentoBorder else BentoBorder.copy(alpha = 0.35f)
        else -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagBg"
    )
    val animatedText by animateColorAsState(
        targetValue = targetText,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagText"
    )
    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagBorder"
    )
    val tagScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 380f
        ),
        label = "tagScale"
    )

    val tagShape = when {
        isNeo -> RoundedCornerShape(8.dp)
        isBento -> RoundedCornerShape(99.dp)
        else -> RoundedCornerShape(14.dp)
    }
    val borderWidth = if (isNeo || isBento) 1.8.dp else 1.dp

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = tagScale
                scaleY = tagScale
            }
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 3.dp, color = Color.Black, cornerRadius = 8.dp)
                else if (isBento) Modifier.shadow(elevation = 2.dp, shape = tagShape, spotColor = BentoCocoa.copy(alpha = 0.12f))
                else Modifier
            )
            .applePressEffect(pressedScale = 0.93f, onClick = onClick)
            .clip(tagShape)
            .background(animatedBg)
            .border(borderWidth, animatedBorder, tagShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isNeo || isBento) FontWeight.Black else FontWeight.Bold,
                letterSpacing = 0.3.sp
            ),
            color = animatedText
        )
    }
}

@Composable
fun MacroBadge(label: String, value: String, color: Color) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val badgeShape = when {
        isNeo -> RoundedCornerShape(6.dp)
        isBento -> RoundedCornerShape(99.dp)
        else -> RoundedCornerShape(8.dp)
    }
    val bgColor = when {
        isNeo -> color.copy(alpha = 0.25f)
        isBento -> color.copy(alpha = 0.16f)
        else -> color.copy(alpha = 0.12f)
    }
    val borderStroke = when {
        isNeo -> BorderStroke(1.5.dp, Color.Black)
        isBento -> BorderStroke(1.2.dp, BentoBorder)
        else -> BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    }

    Surface(
        shape = badgeShape,
        color = bgColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = when {
                    isNeo -> Color.Black
                    isBento -> BentoCocoa
                    else -> color
                }
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isNeo || isBento) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = when {
                    isNeo -> Color.Black
                    isBento -> BentoCocoa
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SearchResultCard(
    food: com.example.data.FoodItem,
    showAddIcon: Boolean = true,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val cardShape = if (isNeo) RoundedCornerShape(12.dp) else RoundedCornerShape(18.dp)
    val cardBorder = if (isNeo) BorderStroke(2.dp, Color.Black) else BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isNeo) Modifier.neoHardShadow(offset = 4.dp, color = Color.Black, cornerRadius = 12.dp)
                else Modifier
            )
            .applePressEffect(pressedScale = 0.97f, onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isNeo) Color.White else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (food.brand.isNotBlank()) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Line 1: Calories
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = if (isNeo) RoundedCornerShape(6.dp) else RoundedCornerShape(8.dp),
                            color = if (isNeo) NeoBrutalAmber else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = if (isNeo) BorderStroke(1.5.dp, Color.Black) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${food.caloriesPer100g} kcal/100g",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isNeo) FontWeight.Black else FontWeight.ExtraBold,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = if (isNeo) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Line 2: Carbs, Protein & Fats (C -> P -> F)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacroBadge(label = "C", value = "${food.carbsPer100g}g", color = MaterialTheme.macroColors.carbs)
                        MacroBadge(label = "P", value = "${food.proteinPer100g}g", color = MaterialTheme.macroColors.protein)
                        MacroBadge(label = "F", value = "${food.fatPer100g}g", color = MaterialTheme.macroColors.fat)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleFavorite != null) {
                    val heartScale by animateFloatAsState(
                        targetValue = if (food.isFavorite) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 350f
                        ),
                        label = "fav_pop"
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.applePressEffect(pressedScale = 0.96f)
                    ) {
                        Icon(
                            imageVector = if (food.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (food.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                        )
                    }
                }
                if (showAddIcon && onAddClick != null) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.applePressEffect(pressedScale = 0.96f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun calorieOverTargetColor(overCalories: Int, fallback: Color): Color {
    if (overCalories <= 0) return fallback
    val progress = (overCalories / 700f).coerceIn(0f, 1f)
    return androidx.compose.ui.graphics.lerp(Color(0xFFFDE68A), Color(0xFFEF4444), progress)
}
