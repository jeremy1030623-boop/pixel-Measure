package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

import androidx.compose.runtime.collectAsState

@Composable
fun RulerComponent(
    onSaveClick: (String, Double) -> Unit,
    selectedUnit: String,
    viewModel: com.example.ui.viewmodel.MeasureViewModel
) {
    // Zero reference offsets (offset from top of screen to make room for status bars / pin hole cameras)
    val zeroY = with(LocalDensity.current) { 60.dp.toPx() }

    val currentLang by viewModel.currentLanguage.collectAsState()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val displayMetrics = context.resources.displayMetrics
    
    // Real hardware Y-DPI or DPI fallback
    val ydpi = remember(displayMetrics) {
        val y = displayMetrics.ydpi
        if (y > 50f && !y.isNaN() && !y.isInfinite()) y else displayMetrics.densityDpi.toFloat()
    }
    
    val calibrationFactor by viewModel.rulerCalibration.collectAsState()
    val scaledYdpi = remember(ydpi, calibrationFactor) { ydpi * calibrationFactor }

    // Helper functions for CM to Pixel mapping
    val mmInPx = remember(scaledYdpi) { scaledYdpi / 25.4f }

    // UI controls
    var rulerOnRight by remember { mutableStateOf(true) }
    var showCalibration by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var objectNameInput by remember { mutableStateOf("") }
    var manualValueInput by remember { mutableStateOf("") }

    // Interactive translations
    val titleText = viewModel.getString("onboarding_title_1")
    val alignLeftText = viewModel.getString("cancel")
    val alignRightText = viewModel.getString("ok_clear")
    val calibrationText = viewModel.getString("diagnostics_title")
    val calibrateModeText = viewModel.getString("diagnostics_status_searching")
    val saveBtnText = viewModel.getString("history_saved")
    val dragHintText = viewModel.getString("onboarding_desc_1")
    val saveDialogTitle = viewModel.getString("history_saved")
    val saveDialogHint = viewModel.getString("onboarding_desc_2")
    val saveDialogPlaceholderName = viewModel.getString("onboarding_desc_3")
    val saveDialogPlaceholderValue = viewModel.getString("measurement_hint")
    val defaultObjectName = viewModel.getString("history_item_ruler")
    val okConfirmText = viewModel.getString("ok_clear")
    val cancelText = viewModel.getString("cancel")

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorBackground = MaterialTheme.colorScheme.background
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorBackground)
    ) {
        val rulerPanelWidth = 140.dp
        
        AnimatedContent(
            targetState = rulerOnRight,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "RulerSideToggle"
        ) { isRight ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (isRight) {
                    // Workspace on Left
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WorkspaceDisplayCard(
                            titleText = titleText,
                            dragHintText = dragHintText,
                            showSaveDialog = {
                                objectNameInput = ""
                                manualValueInput = ""
                                showSaveDialog = true
                            },
                            rulerOnRight = true,
                            onToggleSide = { rulerOnRight = it },
                            showCalibration = showCalibration,
                            onToggleCalibration = { showCalibration = it },
                            saveBtnText = saveBtnText,
                            alignLeftText = alignLeftText,
                            alignRightText = alignRightText,
                            calibrationText = calibrationText
                        )
                    }
                    
                    // Samsung-Style Ruler Panel on Right
                    SamsungRulerPanel(
                        width = rulerPanelWidth,
                        rulerOnRight = true,
                        zeroY = zeroY,
                        mmInPx = mmInPx,
                        colorOnSurfaceVariant = colorOnSurfaceVariant,
                        selectedUnit = selectedUnit,
                        scaledYdpi = scaledYdpi,
                        showCalibration = showCalibration,
                        calibrationFactor = calibrationFactor,
                        calibrateModeText = calibrateModeText,
                        onUpdateCalibration = { viewModel.updateRulerCalibration(it) }
                    )
                } else {
                    // Samsung-Style Ruler Panel on Left
                    SamsungRulerPanel(
                        width = rulerPanelWidth,
                        rulerOnRight = false,
                        zeroY = zeroY,
                        mmInPx = mmInPx,
                        colorOnSurfaceVariant = colorOnSurfaceVariant,
                        selectedUnit = selectedUnit,
                        scaledYdpi = scaledYdpi,
                        showCalibration = showCalibration,
                        calibrationFactor = calibrationFactor,
                        calibrateModeText = calibrateModeText,
                        onUpdateCalibration = { viewModel.updateRulerCalibration(it) }
                    )

                    // Workspace on Right
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WorkspaceDisplayCard(
                            titleText = titleText,
                            dragHintText = dragHintText,
                            showSaveDialog = {
                                objectNameInput = ""
                                manualValueInput = ""
                                showSaveDialog = true
                            },
                            rulerOnRight = false,
                            onToggleSide = { rulerOnRight = it },
                            showCalibration = showCalibration,
                            onToggleCalibration = { showCalibration = it },
                            saveBtnText = saveBtnText,
                            alignLeftText = alignLeftText,
                            alignRightText = alignRightText,
                            calibrationText = calibrationText
                        )
                    }
                }
            }
        }
    }

    // Modal Saving Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(saveDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(saveDialogHint, style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedTextField(
                        value = objectNameInput,
                        onValueChange = { objectNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(if (currentLang.startsWith("zh")) "物體名稱" else "Object Name") },
                        placeholder = { Text(saveDialogPlaceholderName) }
                    )

                    OutlinedTextField(
                        value = manualValueInput,
                        onValueChange = { manualValueInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(if (currentLang.startsWith("zh")) "讀取長度 (${selectedUnit})" else "Measured Length (${selectedUnit})") },
                        placeholder = { Text(saveDialogPlaceholderValue) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalLabel = objectNameInput.trim().ifEmpty { defaultObjectName }
                        val parsedVal = manualValueInput.toDoubleOrNull() ?: 0.0
                        
                        // If selectedUnit is not cm, convert it into cm first before saving
                        val cmValue = when (selectedUnit) {
                            "m" -> parsedVal * 100.0
                            "in" -> parsedVal * 2.54
                            "ft" -> parsedVal * 30.48
                            else -> parsedVal
                        }
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSaveClick(finalLabel, cmValue)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(okConfirmText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(cancelText)
                }
            }
        )
    }
}

@Composable
fun WorkspaceDisplayCard(
    titleText: String,
    dragHintText: String,
    showSaveDialog: () -> Unit,
    rulerOnRight: Boolean,
    onToggleSide: (Boolean) -> Unit,
    showCalibration: Boolean,
    onToggleCalibration: (Boolean) -> Unit,
    saveBtnText: String,
    alignLeftText: String,
    alignRightText: String,
    calibrationText: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Description of how to measure physically
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { clip = true }
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp)
            ) {
                Text(
                    text = dragHintText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Quick Control Action Buttons: Save & Calibrate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save measurement record button
                Button(
                    onClick = showSaveDialog,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "儲存記錄", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(saveBtnText, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Position & Calibration Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rule side adjustment toggle
                Button(
                    onClick = { onToggleSide(!rulerOnRight) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = "側邊切換", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (rulerOnRight) alignLeftText else alignRightText, style = MaterialTheme.typography.labelSmall)
                }

                // Calibration overlay toggle
                Button(
                    onClick = { onToggleCalibration(!showCalibration) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCalibration) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showCalibration) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "校準", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(calibrationText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SamsungRulerPanel(
    width: androidx.compose.ui.unit.Dp,
    rulerOnRight: Boolean,
    zeroY: Float,
    mmInPx: Float,
    colorOnSurfaceVariant: Color,
    selectedUnit: String,
    scaledYdpi: Float,
    showCalibration: Boolean,
    calibrationFactor: Float,
    calibrateModeText: String,
    onUpdateCalibration: (Float) -> Unit
) {
    val colorPrimary = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xE61A1C1E)) // Elegant dynamic dark acrylic gray
    ) {
        // Calibration Slider sliding overlay if toggled
        AnimatedVisibility(
            visible = showCalibration,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xF2121415)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = calibrateModeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f%%", calibrationFactor * 100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorPrimary
                        )
                        Slider(
                            value = calibrationFactor,
                            onValueChange = onUpdateCalibration,
                            valueRange = 0.85f..1.15f
                        )
                        TextButton(
                            onClick = { onUpdateCalibration(1.0f) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Draw scale ticks flush to screen edge
        Canvas(modifier = Modifier.fillMaxSize()) {
            val panelW = size.width
            val h = size.height

            // Paint configurations for vertical numeric labels
            val textPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                isAntiAlias = true
            }

            // Loop metrics
            var curY = zeroY
            var idx = 0

            // Draw graduated rules
            while (curY < h - 40f) {
                // Decide tick length based on centimeter, half-centimeter, or millimeter status
                val tickLen = when {
                    idx % 10 == 0 -> 24.dp.toPx() // centimeters
                    idx % 5 == 0 -> 15.dp.toPx()  // 0.5 cm
                    else -> 8.dp.toPx()           // millimeters
                }

                // Calculate edge alignment
                // Right aligned means start on panel width (panelW) and draw to left
                // Left aligned means start at 0 and draw to right
                val startX = if (rulerOnRight) panelW else 0f
                val endX = if (rulerOnRight) panelW - tickLen else tickLen

                drawLine(
                    color = if (idx % 10 == 0) colorPrimary else Color.White.copy(alpha = 0.4f),
                    start = Offset(startX, curY),
                    end = Offset(endX, curY),
                    strokeWidth = if (idx % 10 == 0) 2.5f.dp.toPx() else 1.2f.dp.toPx()
                )

                // Render number index texts for centimeters
                if (idx % 10 == 0) {
                    val labelStr = (idx / 10).toString()
                    val textX = if (rulerOnRight) {
                        panelW - tickLen - 12.dp.toPx()
                    } else {
                        tickLen + 6.dp.toPx()
                    }
                    val textY = curY + 3.5f.dp.toPx() // vertical centering offset

                    drawContext.canvas.nativeCanvas.drawText(
                        labelStr,
                        textX,
                        textY,
                        textPaint
                    )
                }

                curY += mmInPx
                idx++
            }
        }
    }
}
