package com.example.pantrypure.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NumberMode { INTEGER, DECIMAL }

/**
 * A flexible NumberSpinner with OutlinedTextField design.
 * Supports scrolling, arrow buttons, and manual entry via dialog.
 */
@Composable
fun NumberSpinnerFlexible(
  mode: NumberMode = NumberMode.INTEGER,
  label: String = "",
  intRange: IntRange = 0..1000,
  decimalStart: Double = 0.0,
  decimalEnd: Double = 1000.0,
  initial: Double? = null,
  format: (Double) -> String = {
        if (mode == NumberMode.INTEGER)
            it.roundToInt().toString()
        else
            "%.2f".format(java.util.Locale.getDefault(), it)
    },
  onValueChange: (Double) -> Unit
) {
    val modifier: Modifier = Modifier
    val containerHeight = 56.dp
    val itemHeight = 48.dp
    
    // Generate values list
    val values: List<Double> = remember(mode, intRange, decimalStart, decimalEnd) {
        when (mode) {
            NumberMode.INTEGER -> {
                val steps = (intRange.last - intRange.first).coerceAtLeast(0)
                List(steps + 1) { i -> (intRange.first + i).toDouble() }
            }
            NumberMode.DECIMAL -> {
                val steps = (((decimalEnd - decimalStart) * 20).roundToInt()).coerceAtLeast(0)
                List(steps + 1) { i -> decimalStart + (i * 0.05) }
            }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showManualInput by remember { mutableStateOf(false) }

    // Track if initialization or manual update is needed
    LaunchedEffect(initial, values) {
        val target = initial ?: when (mode) {
            NumberMode.INTEGER -> intRange.first.toDouble()
            NumberMode.DECIMAL -> decimalStart
        }
        val idx = values.indices.minByOrNull { abs(values[it] - target) } ?: 0
        if (listState.firstVisibleItemIndex != idx) {
            listState.scrollToItem(idx)
        }
    }

    // Update parent when scroll finishes
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            values.getOrNull(listState.firstVisibleItemIndex)?.let {
                onValueChange(it)
            }
        }
    }

    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .height(containerHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = OutlinedTextFieldDefaults.shape
                )
                .clickable { showManualInput = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = (containerHeight - itemHeight) / 2),
                userScrollEnabled = true
            ) {
                itemsIndexed(values) { index, item ->
                    val isSelected = listState.firstVisibleItemIndex == index
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format(item),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Arrow buttons
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            val next = (listState.firstVisibleItemIndex + 1).coerceAtMost(values.lastIndex)
                            listState.animateScrollToItem(next)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowDropUp, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            val prev = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(prev)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Integrated Label
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = 12.dp, y = (-8).dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp)
            )
        }
    }

    // Manual Input Dialog
    if (showManualInput) {
        val currentValue = values.getOrNull(listState.firstVisibleItemIndex) ?: 0.0
        val initialString = if (mode == NumberMode.INTEGER) currentValue.toInt().toString() else "%.2f".format(java.util.Locale.US, currentValue)
        
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = initialString,
                    selection = TextRange(0, initialString.length)
                )
            )
        }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val onConfirm = {
            textFieldValue.text.toDoubleOrNull()?.let { inputVal ->
                val nearest = values.minByOrNull { v -> abs(v - inputVal) } ?: inputVal
                onValueChange(nearest)
                scope.launch {
                    val idx = values.indices.minByOrNull { abs(values[it] - nearest) } ?: 0
                    listState.scrollToItem(idx)
                }
            }
            showManualInput = false
        }

        AlertDialog(
            onDismissRequest = { showManualInput = false },
            title = { Text(text = "Enter $label") },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        val newText = it.text.replace(',', '.')
                        textFieldValue = it.copy(text = newText)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == NumberMode.INTEGER) KeyboardType.Number else KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onConfirm()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(label) }
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInput = false }) {
                    Text("Cancel")
                }
            }
        )

        LaunchedEffect(Unit) {
            delay(300)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Ignore focus errors if the dialog was dismissed quickly
            }
        }
    }
}
