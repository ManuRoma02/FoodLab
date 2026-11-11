package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.BentoCocoa
import com.example.ui.theme.BentoBerryPink
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceContainerHigh
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.NeoBrutalAmber
import androidx.compose.foundation.BorderStroke


@Composable
fun FoodsDatabaseScreen(
    viewModel: com.example.ui.FoodSearchViewModel,
    onNavigateToCreateFood: () -> Unit,
    onNavigateToCreateRecipe: () -> Unit,
    onFoodSelected: (Int) -> Unit,
    onNavigateToRecipes: () -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidth >= 600

    var selectedFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("TUTTI") }
    var selectedFoodId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }

    val displayedResults = remember(searchResults, selectedFilter, searchQuery) {
        val filtered = searchResults.filter { food ->
            when (selectedFilter) {
                "I MIEI CIBI" -> food.id > 0 && food.brand != "Recipe"
                "RICETTE" -> food.brand == "Recipe"
                "PREFERITI" -> food.isFavorite
                else -> true
            }
        }
        if (searchQuery.isBlank()) {
            filtered.sortedByDescending { it.useCount }
        } else {
            filtered
        }
    }

    if (isWideScreen) {
        // Dual Pane Layout (Tablet & Foldable)
        val selectedFood by androidx.compose.runtime.produceState<com.example.data.FoodItem?>(initialValue = null, selectedFoodId) {
            val id = selectedFoodId
            value = if (id != null) viewModel.getFoodById(id) else null
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            // Left Pane (42% width)
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                FoodLabScreenHeader(
                    title = "Alimenti",
                    subtitle = "Database e Ricerca"
                )
                Spacer(modifier = Modifier.height(16.dp))

                FoodsListContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    displayedResults = displayedResults,
                    isSearchLoading = isSearchLoading,
                    onNavigateToCreateFood = onNavigateToCreateFood,
                    onNavigateToCreateRecipe = onNavigateToCreateRecipe,
                    onNavigateToRecipes = onNavigateToRecipes,
                    onToggleFavorite = viewModel::toggleFavorite,
                    selectedFoodId = selectedFoodId,
                    onFoodClick = { food -> selectedFoodId = food.id },
                    modifier = Modifier.weight(1f)
                )
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Right Pane (58% width)
            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
            ) {
                val currentDetailFood = selectedFood
                if (currentDetailFood != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FoodLabScreenHeader(
                            title = currentDetailFood.name,
                            subtitle = currentDetailFood.brand.ifEmpty { "Valori nutrizionali per porzione" },
                            modifier = Modifier.padding(16.dp),
                            trailing = {
                                IconButton(onClick = { viewModel.toggleFavorite(currentDetailFood) }) {
                                    Icon(
                                        imageVector = if (currentDetailFood.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Preferito",
                                        tint = if (currentDetailFood.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        FoodDetailCardContent(
                            food = currentDetailFood,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Fastfood,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                "SELEZIONA UN ALIMENTO",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Seleziona un elemento dalla lista a sinistra per visualizzare macronutrienti, calorie e porzioni.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Single Pane Layout (Compact Phones)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .weight(1f)
            ) {
                FoodLabScreenHeader(
                    title = "Alimenti",
                    subtitle = "Cerca, preferiti e piatti personalizzati"
                )
                Spacer(modifier = Modifier.height(16.dp))

                FoodsListContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    displayedResults = displayedResults,
                    isSearchLoading = isSearchLoading,
                    onNavigateToCreateFood = onNavigateToCreateFood,
                    onNavigateToCreateRecipe = onNavigateToCreateRecipe,
                    onNavigateToRecipes = onNavigateToRecipes,
                    onToggleFavorite = viewModel::toggleFavorite,
                    selectedFoodId = null,
                    onFoodClick = { food -> onFoodSelected(food.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FoodsListContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    displayedResults: List<com.example.data.FoodItem>,
    isSearchLoading: Boolean,
    onNavigateToCreateFood: () -> Unit,
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipes: () -> Unit = {},
    onToggleFavorite: (com.example.data.FoodItem) -> Unit,
    selectedFoodId: Int?,
    onFoodClick: (com.example.data.FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
    Column(modifier = modifier) {
        val searchShape = when {
            isNothing -> RoundedCornerShape(12.dp)
            isNeo -> RoundedCornerShape(10.dp)
            isBento -> RoundedCornerShape(99.dp)
            else -> RoundedCornerShape(18.dp)
        }
        val searchBorderColor = when {
            isNothing -> NothingBorderVisible
            isBento -> BentoBorder
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        }
        val searchFocusedBorder = when {
            isNothing -> NothingTextDisplay
            isBento -> BentoBerryPink
            else -> MaterialTheme.colorScheme.primary
        }
        val searchBg = when {
            isNothing -> NothingSurface
            isBento -> BentoSurfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        }

        // Search Input - Apple Spotlight Style
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Cerca alimento, marca, codice a barre...", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null), color = if (isNothing) NothingTextDisabled else if (isBento) BentoCocoa.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca", tint = if (isNothing) NothingRed else if (isBento) BentoBerryPink else MaterialTheme.colorScheme.primary) },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.applePressEffect(pressedScale = 0.96f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancella ricerca",
                            tint = if (isNothing) NothingTextSecondary else if (isBento) BentoCocoa.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = searchFocusedBorder,
                unfocusedBorderColor = searchBorderColor,
                focusedContainerColor = searchBg,
                unfocusedContainerColor = searchBg,
                focusedTextColor = if (isNothing) NothingTextDisplay else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (isNothing) NothingTextPrimary else if (isBento) BentoCocoa else MaterialTheme.colorScheme.onSurface
            ),
            shape = searchShape
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Categories Segmented Selector (All 4 categories on screen at once, no cutoffs)
        val filterOptions = listOf(
            "TUTTI" to "Tutti",
            "I MIEI CIBI" to "Miei Cibi",
            "RICETTE" to "Ricette",
            "PREFERITI" to "Preferiti"
        )
        val containerShape = when {
            isNothing -> RoundedCornerShape(12.dp)
            isNeo -> RoundedCornerShape(10.dp)
            isBento -> RoundedCornerShape(99.dp)
            else -> RoundedCornerShape(12.dp)
        }
        val containerBg = when {
            isNothing -> NothingSurface
            isNeo -> Color.White
            isBento -> BentoSurfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val containerBorder = when {
            isNothing -> BorderStroke(1.dp, NothingBorderVisible)
            isNeo -> BorderStroke(2.dp, Color.Black)
            isBento -> BorderStroke(1.2.dp, BentoBorder)
            else -> null
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(containerBg)
                .then(
                    if (containerBorder != null) Modifier.border(containerBorder.width, containerBorder.brush, containerShape)
                    else Modifier
                )
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            filterOptions.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                val pillShape = when {
                    isNothing -> RoundedCornerShape(8.dp)
                    isNeo -> RoundedCornerShape(8.dp)
                    isBento -> RoundedCornerShape(99.dp)
                    else -> RoundedCornerShape(10.dp)
                }
                val bg = if (isSelected) {
                    when {
                        isNothing -> NothingTextDisplay
                        isNeo -> NeoBrutalAmber
                        isBento -> BentoBerryPink
                        else -> MaterialTheme.colorScheme.primary
                    }
                } else Color.Transparent
                val fg = if (isSelected) {
                    when {
                        isNothing -> NothingBlack
                        isNeo -> Color.Black
                        isBento -> Color.White
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                } else if (isNothing) NothingTextSecondary else if (isBento) BentoCocoa.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillShape)
                        .background(bg)
                        .then(
                            if (isSelected && isNothing) Modifier.border(1.dp, NothingBorderVisible, pillShape)
                            else if (isSelected && isNeo) Modifier.border(1.5.dp, Color.Black, pillShape)
                            else Modifier
                        )
                        .clickable {
                            onFilterChange(key)
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            fontFamily = if (isNothing) androidx.compose.ui.text.font.FontFamily.Monospace else null
                        ),
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FoodLabSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ALIMENTI E RICETTE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                        Text("${displayedResults.size}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FoodLabActionButton("+ Alimento", Icons.Default.Restaurant, onNavigateToCreateFood, Modifier.weight(1f))
                        FoodLabActionButton("Ricette", Icons.AutoMirrored.Filled.MenuBook, onNavigateToRecipes, Modifier.weight(1f))
                    }
                }
            }

            if (isSearchLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (displayedResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NESSUN ALIMENTO TROVATO", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(items = displayedResults, key = { it.id }) { food ->
                    val isSelected = selectedFoodId != null && selectedFoodId == food.id
                    val itemModifier = if (isSelected) {
                        Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    Box(modifier = itemModifier) {
                        SearchResultCard(
                            food = food,
                            showAddIcon = false,
                            onClick = { onFoodClick(food) },
                            onToggleFavorite = { onToggleFavorite(food) }
                        )
                    }
                }
            }
        }
    }
}


