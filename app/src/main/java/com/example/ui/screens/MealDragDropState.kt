package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MealEntry
import com.example.ui.foundation.appleSpring
import com.example.ui.theme.macroColors
import kotlin.math.roundToInt

@Stable
class MealDragDropState {
    var isDragging by mutableStateOf(false)
        private set

    var draggedMeal by mutableStateOf<MealEntry?>(null)
        private set

    var sourceMealType by mutableStateOf<String?>(null)
        private set

    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    var fingerGrabOffset by mutableStateOf(Offset.Zero)
        private set

    var itemWidthPx by mutableStateOf(0f)
        private set

    var itemHeightPx by mutableStateOf(0f)
        private set

    var hoveredMealType by mutableStateOf<String?>(null)
        private set

    val targetBoundsMap = mutableStateMapOf<String, Rect>()

    fun registerMealBounds(mealType: String, bounds: Rect) {
        targetBoundsMap[mealType] = bounds
    }

    fun unregisterMealBounds(mealType: String) {
        targetBoundsMap.remove(mealType)
    }

    fun onDragStart(
        meal: MealEntry,
        sourceMealType: String,
        itemTopLeftInRoot: Offset,
        itemWidthPx: Float,
        itemHeightPx: Float,
        touchPositionInRoot: Offset
    ) {
        this.draggedMeal = meal
        this.sourceMealType = sourceMealType
        this.itemWidthPx = itemWidthPx
        this.itemHeightPx = itemHeightPx
        this.fingerGrabOffset = touchPositionInRoot - itemTopLeftInRoot
        this.dragPosition = touchPositionInRoot
        this.isDragging = true
        updateHoveredMeal(touchPositionInRoot)
    }

    fun onDrag(dragAmount: Offset) {
        dragPosition += dragAmount
        updateHoveredMeal(dragPosition)
    }

    private fun updateHoveredMeal(position: Offset) {
        val hit = targetBoundsMap.entries.firstOrNull { (_, rect) ->
            rect.contains(position)
        }?.key

        hoveredMealType = if (hit != null && hit != sourceMealType) {
            hit
        } else {
            null
        }
    }

    var onDropListener: ((meal: MealEntry, targetMealType: String) -> Unit)? = null

    fun onDragEnd(onDrop: ((meal: MealEntry, targetMealType: String) -> Unit)? = null) {
        val target = hoveredMealType
        val meal = draggedMeal
        if (target != null && meal != null && target != sourceMealType) {
            onDrop?.invoke(meal, target) ?: onDropListener?.invoke(meal, target)
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        isDragging = false
        draggedMeal = null
        sourceMealType = null
        hoveredMealType = null
        dragPosition = Offset.Zero
        fingerGrabOffset = Offset.Zero
    }
}

@Composable
fun rememberMealDragDropState(): MealDragDropState {
    return remember { MealDragDropState() }
}

@Composable
fun GhostMealCard(
    meal: MealEntry,
    dragDropState: MealDragDropState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val topLeft = dragDropState.dragPosition - dragDropState.fingerGrabOffset

    val targetWidth = if (dragDropState.itemWidthPx > 0f) {
        with(density) { dragDropState.itemWidthPx.toDp() }
    } else {
        320.dp
    }

    Box(
        modifier = modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .width(targetWidth)
            .graphicsLayer {
                scaleX = 1.04f
                scaleY = 1.04f
                alpha = 0.95f
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.40f),
                ambientColor = Color.Black.copy(alpha = 0.20f)
            )
            .border(
                1.5.dp,
                accentColor.copy(alpha = 0.90f),
                RoundedCornerShape(20.dp)
            )
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .height(30.dp)
                        .background(accentColor, RoundedCornerShape(99.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.foodName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = " (${meal.quantityGrams}g)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Macro order: Carboidrati -> Proteine -> Grassi
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
                    text = "${meal.calories}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "KCAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = accentColor
                )
            }
        }
    }
}
