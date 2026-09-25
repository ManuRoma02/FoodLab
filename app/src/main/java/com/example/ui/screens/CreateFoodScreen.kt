package com.example.ui.screens

import com.example.data.BarcodeLookupResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FoodSearchViewModel
import com.example.util.LocalSoundManager
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class FoodInputMode {
    PER_100G,
    TOTAL_DISH
}

@Composable
fun CreateFoodScreen(
    viewModel: FoodSearchViewModel, 
    onNavigateBack: () -> Unit,
    initialBarcode: String = "",
    initialName: String = ""
) {
    val soundManager = LocalSoundManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputMode by remember { mutableStateOf(FoodInputMode.PER_100G) }

    var name by remember(initialName) { mutableStateOf(initialName) }
    var brand by remember { mutableStateOf("") }
    var barcodeText by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var isBarcodeSearching by remember { mutableStateOf(false) }
    var barcodeStatusMessage by remember { mutableStateOf<String?>(null) }

    // Direct per-100g values
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }

    // Total Dish values
    var totalWeightText by remember { mutableStateOf("") }
    var totalCaloriesText by remember { mutableStateOf("") }
    var totalProteinText by remember { mutableStateOf("") }
    var totalCarbsText by remember { mutableStateOf("") }
    var totalFatText by remember { mutableStateOf("") }

    var portionsList by remember { mutableStateOf(listOf<Pair<String, Float>>()) }
    var portionNameInput by remember { mutableStateOf("") }
    var portionWeightInput by remember { mutableStateOf("") }

    val handleBarcodeLookup: (String) -> Unit = { code ->
        val cleanCode = code.filter { it.isDigit() }
        if (cleanCode.isNotBlank()) {
            barcodeText = cleanCode
            isBarcodeSearching = true
            barcodeStatusMessage = "Ricerca in corso per barcode: $cleanCode..."
            scope.launch {
                when (val result = viewModel.fetchProductByBarcode(cleanCode)) {
                    is BarcodeLookupResult.Success -> {
                        val food = result.food
                        name = food.name
                        brand = food.brand
                        caloriesText = food.caloriesPer100g.toString()
                        proteinText = String.format(java.util.Locale.US, "%.1f", food.proteinPer100g)
                        carbsText = String.format(java.util.Locale.US, "%.1f", food.carbsPer100g)
                        fatText = String.format(java.util.Locale.US, "%.1f", food.fatPer100g)
                        inputMode = FoodInputMode.PER_100G
                        barcodeStatusMessage = "✅ Prodotto trovato: ${food.name} (${food.brand})"
                    }
                    BarcodeLookupResult.NotFound -> {
                        barcodeStatusMessage = "⚠️ Nessun cibo trovato per barcode $cleanCode. Inserisci i valori manualmente."
                    }
                    is BarcodeLookupResult.NetworkError -> {
                        barcodeStatusMessage = "❌ Errore di rete. Compila manualmente i valori nutrizionali."
                    }
                }
                isBarcodeSearching = false
            }
        }
    }

    fun startQrScanner() {
        soundManager.playSound("click")
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
                    barcode.rawValue?.let { rawCode ->
                        handleBarcodeLookup(rawCode)
                    }
                }
                .addOnFailureListener {
                    barcodeStatusMessage = "Scansione annullata o non supportata su questo dispositivo."
                }
        } catch (e: Exception) {
            barcodeStatusMessage = "Compila manualmente il codice a barre."
        }
    }

    // Calculated values normalized per 100g
    val finalWeight = totalWeightText.toFloatOrNull() ?: 0f
    val ratio100g = if (finalWeight > 0) 100f / finalWeight else 0f

    val finalCaloriesPer100g = if (inputMode == FoodInputMode.PER_100G) {
        caloriesText.toFloatOrNull()?.roundToInt() ?: 0
    } else {
        ((totalCaloriesText.toFloatOrNull() ?: 0f) * ratio100g).roundToInt()
    }

    val finalProteinPer100g = if (inputMode == FoodInputMode.PER_100G) {
        proteinText.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
    } else {
        ((totalProteinText.toFloatOrNull() ?: 0f) * ratio100g).coerceAtLeast(0f)
    }

    val finalCarbsPer100g = if (inputMode == FoodInputMode.PER_100G) {
        carbsText.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
    } else {
        ((totalCarbsText.toFloatOrNull() ?: 0f) * ratio100g).coerceAtLeast(0f)
    }

    val finalFatPer100g = if (inputMode == FoodInputMode.PER_100G) {
        fatText.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
    } else {
        ((totalFatText.toFloatOrNull() ?: 0f) * ratio100g).coerceAtLeast(0f)
    }

    // Input Validation Logic
    val isNameValid = name.trim().isNotBlank() && name.length <= 160
    val nameErrorMessage = when {
        name.isBlank() && name.isNotEmpty() -> "Il nome del cibo è obbligatorio"
        name.length > 160 -> "Massimo 160 caratteri"
        else -> null
    }

    // Per 100g field errors
    val caloriesVal = caloriesText.toIntOrNull()
    val calError = if (inputMode == FoodInputMode.PER_100G && caloriesText.isNotBlank() && (caloriesVal == null || caloriesVal < 0 || caloriesVal > 900)) {
        "Valore non valido (0 - 900 kcal)"
    } else null

    val proteinVal = proteinText.toFloatOrNull()
    val proteinError = if (inputMode == FoodInputMode.PER_100G && proteinText.isNotBlank() && (proteinVal == null || proteinVal < 0f || proteinVal > 100f)) {
        "Inserisci un valore tra 0 e 100g"
    } else null

    val carbsVal = carbsText.toFloatOrNull()
    val carbsError = if (inputMode == FoodInputMode.PER_100G && carbsText.isNotBlank() && (carbsVal == null || carbsVal < 0f || carbsVal > 100f)) {
        "Inserisci un valore tra 0 e 100g"
    } else null

    val fatVal = fatText.toFloatOrNull()
    val fatError = if (inputMode == FoodInputMode.PER_100G && fatText.isNotBlank() && (fatVal == null || fatVal < 0f || fatVal > 100f)) {
        "Inserisci un valore tra 0 e 100g"
    } else null

    // Total dish field errors
    val totalWeightVal = totalWeightText.toFloatOrNull()
    val totalWeightError = if (inputMode == FoodInputMode.TOTAL_DISH && totalWeightText.isNotBlank() && (totalWeightVal == null || totalWeightVal <= 0f)) {
        "Il peso dev'essere maggiore di 0g"
    } else null

    val totalCalVal = totalCaloriesText.toIntOrNull()
    val totalCalError = if (inputMode == FoodInputMode.TOTAL_DISH && totalCaloriesText.isNotBlank() && (totalCalVal == null || totalCalVal < 0)) {
        "Inserisci calorie non negative"
    } else null

    val totalProVal = totalProteinText.toFloatOrNull()
    val totalProError = if (inputMode == FoodInputMode.TOTAL_DISH && totalProteinText.isNotBlank() && (totalProVal == null || totalProVal < 0f)) {
        "Inserisci un valore non negativo"
    } else null

    val totalCarbsVal = totalCarbsText.toFloatOrNull()
    val totalCarbsError = if (inputMode == FoodInputMode.TOTAL_DISH && totalCarbsText.isNotBlank() && (totalCarbsVal == null || totalCarbsVal < 0f)) {
        "Inserisci un valore non negativo"
    } else null

    val totalFatVal = totalFatText.toFloatOrNull()
    val totalFatError = if (inputMode == FoodInputMode.TOTAL_DISH && totalFatText.isNotBlank() && (totalFatVal == null || totalFatVal < 0f)) {
        "Inserisci un valore non negativo"
    } else null

    val isCaloriesValid = if (inputMode == FoodInputMode.PER_100G) {
        caloriesText.isNotBlank() && caloriesVal != null && caloriesVal in 0..900
    } else {
        totalCaloriesText.isNotBlank() && totalCalVal != null && totalCalVal >= 0
    }

    val isProteinValid = if (inputMode == FoodInputMode.PER_100G) {
        proteinText.isNotBlank() && proteinVal != null && proteinVal in 0f..100f
    } else {
        totalProteinText.isNotBlank() && totalProVal != null && totalProVal >= 0f
    }

    val isCarbsValid = if (inputMode == FoodInputMode.PER_100G) {
        carbsText.isNotBlank() && carbsVal != null && carbsVal in 0f..100f
    } else {
        totalCarbsText.isNotBlank() && totalCarbsVal != null && totalCarbsVal >= 0f
    }

    val isFatValid = if (inputMode == FoodInputMode.PER_100G) {
        fatText.isNotBlank() && fatVal != null && fatVal in 0f..100f
    } else {
        totalFatText.isNotBlank() && totalFatVal != null && totalFatVal >= 0f
    }

    val isWeightValid = inputMode == FoodInputMode.PER_100G || (totalWeightText.isNotBlank() && totalWeightVal != null && totalWeightVal > 0f)

    val canSave = isNameValid && isCaloriesValid && isProteinValid && isCarbsValid && isFatValid && isWeightValid &&
            finalCaloriesPer100g in 0..900 && listOf(finalProteinPer100g, finalCarbsPer100g, finalFatPer100g).all { it.isFinite() && it in 0f..100f }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FoodLabScreenHeader(
                title = "Crea Cibo",
                subtitle = "Cibo personalizzato & calcolatore macro",
                onBack = {
                    soundManager.playSound("click")
                    onNavigateBack()
                }
            )

            // QR & Barcode Scan Card
            FoodLabSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BARCODE / QR CODE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                    Button(
                        onClick = { startQrScanner() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCAN BARCODE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        FoodLabTextField(
                            value = barcodeText,
                            onValueChange = { barcodeText = it.filter { c -> c.isDigit() } },
                            label = "Barcode / QR (Opzionale)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    IconButton(
                        onClick = { handleBarcodeLookup(barcodeText) },
                        modifier = Modifier.size(48.dp),
                        enabled = barcodeText.isNotBlank() && !isBarcodeSearching
                    ) {
                        if (isBarcodeSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search Barcode", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                barcodeStatusMessage?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input Mode Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MODALITÀ CALCOLO", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (inputMode == FoodInputMode.PER_100G) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    soundManager.playSound("click")
                                    inputMode = FoodInputMode.PER_100G
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Per 100g",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (inputMode == FoodInputMode.PER_100G) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (inputMode == FoodInputMode.TOTAL_DISH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    soundManager.playSound("click")
                                    inputMode = FoodInputMode.TOTAL_DISH
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Porzione Totale & Auto-100g",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (inputMode == FoodInputMode.TOTAL_DISH) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Dish Info Card
            FoodLabSectionCard {
                Text("INFORMAZIONI CIBO", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)
                FoodLabTextField(
                    value = name,
                    onValueChange = { name = it.take(160) },
                    label = "Nome cibo (obbligatorio, es. Pasta alla Carbonara)",
                    isError = nameErrorMessage != null || (name.isBlank() && name.isNotEmpty()),
                    errorMessage = nameErrorMessage ?: if (name.isBlank() && name.isNotEmpty()) "Il nome del cibo è obbligatorio" else null
                )
                FoodLabTextField(
                    value = brand,
                    onValueChange = { brand = it.take(160) },
                    label = "Marca o fonte (Opzionale)"
                )
            }

            // Nutrition Entry Card
            FoodLabSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (inputMode == FoodInputMode.PER_100G) "VALORI PER 100G" else "VALORI TOTALE PORZIONE",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (inputMode == FoodInputMode.PER_100G) "DIRETTO" else "CALCOLATO AUTO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (inputMode == FoodInputMode.PER_100G) {
                    FoodLabTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it.filter { c -> c.isDigit() } },
                        label = "Calorie (kcal / 100g)",
                        keyboardType = KeyboardType.Number,
                        isError = calError != null,
                        errorMessage = calError
                    )
                    FoodLabTextField(
                        value = carbsText,
                        onValueChange = { carbsText = sanitizeDecimalInput(it) },
                        label = "Carboidrati (g / 100g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = carbsError != null,
                        errorMessage = carbsError
                    )
                    FoodLabTextField(
                        value = proteinText,
                        onValueChange = { proteinText = sanitizeDecimalInput(it) },
                        label = "Proteine (g / 100g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = proteinError != null,
                        errorMessage = proteinError
                    )
                    FoodLabTextField(
                        value = fatText,
                        onValueChange = { fatText = sanitizeDecimalInput(it) },
                        label = "Grassi (g / 100g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = fatError != null,
                        errorMessage = fatError
                    )
                } else {
                    FoodLabTextField(
                        value = totalWeightText,
                        onValueChange = { totalWeightText = sanitizeDecimalInput(it) },
                        label = "Peso Totale Cibo (g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = totalWeightError != null,
                        errorMessage = totalWeightError
                    )
                    FoodLabTextField(
                        value = totalCaloriesText,
                        onValueChange = { totalCaloriesText = it.filter { c -> c.isDigit() } },
                        label = "Calorie Totali Cibo (kcal)",
                        keyboardType = KeyboardType.Number,
                        isError = totalCalError != null,
                        errorMessage = totalCalError
                    )
                    FoodLabTextField(
                        value = totalCarbsText,
                        onValueChange = { totalCarbsText = sanitizeDecimalInput(it) },
                        label = "Carboidrati Totali Cibo (g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = totalCarbsError != null,
                        errorMessage = totalCarbsError
                    )
                    FoodLabTextField(
                        value = totalProteinText,
                        onValueChange = { totalProteinText = sanitizeDecimalInput(it) },
                        label = "Proteine Totali Cibo (g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = totalProError != null,
                        errorMessage = totalProError
                    )
                    FoodLabTextField(
                        value = totalFatText,
                        onValueChange = { totalFatText = sanitizeDecimalInput(it) },
                        label = "Grassi Totali Cibo (g)",
                        keyboardType = KeyboardType.Decimal,
                        isError = totalFatError != null,
                        errorMessage = totalFatError
                    )
                }

                // Live Normalized Per-100g Preview Card
                MacroPreviewCard(
                    calories = finalCaloriesPer100g,
                    protein = finalProteinPer100g,
                    carbs = finalCarbsPer100g,
                    fat = finalFatPer100g,
                    accent = MaterialTheme.colorScheme.primary,
                    subtitle = if (inputMode == FoodInputMode.TOTAL_DISH && finalWeight > 0) "Valori ricalcolati per 100g" else "Valori normalizzati per 100g"
                )
            }

            // Custom Portions Card
            FoodLabSectionCard {
                Text("PORZIONI PERSONALIZZATE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), color = MaterialTheme.colorScheme.onSurface)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FoodLabTextField(
                            value = portionNameInput,
                            onValueChange = { portionNameInput = it.take(60) },
                            label = "Nome (es. Scodella)"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FoodLabTextField(
                            value = portionWeightInput,
                            onValueChange = { portionWeightInput = sanitizeDecimalInput(it) },
                            label = "Peso (g)",
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                }

                val canAddPortion = portionNameInput.isNotBlank() && (portionWeightInput.toFloatOrNull() ?: 0f) in 0.1f..5000f
                Button(
                    onClick = {
                        val w = portionWeightInput.toFloatOrNull() ?: 0f
                        portionsList = portionsList + (portionNameInput.trim() to w)
                        portionNameInput = ""
                        portionWeightInput = ""
                    },
                    enabled = canAddPortion,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi porzione", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Aggiungi Porzione", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                if (portionsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        portionsList.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${pair.first} (${pair.second.toInt()}g)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(
                                    onClick = { portionsList = portionsList.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Validation Helper Banner if incomplete
            AnimatedVisibility(visible = !canSave) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when {
                                name.trim().isBlank() -> "Inserisci un nome valido per il cibo."
                                inputMode == FoodInputMode.TOTAL_DISH && (totalWeightText.toFloatOrNull() ?: 0f) <= 0f -> "Inserisci un peso totale maggiore di 0g."
                                !isCaloriesValid || !isProteinValid || !isCarbsValid || !isFatValid -> "Inserisci valori numerici non negativi per calorie e macronutrienti."
                                else -> "Controlla che i valori inseriti siano validi per poter salvare."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    if (canSave) {
                        val finalPortions = if (inputMode == FoodInputMode.TOTAL_DISH && finalWeight > 0 && portionsList.none { it.second == finalWeight }) {
                            portionsList + ("Whole Portion" to finalWeight)
                        } else portionsList

                        viewModel.createFood(
                            name.trim(),
                            brand.trim(),
                            finalCaloriesPer100g,
                            finalProteinPer100g,
                            finalCarbsPer100g,
                            finalFatPer100g,
                            finalPortions,
                            barcodeText.trim()
                        )
                        soundManager.playSound("coin")
                        android.widget.Toast.makeText(context, "${name.trim()} salvato nei tuoi alimenti", android.widget.Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "Salva", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SALVA CIBO", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

private fun sanitizeDecimalInput(value: String): String {
    val replaced = value.replace(',', '.')
    val sb = StringBuilder()
    var dotCount = 0
    for (ch in replaced) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && dotCount == 0) {
            sb.append('.')
            dotCount++
        }
    }
    return sb.toString().take(7)
}

@Composable
fun FoodLabTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
