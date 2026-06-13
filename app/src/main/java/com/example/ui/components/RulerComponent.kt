package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
                shape = MaterialTheme.shapes.extraLarge
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
                    
                    AnimatedContent(
                        targetState = displayValue,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "valueAnim"
                    ) { targetValue ->
                        Text(
                            text = String.format("%.2f %s", targetValue, selectedUnit),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            objectNameInput = "物品"
                            showSaveDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "儲存")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("儲存此測量")
                    }
                }
            }

            // Draggable ruler board
            val entryScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "entryScale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = entryScale, scaleY = entryScale)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(28.dp))
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

                    // Single graduation scales on the left edge (Centimeter physical scale)
                    val mmInDp = 1.5875f
                    val mmInPx = mmInDp.dp.toPx()
                    var curY = 0f
                    var idx = 0

                    val textPaint = android.text.TextPaint().apply {
                        val alphaVal = (colorOnSurfaceVariant.alpha * 180).toInt()
                        val redVal = (colorOnSurfaceVariant.red * 255).toInt()
                        val greenVal = (colorOnSurfaceVariant.green * 255).toInt()
                        val blueVal = (colorOnSurfaceVariant.blue * 255).toInt()
                        color = android.graphics.Color.argb(alphaVal, redVal, greenVal, blueVal)
                        textSize = 10.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }

                    while (curY < h) {
                        val tickLen = when {
                            idx % 10 == 0 -> 22.dp.toPx() // Centimeter ticks
                            idx % 5 == 0 -> 14.dp.toPx()  // 0.5 cm ticks
                            else -> 8.dp.toPx()           // Millimeter ticks
                        }
                        
                        drawLine(
                            color = colorOutline,
                            start = Offset(0f, curY),
                            end = Offset(tickLen, curY),
                            strokeWidth = if (idx % 10 == 0) 2.dp.toPx() else 1.dp.toPx()
                        )

                        // Centimeter labels next to major centimeter ticks
                        if (idx % 10 == 0) {
                            val cmLabel = (idx / 10).toString()
                            val textX = tickLen + 8.dp.toPx()
                            val textY = curY + 3.5f.dp.toPx() // offset vertically to center the font baseline
                            drawContext.canvas.nativeCanvas.drawText(
                                cmLabel,
                                textX,
                                textY,
                                textPaint
                            )
                        }

                        curY += mmInPx
                        idx++
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
                        color = colorTertiary.copy(alpha = 0.5f),
                        start = Offset(w / 2f, topCaliperY),
                        end = Offset(w / 2f, bottomCaliperY),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )

                    // Draw Top Caliper handle indicator (Horizontal capsule slide bar)
                    val barHeight = 8f
                    drawRoundRect(
                        color = colorPrimary.copy(alpha = 0.3f),
                        topLeft = Offset(0f, topCaliperY - barHeight / 2f),
                        size = Size(w, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                    drawLine(
                        color = colorPrimary,
                        start = Offset(0f, topCaliperY),
                        end = Offset(w, topCaliperY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw nice premium rounded pill-shaped grabber handle in the middle of top caliper
                    val handleW = 120f
                    val handleH = 40f
                    val handleR = 20f
                    
                    // Ripple-like outer glow ring
                    drawCircle(
                        color = colorPrimary.copy(alpha = 0.15f),
                        radius = 32f,
                        center = Offset(w / 2f, topCaliperY)
                    )
                    
                    drawRoundRect(
                        color = colorPrimary,
                        topLeft = Offset(w / 2f - handleW / 2f, topCaliperY - handleH / 2f),
                        size = Size(handleW, handleH),
                        cornerRadius = CornerRadius(handleR, handleR)
                    )
                    
                    // Inner beautiful tactile capsule notch
                    val notchW = 40f
                    val notchH = 8f
                    val notchR = 4f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(w / 2f - notchW / 2f, topCaliperY - notchH / 2f),
                        size = Size(notchW, notchH),
                        cornerRadius = CornerRadius(notchR, notchR)
                    )

                    // Draw Bottom Caliper handle indicator (Horizontal capsule slide bar)
                    drawRoundRect(
                        color = colorPrimary.copy(alpha = 0.3f),
                        topLeft = Offset(0f, bottomCaliperY - barHeight / 2f),
                        size = Size(w, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                    drawLine(
                        color = colorPrimary,
                        start = Offset(0f, bottomCaliperY),
                        end = Offset(w, bottomCaliperY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    
                    // Ripple-like outer glow ring
                    drawCircle(
                        color = colorPrimary.copy(alpha = 0.15f),
                        radius = 32f,
                        center = Offset(w / 2f, bottomCaliperY)
                    )
                    
                    // Draw nice premium rounded pill-shaped grabber handle in the middle of bottom caliper
                    drawRoundRect(
                        color = colorPrimary,
                        topLeft = Offset(w / 2f - handleW / 2f, bottomCaliperY - handleH / 2f),
                        size = Size(handleW, handleH),
                        cornerRadius = CornerRadius(handleR, handleR)
                    )
                    
                    // Inner beautiful tactile capsule notch
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(w / 2f - notchW / 2f, bottomCaliperY - notchH / 2f),
                        size = Size(notchW, notchH),
                        cornerRadius = CornerRadius(notchR, notchR)
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
