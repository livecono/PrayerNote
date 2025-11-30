package com.prayernote.app.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs

@Composable
fun CustomTimePickerDialog(
    initialHour: Int = 7,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var isPM by remember { mutableStateOf(initialHour >= 12) }
    
    // Display hour (1-12)
    val displayHour = remember(hour) {
        when (hour) {
            0 -> 12
            in 1..12 -> hour
            else -> hour - 12
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "시간 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AM/PM Picker
                    TimePickerColumn(
                        items = listOf("오전", "오후"),
                        selectedIndex = if (isPM) 1 else 0,
                        onSelectedIndexChange = { newIndex ->
                            val newIsPM = newIndex == 1
                            // Convert current display hour to new 24-hour format
                            hour = when {
                                newIsPM -> if (displayHour == 12) 12 else displayHour + 12
                                else -> if (displayHour == 12) 0 else displayHour
                            }
                            isPM = newIsPM
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Hour Picker
                    TimePickerColumn(
                        items = (1..12).map { it.toString().padStart(2, '0') },
                        selectedIndex = displayHour - 1,
                        onSelectedIndexChange = { index ->
                            val newDisplayHour = index + 1
                            hour = when {
                                isPM -> if (newDisplayHour == 12) 12 else newDisplayHour + 12
                                else -> if (newDisplayHour == 12) 0 else newDisplayHour
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute Picker
                    TimePickerColumn(
                        items = (0..59).map { it.toString().padStart(2, '0') },
                        selectedIndex = minute,
                        onSelectedIndexChange = { minute = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        onConfirm(hour, minute)
                        onDismiss()
                    }) {
                        Text("확인")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
    }

    Box(
        modifier = modifier
            .height(150.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        val threshold = 20.dp.toPx() // Sensitivity threshold
                        
                        while (accumulatedDrag > threshold) {
                            // Drag down - previous item
                            currentIndex = (currentIndex - 1 + items.size) % items.size
                            onSelectedIndexChange(currentIndex)
                            accumulatedDrag -= threshold
                        }
                        
                        while (accumulatedDrag < -threshold) {
                            // Drag up - next item
                            currentIndex = (currentIndex + 1) % items.size
                            onSelectedIndexChange(currentIndex)
                            accumulatedDrag += threshold
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Previous item
            Text(
                text = items[(currentIndex - 1 + items.size) % items.size],
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .alpha(0.3f)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            // Current item (selected)
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = items[currentIndex],
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // Next item
            Text(
                text = items[(currentIndex + 1) % items.size],
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .alpha(0.3f)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
