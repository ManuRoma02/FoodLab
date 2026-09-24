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


@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
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
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.DateRange,
                    label = "Diario",
                    isSelected = currentRoute == Routes.Dashboard
                ) {
                    soundManager.playSound("click")
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                NavBarItem(
                    icon = Icons.Default.Restaurant,
                    label = "Alimenti",
                    isSelected = currentRoute == Routes.FoodsDatabase ||
                            currentRoute?.startsWith("foods_database_detail") == true ||
                            currentRoute == Routes.CreateFood
                ) {
                    soundManager.playSound("click")
                    navController.navigate(Routes.FoodsDatabase) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                NavBarItem(
                    icon = Icons.Default.BarChart,
                    label = "Progressi",
                    isSelected = currentRoute == Routes.Stats || currentRoute == Routes.Analytics
                ) {
                    soundManager.playSound("click")
                    navController.navigate(Routes.Stats) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                NavBarItem(
                    icon = Icons.Default.Settings,
                    label = "Impostazioni",
                    isSelected = currentRoute == Routes.Settings
                ) {
                    soundManager.playSound("click")
                    navController.navigate(Routes.Settings) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
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
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else unselectedColor,
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "nav_item_color"
    )
    val bgPillColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "nav_item_bg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "nav_icon_scale"
    )

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgPillColor)
            .applePressEffect(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = appleSpring(dampingRatio = 0.75f, stiffness = 300f),
        label = "applePressScale"
    )

    val scaleModifier = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        scaleModifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val released = try {
                        tryAwaitRelease()
                    } catch (_: Exception) {
                        false
                    }
                    isPressed = false
                    if (released) {
                        currentOnClick?.invoke()
                    }
                }
            )
        }
    } else {
        scaleModifier.pointerInput(Unit) {
            awaitPointerEventScope {
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null && !isPressed) {
                            isPressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (isPressed && event.changes.all { !it.pressed }) {
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
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagBg"
    )
    val animatedText by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagText"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tagBorder"
    )

    Box(
        modifier = Modifier
            .applePressEffect(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            ),
            color = animatedText
        )
    }
}

@Composable
fun MacroBadge(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .applePressEffect(pressedScale = 0.97f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${food.caloriesPer100g} kcal/100g",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.1).sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    MacroBadge(label = "P", value = "${food.proteinPer100g}g", color = Color(0xFF22C55E))
                    MacroBadge(label = "C", value = "${food.carbsPer100g}g", color = Color(0xFFF59E0B))
                    MacroBadge(label = "F", value = "${food.fatPer100g}g", color = Color(0xFF06B6D4))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.applePressEffect(pressedScale = 0.85f)
                    ) {
                        Icon(
                            imageVector = if (food.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (food.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showAddIcon && onAddClick != null) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.applePressEffect(pressedScale = 0.85f)
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
