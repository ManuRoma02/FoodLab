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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
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
    onNavigateToCreateFood: (String, String) -> Unit,
    onNavigateToCreateRecipe: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
    var manualBarcodeQuery by remember { mutableStateOf("") }
    var scanLoading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var selectedFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("TUTTI") }
    BackHandler(enabled = mealToEdit != null) { mealToEdit = null }
    BackHandler(enabled = showScannerDialog) {
        showScannerDialog = false
        scanLoading = false
        scanError = null
    }

    fun lookupBarcode(code: String) {
        val clean = code.trim()
        if (clean.isBlank()) return
        scanLoading = true
        scanError = null
        scope.launch {
            val result = viewModel.fetchProductByBarcode(clean)
            scanLoading = false
            when (result) {
                is BarcodeLookupResult.Success -> {
                    soundManager.playSound("power_up")
                    showScannerDialog = false
                    onFoodSelected(result.food.id)
                }
                BarcodeLookupResult.NotFound -> {
                    soundManager.playSound("error")
                    manualBarcodeQuery = clean
                    scanError = "Nessun alimento trovato per il codice: $clean. Puoi cercarlo per nome o crearlo manualmente."
                    showScannerDialog = true
                }
                is BarcodeLookupResult.NetworkError -> {
                    soundManager.playSound("error")
                    manualBarcodeQuery = clean
                    scanError = result.message
                    showScannerDialog = true
                }
            }
        }
    }

    fun startCameraScan() {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128
                )
                .enableAutoZoom()
                .build()
            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        lookupBarcode(rawValue)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(SCREEN_TAG, "GMS barcode scan failed or cancelled", e)
                    val message = e.message.orEmpty()
                    if (message.contains("cancel", ignoreCase = true) || message.contains("16", ignoreCase = true)) {
                        // User cancelled scan normally, do not display error dialog
                    } else {
                        scanError = "Scansione con fotocamera non disponibile. Puoi inserire il codice a barre manualmente qui sotto."
                        showScannerDialog = true
                    }
                }
        } catch (e: Exception) {
            Log.w(SCREEN_TAG, "GMS scanner initiation failed", e)
            scanError = "Scansione non supportata su questo dispositivo. Inserisci il codice a barre manualmente."
            showScannerDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
            FoodLabScreenHeader(
                title = "Aggiungi alimento",
                subtitle = "Aggiunta a ${MealTypes.displayName(mealType)}",
                onBack = onNavigateBack
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca alimento, marca, codice a barre...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    soundManager.playSound("click")
                                    viewModel.updateSearchQuery("")
                                },
                                modifier = Modifier.applePressEffect(pressedScale = 0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancella ricerca",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                soundManager.playSound("click")
                                scanError = null
                                startCameraScan()
                            },
                            modifier = Modifier.applePressEffect(pressedScale = 0.85f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scansiona codice a barre",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                        text = if (searchQuery.isBlank()) "Digita almeno 2 caratteri per la ricerca online."
                        else if (isOffline) "Modalità offline attiva. Mostrati solo gli alimenti locali salvati."
                        else searchError ?: "Ricerca tra i cibi salvati e Open Food Facts.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (searchError != null || isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories
            val filterOptions = listOf("TUTTI", "I MIEI CIBI", "RICETTE", "PREFERITI", "COMBINAZIONI")
            Text("FILTRI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "I MIEI CIBI" -> food.id > 0 && food.brand != "Recipe"
                        "RICETTE" -> food.brand == "Recipe"
                        "PREFERITI" -> food.isFavorite
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
                    text = if (searchQuery.isBlank()) "RECENTI E PREFERITI" else "${displayedResults.size} RISULTATI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (searchQuery.isBlank()) "Subito disponibili" else "Ordinati per pertinenza",
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
                                text = "REGISTRATI IN ${mealType.uppercase()}", 
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
                                                    text = "${meal.quantityGrams}g  •  ${meal.calories} kcal  •  P: ${meal.protein.toInt()}g C: ${meal.carbs.toInt()}g F: ${meal.fat.toInt()}g", 
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
                            text = if (searchQuery.isBlank()) "ALIMENTI, RECENTI E PREFERITI" else "RISULTATI RICERCA",
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
                if (selectedFilter != "COMBINAZIONI") {
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
                                        onNavigateToCreateFood("", "")
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
                                        "+ Nuovo Alimento",
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
                                        "+ Ricetta",
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedFilter == "COMBINAZIONI") {
                    if (loggedMeals.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundManager.playSound("click")
                                        templateNameInput = "Combinazione ${MealTypes.displayName(mealType)} ${templates.size + 1}"
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
                                        "SALVA ${MealTypes.displayName(mealType).uppercase()} COME COMBINAZIONE",
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
                                        "NESSUNA COMBINAZIONE SALVATA",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    if (loggedMeals.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Aggiungi degli alimenti per salvarli insieme come combinazione rapida.",
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
                                            Text("${templateFoodList.size} alimenti  •  ${templateFoodList.sumOf { (it.caloriesPer100g * it.quantityGrams) / 100 }} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                soundManager.playSound("error")
                                                viewModel.deleteTemplate(t.id)
                                            }, modifier = Modifier.size(48.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Elimina combinazione", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                            Button(
                                                onClick = {
                                                    soundManager.playSound("coin")
                                                    viewModel.logTemplate(templateWithItems, currentDate, mealType)
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                            ) {
                                                Text("AGGIUNGI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                                                Text("• ${item.foodName} (${item.quantityGrams}g)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$cals kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if ((isOffline || searchError != null) && searchQuery.isNotBlank()) {
                        item(key = "search_offline_banner") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "RICERCA ONLINE NON DISPONIBILE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = searchError ?: "Connessione assente. Risultati mostrati dal database locale.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.updateSearchQuery(searchQuery) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Riprova", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

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
                    }

                    if (displayedResults.isEmpty() && !isSearchLoading) {
                        item(key = "search_empty_state") {
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
                                        if (searchQuery.isBlank()) "Nessun alimento salvato" else "Nessun risultato trovato per \"$searchQuery\".",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        val isBarcode = searchQuery.isNotBlank() && searchQuery.all { it.isDigit() }
                                        val barcodeParam = if (isBarcode) searchQuery.trim() else ""
                                        val nameParam = if (!isBarcode) searchQuery.trim() else ""
                                        onNavigateToCreateFood(barcodeParam, nameParam)
                                    }) {
                                        Text("CREA ALIMENTO")
                                    }
                                }
                            }
                        }
                    } else if (displayedResults.isNotEmpty()) {
                        items(items = displayedResults, key = { it.id }) { food ->
                            SearchResultCard(
                                food = food,
                                showAddIcon = true,
                                onClick = { 
                                    soundManager.playSound("click")
                                    onFoodSelected(food.id)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(food) },
                                onAddClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    soundManager.playSound("coin")
                                    viewModel.quickLogFood(food, currentDate, mealType)
                                    android.widget.Toast.makeText(context, "${food.name} aggiunto a ${mealType.lowercase()}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    mealToEdit?.let { meal ->
        EditMealEntryDialog(
            meal = meal,
            onDismiss = { mealToEdit = null },
            onSave = { newGrams ->
                dashboardViewModel.updateMealQuantity(meal, newGrams)
                viewModel.updateMealQuantity(meal, newGrams)
                android.widget.Toast.makeText(context, "Quantità aggiornata a ${newGrams}g", android.widget.Toast.LENGTH_SHORT).show()
                mealToEdit = null
            },
            onDelete = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                dashboardViewModel.deleteMealEntry(meal.id)
                viewModel.deleteMeal(meal.id)
                android.widget.Toast.makeText(context, "${meal.foodName} rimosso", android.widget.Toast.LENGTH_SHORT).show()
                mealToEdit = null
            }
        )
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
                    Text("SALVA COME COMBINAZIONE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Salva questi ${loggedMeals.size} alimenti come combinazione da riutilizzare rapidamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        label = { Text("NOME COMBINAZIONE") },
                        placeholder = { Text("es. Colazione Proteica") },
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
                            Text("ANNULLA")
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
                            Text("SALVA")
                        }
                    }
                }
            }
        }
    }

    if (scanLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Ricerca alimento da codice a barre...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showScannerDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showScannerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CODICE A BARRE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showScannerDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (scanError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = scanError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        "Digita o incolla il codice a barre per cercarlo nel database:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualBarcodeQuery,
                        onValueChange = { input ->
                            manualBarcodeQuery = input.filter { it.isDigit() }.take(32)
                        },
                        label = { Text("Codice a barre (EAN / UPC)") },
                        placeholder = { Text("es. 8001234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showScannerDialog = false
                                startCameraScan()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fotocamera", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                if (manualBarcodeQuery.isNotBlank()) {
                                    lookupBarcode(manualBarcodeQuery)
                                }
                            },
                            enabled = manualBarcodeQuery.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cerca", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            showScannerDialog = false
                            val isBarcode = manualBarcodeQuery.isNotBlank() && manualBarcodeQuery.all { it.isDigit() }
                            val barcodeParam = if (isBarcode) manualBarcodeQuery.trim() else ""
                            val nameParam = if (!isBarcode) manualBarcodeQuery.trim() else ""
                            onNavigateToCreateFood(barcodeParam, nameParam)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "+ Crea nuovo alimento manualmente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
