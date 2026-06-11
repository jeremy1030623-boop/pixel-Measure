package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CameraViewComponent
import com.example.ui.components.RulerComponent
import com.example.ui.components.SurfaceLevelComponent
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasureViewModel
import com.example.ui.viewmodel.Point3D
import com.example.ui.viewmodel.deserializePoints
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MeasureViewModel) {
    val currentMode by viewModel.currentMode.collectAsState()
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val savedRecords by viewModel.savedRecords.collectAsState()
    val vibrateOnAlignment by viewModel.vibrateOnAlignment.collectAsState()
    
    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 680

    val iconScale0 by animateFloatAsState(
        targetValue = if (currentMode == 0) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabScale0"
    )
    val iconScale1 by animateFloatAsState(
        targetValue = if (currentMode == 1) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabScale1"
    )
    val iconScale2 by animateFloatAsState(
        targetValue = if (currentMode == 2) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabScale2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Main tool container panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                                // Top Global Header Bar
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .padding(top = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedContent(
                                            targetState = currentMode,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                                                 scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                                .togetherWith(fadeOut(animationSpec = tween(90)))
                                            },
                                            label = "TitleAnim"
                                        ) { targetMode ->
                                            Text(
                                                text = when (targetMode) {
                                                    0 -> "相機 AR 測量"
                                                    1 -> "螢幕卡鉗直尺"
                                                    2 -> "表面水平校正"
                                                    else -> "工具"
                                                },
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Quick Unit Selector Capsule HUD
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Calibration Button
                                            IconButton(
                                                onClick = { viewModel.calibrateSensors() },
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Balance,
                                                    contentDescription = "校準",
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Settings Button
                                            IconButton(
                                                onClick = { showSettingsSheet = true },
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = "設定",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
 
                                            Row(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                                    .padding(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("cm", "m", "in", "ft").forEach { unit ->
                                                    val isSelected = selectedUnit == unit
                                                    val animAlpha by animateFloatAsState(
                                                        targetValue = if (isSelected) 1f else 0f,
                                                        animationSpec = tween(durationMillis = 300),
                                                        label = "unitAlpha"
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .graphicsLayer(alpha = 1f)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = animAlpha),
                                                                MaterialTheme.shapes.small
                                                            )
                                                            .clickable { viewModel.setUnit(unit) }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = unit,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tool viewport loader with animation
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    AnimatedContent(
                                        targetState = currentMode,
                                        transitionSpec = {
                                            val springSpec = spring<Float>(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                            val slideSpring = spring<androidx.compose.ui.unit.IntOffset>(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                            if (targetState > initialState) {
                                                (slideInHorizontally(animationSpec = slideSpring) { width -> width / 3 } + fadeIn(animationSpec = springSpec))
                                                    .togetherWith(slideOutHorizontally(animationSpec = slideSpring) { width -> -width / 3 } + fadeOut(animationSpec = springSpec))
                                            } else {
                                                (slideInHorizontally(animationSpec = slideSpring) { width -> -width / 3 } + fadeIn(animationSpec = springSpec))
                                                    .togetherWith(slideOutHorizontally(animationSpec = slideSpring) { width -> width / 3 } + fadeOut(animationSpec = springSpec))
                                            }.using(
                                                SizeTransform(clip = false)
                                            )
                                        },
                                        label = "ToolModeAnimation"
                                    ) { targetMode ->
                        when (targetMode) {
                            0 -> CameraViewComponent(
                                viewModel = viewModel,
                                onShowHistoryClick = { showHistorySheet = !showHistorySheet }
                            )
                            1 -> RulerComponent(
                                onSaveClick = { title, cmVal ->
                                    viewModel.saveRulerMeasurement(title, cmVal)
                                },
                                selectedUnit = selectedUnit
                            )
                            2 -> SurfaceLevelComponent(
                                pitch = pitch,
                                roll = roll,
                                vibrateOnAlignment = vibrateOnAlignment,
                                onCalibrate = { viewModel.calibrateSensors() },
                                onReset = { viewModel.resetCalibration() }
                            )
                        }
                    }
                }

                // Visual Bottom Navigation tabs bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentMode == 0,
                        onClick = { viewModel.setMode(0) },
                        icon = { 
                            Icon(
                                Icons.Default.CameraAlt, 
                                contentDescription = "相機",
                                modifier = Modifier.graphicsLayer(scaleX = iconScale0, scaleY = iconScale0)
                            ) 
                        },
                        label = { Text("相機 AR") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = currentMode == 1,
                        onClick = { viewModel.setMode(1) },
                        icon = { 
                            Icon(
                                Icons.Default.Straighten, 
                                contentDescription = "直尺",
                                modifier = Modifier.graphicsLayer(scaleX = iconScale1, scaleY = iconScale1)
                            ) 
                        },
                        label = { Text("螢幕尺") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = currentMode == 2,
                        onClick = { viewModel.setMode(2) },
                        icon = { 
                            Icon(
                                Icons.Default.Explore, 
                                contentDescription = "表面水平",
                                modifier = Modifier.graphicsLayer(scaleX = iconScale2, scaleY = iconScale2)
                            ) 
                        },
                        label = { Text("水平儀") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Wide Tablet side-by-side History Panel (Material 3 Supporting Pane Layout)
            if (isWideScreen) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                        .padding(top = 24.dp)
                ) {
                    HistoryContentPane(
                        savedRecords = savedRecords,
                        onDeleteRecord = { viewModel.deleteRecord(it) },
                        onClearAll = { viewModel.clearAllRecords() }
                    )
                }
            }
        }
    }

    // Phone standard Bottom sheet overlay
        if (showHistorySheet && !isWideScreen) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.65f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    HistoryContentPane(
                        savedRecords = savedRecords,
                        onDeleteRecord = { viewModel.deleteRecord(it) },
                        onClearAll = { viewModel.clearAllRecords() }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SettingsContentPane(
                        viewModel = viewModel,
                        onDismiss = { showSettingsSheet = false }
                    )
                }
            }
        }
    }
}

// Reusable custom Measurement History Logs List Pane
@Composable
fun HistoryContentPane(
    savedRecords: List<com.example.data.model.MeasureRecord>,
    onDeleteRecord: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    val dateTimeFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedRecordId by remember { mutableStateOf<Int?>(null) }

    val filteredRecords = remember(savedRecords, searchQuery) {
        if (searchQuery.isBlank()) {
            savedRecords
        } else {
            savedRecords.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                (it.notes ?: "").contains(searchQuery, ignoreCase = true) ||
                it.unit.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "儲存在地測量記錄",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (savedRecords.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("清除全部", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedRecords.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜尋名稱或備註描述...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredRecords.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "無記錄",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (savedRecords.isEmpty()) "尚無儲存的測量數據" else "無匹配的搜尋結果",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (savedRecords.isEmpty()) "使用相機 AR 與螢幕尺子測量，並點擊「儲存」按鈕來記錄您的數據。" else "嘗試更換其他搜尋關鍵字後再行重試。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    val isExpanded = record.id == expandedRecordId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .clickable { expandedRecordId = if (isExpanded) null else record.id },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Source type visual icon capsule
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (record.type == "CAM") Icons.Default.CameraAlt else Icons.Default.Straighten,
                                            contentDescription = "模式",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateTimeFormatter.format(Date(record.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format("%.2f %s", record.value, record.unit),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    IconButton(
                                        onClick = { 
                                            onDeleteRecord(record.id)
                                            if (isExpanded) expandedRecordId = null
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "刪除",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!record.notes.isNullOrBlank()) {
                                        Text(
                                            "備註分析:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            record.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Display 3D annotated vector coordinate analysis if CAM model is loaded
                                    if (record.type == "CAM" && !record.pointsData.isNullOrBlank()) {
                                        val points = try {
                                            record.pointsData.deserializePoints()
                                        } catch (e: Exception) {
                                            emptyList<Point3D>()
                                        }
                                        if (points.isNotEmpty()) {
                                            Text(
                                                "AR 測量標註分析 (${points.size} 個空間標點):",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            points.forEachIndexed { pIdx, pt ->
                                                val pointName = if (pt.label.isNotBlank()) pt.label else "標註點 ${pIdx + 1}"
                                                Text(
                                                    " • $pointName: (X:${String.format("%.2f", pt.x)}, Y:${String.format("%.2f", pt.y)}, Z:${String.format("%.2f", pt.z)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    // Precision sharing utilities hud
                                    Text(
                                        "分享與備份導出:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { com.example.logic.ShareUtility.shareTextReport(context, record) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Share, null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("文字", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        
                                        Button(
                                            onClick = { com.example.logic.ShareUtility.shareImageReport(context, record) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Image, null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("圖片", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }

                                        Button(
                                            onClick = { com.example.logic.ShareUtility.sharePdfReport(context, record) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Description, null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PDF", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContentPane(
    viewModel: MeasureViewModel,
    onDismiss: () -> Unit
) {
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val cameraHeightCm by viewModel.cameraHeightCm.collectAsState()
    val sensorAlpha by viewModel.sensorAlpha.collectAsState()
    val vibrateOnAlignment by viewModel.vibrateOnAlignment.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Sheet Title & Close Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "關閉"
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Group 1: 測量參數設定 (General Settings)
            item {
                Text(
                    text = "測量參數設定",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Option 1: Unit
                        Text(
                            text = "預設測量單位",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("cm", "m", "in", "ft").forEach { unit ->
                                val isSelected = selectedUnit == unit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            MaterialTheme.shapes.small
                                        )
                                        .clickable { viewModel.setUnit(unit) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unit,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Option 2: Default hold height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "相機預設持機高度",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "用於投影與高度測量時估計基準點",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format("%.0f cm", cameraHeightCm),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = cameraHeightCm,
                            onValueChange = { viewModel.setCameraHeight(it) },
                            valueRange = 100f..200f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("100cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("140cm (預設)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("200cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Group 2: 感測器配置 (Sensor Settings)
            item {
                Text(
                    text = "感測器性能配置",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Option 1: Sensor Alpha
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "感測器雜訊平滑度",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        sensorAlpha <= 0.12f -> "強效平滑：最穩定、反應稍緩"
                                        sensorAlpha <= 0.25f -> "中度平滑：平衡穩定與即時性"
                                        else -> "弱效即時：反應最為迅速、有些微抖動"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format("%.2f", sensorAlpha),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = sensorAlpha,
                            onValueChange = { viewModel.setSensorAlpha(it) },
                            valueRange = 0.05f..0.5f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Option 2: Vibration Alignment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "水平儀完美對齊震動回饋",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "於 0° 水平完美對齊時提供觸覺震動提示",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Switch(
                                checked = vibrateOnAlignment,
                                onCheckedChange = { viewModel.setVibrateOnAlignment(it) }
                            )
                        }
                    }
                }
            }

            // Group 3: 進階控制與系統 (System Settings & Actions)
            item {
                Text(
                    text = "校準與歷史備份",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sensor Re-caliber
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "水平儀與陀螺儀基準校準",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "將手機平放於表面，以目前姿態作為零度參考",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { 
                                    viewModel.calibrateSensors()
                                    android.widget.Toast.makeText(context, "已基準歸零校準！", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("立即校準", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Reset sensor Calibration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "重設校準偏置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "還原出廠姿態偏置配置",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            OutlinedButton(
                                onClick = { 
                                    viewModel.resetCalibration()
                                    android.widget.Toast.makeText(context, "已重置偏置為出廠預設值", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("重設偏差", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Clear Database
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "清除所有測量記錄數據",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "清除所有已保存的歷史測量資料",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { showClearConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("清除所有", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("確定清除所有數據？") },
            text = { Text("此動作將永久刪除所有的相機投影測量、螢幕直尺保存記錄，且無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllRecords()
                        showClearConfirm = false
                        android.widget.Toast.makeText(context, "所有歷史數據已清除！", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("確定清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
