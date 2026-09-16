package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import com.example.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.DashboardViewModel
import com.example.ui.FoodSearchViewModel
import com.example.ui.PedometerViewModel
import com.example.ui.components.*
import com.example.util.*
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.Locale
import kotlinx.coroutines.launch

private const val SCREEN_TAG = "SearchScreen"

@Composable
fun SearchScreen(
    viewModel: FoodSearchViewModel,
    dashboardViewModel: DashboardViewModel,
    pedometerViewModel: PedometerViewModel,
    mealType: String,
    currentDate: String,
    onNavigateBack: () -> Unit,
    onFoodSelected: (Int) -> Unit,
    onNavigateToCreateFood: () -> Unit,
    onNavigateToCreateRecipe: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = LocalSoundManager.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    val loggedMeals by viewModel.getLoggedMealsForDate(currentDate).collectAsStateWithLifecycle(initialValue = emptyList())

    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }
    var mealToEdit by remember { mutableStateOf<MealEntry?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showScannerDialog by remember { mutableStateOf(false) }
    var scanLoading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var selectedFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("ALL") }
    BackHandler(enabled = mealToEdit != null) { mealToEdit = null }
    BackHandler(enabled = showScannerDialog) {
        showScannerDialog = false
        scanLoading = false
        scanError = null
    }

    fun startCameraScan() {
        try {
            val scanner = GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        soundManager.playSound("coin")
                        scanLoading = true
                        scope.launch {
                            val result = viewModel.fetchProductByBarcode(rawValue)
                            scanLoading = false
                            when (result) {
                                is BarcodeLookupResult.Success -> {
                                    soundManager.playSound("power_up")
                                    showScannerDialog = false
                                    onFoodSelected(result.food.id)
                                }
                                BarcodeLookupResult.NotFound -> {
                                    soundManager.playSound("error")
                                    scanError = "No product was found for that barcode. Try manual search."
                                }
                                is BarcodeLookupResult.NetworkError -> {
                                    soundManager.playSound("error")
                                    scanError = result.message
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(SCREEN_TAG, "GMS barcode scan failed; showing manual fallback", e)
                    soundManager.playSound("error")
                    scanError = "Camera system unavailable. Please use the simulator below."
                }
        } catch (e: Exception) {
            Log.w(SCREEN_TAG, "GMS scanner initiation failed", e)
            scanError = "Camera scan unsupported. Please use the simulator below."
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
            FoodLabScreenHeader(
                title = "Add food",
                subtitle = "Logging to ${mealType.lowercase(Locale.getDefault())}",
                onBack = onNavigateBack
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search food, brand, barcode, recipe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            soundManager.playSound("click")
                            scanError = null
                            showScannerDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan QR/Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                supportingText = {
                    Text(
                        text = if (searchQuery.isBlank()) "Type 2+ chars for online search. Local results rank by match, favorites, and recent use."
                        else if (isOffline) "Offline mode active. Tap 'LOCAL FOODS' card below for saved items."
                        else searchError ?: "Searching local foods and Open Food Facts. Try name, brand, ingredients, or digits.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (searchError != null || isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories
            val filterOptions = listOf("ALL", "MY FOODS", "RECIPES", "FAVORITES", "TEMPLATES")
            Text("FILTERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    ImprovedTagButton(filter, selectedFilter == filter) { selectedFilter = filter }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayedResults = remember(searchResults, selectedFilter) {
                searchResults.filter { food ->
                    when(selectedFilter) {
                        "MY FOODS" -> food.id > 0 && food.brand != "Recipe"
                        "RECIPES" -> food.brand == "Recipe"
                        "FAVORITES" -> food.isFavorite
                        else -> true
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "RECENT & FAVORITES" else "${displayedResults.size} SEARCH RESULTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (searchQuery.isBlank()) "Ready before you type" else "Ranked by match quality",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Single Scrollable Area
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Already Logged Section
                if (loggedMeals.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "LOGGED TO ${mealType.uppercase()}", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            loggedMeals.forEach { meal ->
                                val accent = mealAccentColor(mealType)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { mealToEdit = meal }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left Accent Bar
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(36.dp)
                                                    .background(accent, RoundedCornerShape(16.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = meal.foodName, 
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), 
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${meal.quantityGrams}g  â¢  ${meal.calories} kcal  â¢  P: ${meal.protein.toInt()}g C: ${meal.carbs.toInt()}g F: ${meal.fat.toInt()}g", 
                                                    style = MaterialTheme.typography.labelSmall, 
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { 
                                                    soundManager.playSound("click")
                                                    mealToEdit = meal 
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit, 
                                                    contentDescription = "Edit quantity", 
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Results Container Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "DISHES, RECENT & FAVORITES" else "SEARCH RESULTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${displayedResults.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }

                // Bento-style Custom Dish & Recipe Buttons
                if (selectedFilter != "TEMPLATES") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundManager.playSound("click")
                                        onNavigateToCreateFood()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "+ Custom Dish",
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundManager.playSound("click")
                                        onNavigateToCreateRecipe()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Cookie,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Recipe",
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedFilter == "TEMPLATES") {
                    if (loggedMeals.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundManager.playSound("click")
                                        templateNameInput = "${mealType} Template ${templates.size + 1}"
                                        showSaveTemplateDialog = true
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "SAVE CURRENT ${mealType.uppercase()} AS TEMPLATE",
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (templates.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "NO MEAL TEMPLATES YET",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    if (loggedMeals.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Log some foods first to save them as a combo.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(items = templates, key = { it.template.id }) { templateWithItems ->
                            val t = templateWithItems.template
                            val templateFoodList = templateWithItems.items
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(t.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            Text("${templateFoodList.size} foods  •  ${templateFoodList.sumOf { (it.caloriesPer100g * it.quantityGrams) / 100 }} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                soundManager.playSound("error")
                                                viewModel.deleteTemplate(t.id)
                                            }, modifier = Modifier.size(48.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete template", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                            Button(
                                                onClick = {
                                                    soundManager.playSound("coin")
                                                    viewModel.logTemplate(templateWithItems, currentDate, mealType)
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                            ) {
                                                Text("LOG COMBO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        templateFoodList.forEach { item ->
                                            val cals = (item.caloriesPer100g * item.quantityGrams) / 100
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("â¢ ${item.foodName} (${item.quantityGrams}g)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$cals kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isSearchLoading) {
                        items(3) { index ->
                            val infiniteTransition = rememberInfiniteTransition(label = "skeletonPulse_$index")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.75f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(750, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "skeletonAlpha_$index"
                            )
                            SkeletonSearchCard(alpha = alpha)
                        }
                    } else if ((isOffline || searchError != null) && searchQuery.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "ONLINE SEARCH UNAVAILABLE",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = searchError ?: "Connection error. Open Food Facts unreachable. You can continue searching local foods.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateSearchQuery(searchQuery) },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("RETRY", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Button(
                                            onClick = {
                                                selectedFilter = "MY FOODS"
                                                soundManager.playSound("click")
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.weight(1.3f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                        ) {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("LOCAL FOODS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (displayedResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.foodlab_mascot_duo),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (searchQuery.isBlank()) "No saved foods yet" else "No matches. Try a brand, barcode, or shorter name.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    if (searchQuery.isBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = onNavigateToCreateFood) { Text("CREATE A FOOD") }
                                    }
                                }
                            }
                        }
                    } else {
                        items(items = displayedResults, key = { it.id }) { food ->
                            SearchResultCard(
                                food = food,
                                onToggleFavorite = { viewModel.toggleFavorite(food) },
                                onClick = { 
                                    soundManager.playSound("click")
                                    onFoodSelected(food.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveTemplateDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSaveTemplateDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("SAVE MEAL AS TEMPLATE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Save these ${loggedMeals.size} logged foods as a single reusable template.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        label = { Text("TEMPLATE NAME") },
                        placeholder = { Text("e.g. My Heavy Breakfast") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showSaveTemplateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("CANCEL")
                        }
                        Button(
                            onClick = {
                                if (templateNameInput.isNotBlank()) {
                                    soundManager.playSound("coin")
                                    viewModel.saveMealGroupAsTemplate(templateNameInput.trim(), loggedMeals)
                                    showSaveTemplateDialog = false
                                    templateNameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("SAVE")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonSearchCard(alpha: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                )
            }
        }
    }
}
