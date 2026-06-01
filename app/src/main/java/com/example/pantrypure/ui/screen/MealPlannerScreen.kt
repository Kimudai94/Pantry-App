package com.example.pantrypure.ui.screen

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.pantrypure.data.model.MealPlan
import com.example.pantrypure.data.model.MissingIngredient
import com.example.pantrypure.data.model.MealPlanWithDetails
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    viewModel: PantryViewModel,
    onNavigateBack: () -> Unit,
    onAddMealToPlan: (Long) -> Unit // Navigates to a picker or meal list
) {
    val currentStartDate by viewModel.currentPlannerDate.collectAsState()
    val weeklyPlans by viewModel.weeklyMealPlans.collectAsState()
    val simulation by viewModel.mealPlanSimulation.collectAsState()
    var draggedMeal by remember { mutableStateOf<MealPlanWithDetails?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialTouchPoint by remember { mutableStateOf(Offset.Zero) }
    val dayBounds = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    
    val sdfDay = SimpleDateFormat("EEEE, dd.MM.", Locale.getDefault())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Planner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.movePlannerWeek(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Week")
                    }
                    IconButton(onClick = { viewModel.movePlannerWeek(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Week")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Generate 7 days starting from Monday
            val days = (0..6).map { offset ->
                Calendar.getInstance().apply {
                    timeInMillis = currentStartDate
                    add(Calendar.DAY_OF_YEAR, offset)
                }.timeInMillis
            }
            
            items(days) { dayTimestamp ->
                val plansForDay = weeklyPlans.filter { it.plan.plannedDate == dayTimestamp }
                
                DaySection(
                    dateLabel = sdfDay.format(Date(dayTimestamp)),
                    isToday = isSameDay(dayTimestamp, System.currentTimeMillis()),
                    plans = plansForDay,
                    simulation = simulation,
                    draggedMeal = draggedMeal,
                    onAddClick = { onAddMealToPlan(dayTimestamp) },
                    onToggleConsumed = { viewModel.toggleMealPlanConsumed(it.plan) },
                    onDeleteClick = { viewModel.deleteMealPlan(it.plan) },
                    onDragStart = { meal, touchPoint ->
                        draggedMeal = meal
                        dragOffset = Offset.Zero
                        initialTouchPoint = touchPoint
                    },
                    onDrag = { dragAmount ->
                        dragOffset += dragAmount
                    },
                    onDragEnd = { meal ->
                        val currentTouchPoint = initialTouchPoint + dragOffset
                        val targetDay = dayBounds.entries.find { it.value.contains(currentTouchPoint) }?.key
                        if (targetDay != null && targetDay != meal.plan.plannedDate) {
                            viewModel.updateMealPlanDate(meal.plan, targetDay)
                        }
                        draggedMeal = null
                        dragOffset = Offset.Zero
                    },
                    onUpdateServings = { plan, servings -> viewModel.updateMealPlanServings(plan, servings) },
                    dragOffset = if (draggedMeal?.plan?.id != null) dragOffset else Offset.Zero,
                    onPositioned = { rect -> dayBounds[dayTimestamp] = rect }
                )
            }
        }
    }
}

@Composable
fun DaySection(
    dateLabel: String,
    isToday: Boolean,
    plans: List<MealPlanWithDetails>,
    simulation: Map<Long, List<MissingIngredient>>,
    draggedMeal: MealPlanWithDetails?,
    onAddClick: () -> Unit,
    onToggleConsumed: (MealPlanWithDetails) -> Unit,
    onDeleteClick: (MealPlanWithDetails) -> Unit,
    onDragStart: (MealPlanWithDetails, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (MealPlanWithDetails) -> Unit,
    onUpdateServings: (MealPlan, Int) -> Unit,
    dragOffset: Offset,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        }
        
        if (plans.isEmpty()) {
            Text(
                text = "No meals planned",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            plans.forEach { planWithDetails ->
                val missing = simulation[planWithDetails.plan.id] ?: emptyList()
                val isBeingDragged = draggedMeal?.plan?.id == planWithDetails.plan.id
                
                MealPlanItem(
                    planWithDetails = planWithDetails,
                    missingIngredients = missing,
                    onToggleConsumed = { onToggleConsumed(planWithDetails) },
                    onDeleteClick = { onDeleteClick(planWithDetails) },
                    isDragged = isBeingDragged,
                    dragOffset = if (isBeingDragged) dragOffset else Offset.Zero,
                    onDragStart = { touchPoint -> onDragStart(planWithDetails, touchPoint) },
                    onDrag = onDrag,
                    onDragEnd = { onDragEnd(planWithDetails) },
                    onUpdateServings = { servings -> onUpdateServings(planWithDetails.plan, servings) }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

@Composable
fun MealPlanItem(
    planWithDetails: MealPlanWithDetails,
    missingIngredients: List<MissingIngredient>,
    onToggleConsumed: () -> Unit,
    onDeleteClick: () -> Unit,
    isDragged: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onUpdateServings: (Int) -> Unit = {}
) {
    var currentBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .zIndex(if (isDragged) 1f else 0f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned { 
                if (!isDragged) {
                    currentBounds = it.boundsInRoot()
                }
            }
            .pointerInput(planWithDetails) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(currentBounds.topLeft + offset) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                planWithDetails.plan.isConsumed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = if (isDragged) CardDefaults.cardElevation(defaultElevation = 8.dp) else CardDefaults.cardElevation()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleConsumed) {
                Icon(
                    imageVector = if (planWithDetails.plan.isConsumed) 
                        Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Consumed",
                    tint = if (planWithDetails.plan.isConsumed) 
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = planWithDetails.meal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (planWithDetails.plan.isConsumed) 
                            androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (missingIngredients.isNotEmpty() && !planWithDetails.plan.isConsumed) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Missing ingredients",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateServings(planWithDetails.plan.servings - 1) },
                        modifier = Modifier.size(24.dp),
                        enabled = planWithDetails.plan.servings > 1 && !planWithDetails.plan.isConsumed
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = planWithDetails.plan.servings.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onUpdateServings(planWithDetails.plan.servings + 1) },
                        modifier = Modifier.size(24.dp),
                        enabled = !planWithDetails.plan.isConsumed
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "More", modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (planWithDetails.plan.servings == 1) "Portion" else "Portionen",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (missingIngredients.isNotEmpty() && !planWithDetails.plan.isConsumed) {
                    Text(
                        text = "Missing: ${missingIngredients.joinToString { it.itemName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "${planWithDetails.ingredients.size} ingredients",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun isSameDay(ts1: Long, ts2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
