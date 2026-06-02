package com.example.pantrypure.ui.screen.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.util.ReceiptAnalyzer
import com.example.pantrypure.util.ReceiptParser
import com.google.mlkit.vision.text.Text
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onIngredientsDetected: (List<ScannedIngredient>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val receiptParser = remember { ReceiptParser() }
    
    var detectedIngredients by remember { mutableStateOf(listOf<ScannedIngredient>()) }
    var isScanning by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bon scannen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = { isScanning = !isScanning }) {
                        Text(if (isScanning) "Review" else "Scan")
                    }
                }
            )
        },
        floatingActionButton = {
            if (detectedIngredients.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        onIngredientsDetected(detectedIngredients)
                    },
                    icon = { Icon(Icons.Default.Check, "Fertig") },
                    text = { Text("Übernehmen (${detectedIngredients.size})") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isScanning) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor, ReceiptAnalyzer { visionText: Text ->
                                            val ingredientNames = receiptParser.parseReceipt(visionText)
                                            if (ingredientNames.isNotEmpty()) {
                                                val newIngredients = ingredientNames
                                                    .filter { name -> detectedIngredients.none { it.name == name } }
                                                    .map { name -> ScannedIngredient(name = name) }
                                                
                                                if (newIngredients.isNotEmpty()) {
                                                    detectedIngredients = detectedIngredients + newIngredients
                                                }
                                            }
                                        })
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    // Handle error
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scanner Overlay
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            "Bon in den Fokus rücken. Zutaten werden automatisch erkannt.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    "Erkannte Artikel bearbeiten:",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(detectedIngredients) { ingredient ->
                        ScannedIngredientItem(
                            ingredient = ingredient,
                            onUpdate = { updated ->
                                detectedIngredients = detectedIngredients.map { 
                                    if (it.name == ingredient.name) updated else it 
                                }
                            },
                            onDelete = {
                                detectedIngredients = detectedIngredients.filter { it.name != ingredient.name }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedIngredientItem(
    ingredient: ScannedIngredient,
    onUpdate: (ScannedIngredient) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (ingredient.quantity == 0.0) "" else ingredient.quantity.toString(),
                    onValueChange = { 
                        val qty = it.toDoubleOrNull() ?: 0.0
                        onUpdate(ingredient.copy(quantity = qty))
                    },
                    label = { Text("Menge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(100.dp)
                )
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(ingredient.unit.label)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PantryUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    onUpdate(ingredient.copy(unit = unit))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
