package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun RulerComponent(
    onSaveClick: (String, Double) -> Unit,
    selectedUnit: String
) {
    var topCaliperY by remember { mutableStateOf(100f) }
    var bottomCaliperY by remember { mutableStateOf(500f) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var objectNameInput by remember { mutableStateOf("") }

    val density = LocalDensity.current.density
    
    // Physical Conversion: 1 dp = 0.15875 mm = 0.015875 cm
    // caliper distance in DP = pixelDist / density
    fun getMeasuredCm(): Double {
        val dpDist = abs(bottomCaliperY - topCaliperY) / density
        return dpDist * 0.015875
    }

    val displayCm = getMeasuredCm()
    val displayValue = when (selectedUnit) {
        "cm" -> displayCm
        "m" -> displayCm / 100.0
        "in" -> displayCm / 2.54
        "ft" -> displayCm / 30.48
        else -> displayCm
    }

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorOutline = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Value display panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "螢幕直尺測量儀",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.2f %s", displayValue, selectedUnit),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            objectNameInput = "物品"
                            showSaveDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "儲存")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("儲存此測量")
                    }
                }
            }

            // Draggable ruler board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                    .pointerInput(density) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val touchY = change.position.y
                                // Determine which caliper is closer to drag and move it
                                if (abs(touchY - topCaliperY) < abs(touchY - bottomCaliperY)) {
                                    topCaliperY = touchY.coerceIn(20f, bottomCaliperY - 40f)
                                } else {
                                    bottomCaliperY = bottomCaliperY + dragAmount.y
                                    bottomCaliperY = bottomCaliperY.coerceIn(topCaliperY + 40f, size.height.toFloat() - 20f)
                                }
                            }
                        )
                    }
            ) {
                // Rule Scale drawings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // CM graduation scales on the right edge
                    val mmInDp = 1.5875f
                    val mmInPx = mmInDp * density
                    var curY = 0f
                    var idx = 0
                    while (curY < h) {
                        val tickLen = when {
                            idx % 10 == 0 -> 45f // Major ticks centimeter
                            idx % 5 == 0 -> 30f  // Mid ticks half-cm
                            else -> 18f          // Minor ticks
                        }
                        drawLine(
                            color = colorOutline,
                            start = Offset(w, curY),
                            end = Offset(w - tickLen, curY),
                            strokeWidth = if (idx % 10 == 0) 2.5f else 1.5f
                        )
                        // Label centimeter values
                        if (idx % 10 == 0 && curY > 10f) {
                            val cmLabel = idx / 10
                            // Quick text drawing via custom layout not needed, elegant tick display highlights scale
                        }
                        curY += mmInPx
                        idx++
                    }

                    // INCH graduation scales on the left edge
                    val inchInDp = 160f
                    val inchInPx = inchInDp * density
                    val eighthInchPx = inchInPx / 8f
                    var curInchY = 0f
                    var inchIdx = 0
                    while (curInchY < h) {
                        val tickLen = when {
                            inchIdx % 8 == 0 -> 45f  // Major inch
                            inchIdx % 4 == 0 -> 30f  // Half inch
                            inchIdx % 2 == 0 -> 22f  // Quarter inch
                            else -> 15f              // 1/8 inch
                        }
                        drawLine(
                            color = colorOutline,
                            start = Offset(0f, curInchY),
                            end = Offset(tickLen, curInchY),
                            strokeWidth = if (inchIdx % 8 == 0) 2.5f else 1.5f
                        )
                        curInchY += eighthInchPx
                        inchIdx++
                    }

                    // Background grid patterns for aesthetic sci-fi appeal
                    for (xGrid in (w.toInt() / 5)..w.toInt() step (w.toInt() / 5)) {
                        drawLine(
                            color = colorOnSurfaceVariant.copy(alpha = 0.05f),
                            start = Offset(xGrid.toFloat(), 0f),
                            end = Offset(xGrid.toFloat(), h),
                            strokeWidth = 1f
                        )
                    }

                    // Draw connection line between top & bottom calipers
                    drawLine(
                        color = colorTertiary.copy(alpha = 0.4f),
                        start = Offset(w / 2f, topCaliperY),
                        end = Offset(w / 2f, bottomCaliperY),
                        strokeWidth = 2f
                    )

                    // Draw Top Caliper handle indicator
                    drawLine(
                        color = colorPrimary,
                        start = Offset(0f, topCaliperY),
                        end = Offset(w, topCaliperY),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = colorPrimary,
                        radius = 12f,
                        center = Offset(w / 2f, topCaliperY)
                    )

                    // Draw Bottom Caliper handle indicator
                    drawLine(
                        color = colorPrimary,
                        start = Offset(0f, bottomCaliperY),
                        end = Offset(w, bottomCaliperY),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = colorPrimary,
                        radius = 12f,
                        center = Offset(w / 2f, bottomCaliperY)
                    )
                }

                // HUD Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "《 拖動兩端刻度進行微調測量 》",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorOnSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modal Saving Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("儲存測量數值") },
            text = {
                Column {
                    Text("請輸入測量物品名稱：")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = objectNameInput,
                        onValueChange = { objectNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("例如：筆記本、香蕉...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalLabel = objectNameInput.trim().ifEmpty { "螢幕直尺測量" }
                        onSaveClick(finalLabel, displayCm)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("確認儲存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("取消")
                }
            }
        )
    }
}
