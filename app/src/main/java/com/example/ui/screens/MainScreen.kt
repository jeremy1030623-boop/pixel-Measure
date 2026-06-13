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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CameraViewComponent
import com.example.ui.components.RulerComponent
import com.example.ui.components.SurfaceLevelComponent
import com.example.ui.components.OnboardingTutorialOverlay
import com.example.ui.components.InitialWelcomeScreen
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
    val isFirstTimeUser by viewModel.isFirstTimeUser.collectAsState()
    val showSplashScreen by viewModel.showSplashScreen.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    
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
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                            FilledTonalIconButton(
                                                onClick = { viewModel.calibrateSensors() },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                ),
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .size(48.dp)
                                                    .testTag("calibrate_sensor_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.Balance,
                                                    contentDescription = "校準感應器",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // Settings Button
                                            FilledTonalIconButton(
                                                onClick = { showSettingsSheet = true },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .size(48.dp)
                                                    .testTag("settings_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = "開啟設定",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
 
                                            // Non-interactive current unit pill
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.shapes.medium
                                                    )
                                                    .clickable {
                                                        val msg = if (currentLang.startsWith("zh")) "請由「設定」中變更測量單位" else "Please change measurement units in Settings"
                                                         android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (currentLang.startsWith("zh")) "單位: $selectedUnit" else "Unit: $selectedUnit",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
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
                                contentDescription = viewModel.getString("nav_camera"),
                                modifier = Modifier.graphicsLayer(scaleX = iconScale0, scaleY = iconScale0)
                            ) 
                        },
                        label = { Text(viewModel.getString("nav_camera")) },
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
                                contentDescription = viewModel.getString("nav_ruler"),
                                modifier = Modifier.graphicsLayer(scaleX = iconScale1, scaleY = iconScale1)
                            ) 
                        },
                        label = { Text(viewModel.getString("nav_ruler")) },
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
                                contentDescription = viewModel.getString("nav_level"),
                                modifier = Modifier.graphicsLayer(scaleX = iconScale2, scaleY = iconScale2)
                            ) 
                        },
                        label = { Text(viewModel.getString("nav_level")) },
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
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    HistoryContentPane(
                        viewModel = viewModel,
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
                        viewModel = viewModel,
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

        if (showSplashScreen) {
            InitialWelcomeScreen(
                viewModel = viewModel,
                onEnterApp = { viewModel.dismissSplashScreen() }
            )
        } else if (isFirstTimeUser) {
            OnboardingTutorialOverlay(
                viewModel = viewModel,
                onDismiss = { viewModel.setFirstTimeUser(false) }
            )
        }
    }
}

// Reusable custom Measurement History Logs List Pane
@Composable
fun HistoryContentPane(
    viewModel: com.example.ui.viewmodel.MeasureViewModel,
    savedRecords: List<com.example.data.model.MeasureRecord>,
    onDeleteRecord: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    val currentLang by viewModel.currentLanguage.collectAsState()
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
                if (currentLang.startsWith("zh")) "儲存在地測量記錄" else viewModel.getString("history_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (savedRecords.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(viewModel.getString("clear_all"), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedRecords.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(viewModel.getString("search_placeholder"), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                    contentDescription = if (currentLang.startsWith("zh")) "無記錄" else "No Records",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val emptyStateText = if (currentLang.startsWith("zh")) {
                    if (savedRecords.isEmpty()) "尚無儲存的測量數據" else "無匹配的搜尋結果"
                } else {
                    if (savedRecords.isEmpty()) viewModel.getString("no_records") else "No matching search results"
                }
                
                val emptyStateSubText = if (currentLang.startsWith("zh")) {
                    if (savedRecords.isEmpty()) "使用相機 AR 與螢幕尺子測量，並點擊「儲存」按鈕來記錄您的數據。" else "嘗試更換其他搜尋關鍵字後再行重試。"
                } else {
                    if (savedRecords.isEmpty()) "Perform measurements using AR Camera or Screen Ruler and click Save." else "Try typing a different search keyword and retry."
                }
                
                Text(
                    emptyStateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    emptyStateSubText,
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
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "刪除此筆測量紀錄",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(24.dp)
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
                                            if (currentLang.startsWith("zh")) "備註分析:" else "Remarks Analysis:",
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
                                                if (currentLang.startsWith("zh")) "AR 測量標註分析 (${points.size} 個空間標點):" else "AR Annotation Analysis (${points.size} space points):",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            points.forEachIndexed { pIdx, pt ->
                                                val pointName = if (pt.label.isNotBlank()) pt.label else if (currentLang.startsWith("zh")) "標註點 ${pIdx + 1}" else "Point ${pIdx + 1}"
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
                                        if (currentLang.startsWith("zh")) "分享與備份導出:" else "Export & Share Report:",
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
                                                Text(if (currentLang.startsWith("zh")) "文字" else "Text", style = MaterialTheme.typography.labelMedium)
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
                                                Text(if (currentLang.startsWith("zh")) "圖片" else "Image", style = MaterialTheme.typography.labelMedium)
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
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val arCoreActive by viewModel.arCoreActive.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (currentLang.startsWith("zh")) "精密量測設定" else "Precision Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentLang.startsWith("zh")) "校準偏置・控制元件參數與性能優化" else "Calibrate offset, filter parameters & performance optimization",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (currentLang.startsWith("zh")) "關閉設定頁面" else "Close Settings Page",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Divider(
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Group 1: 測量參數規格
            item {
                SettingsGroupHeader(
                    icon = Icons.Default.Straighten,
                    title = if (currentLang.startsWith("zh")) "測量規格與參考高度" else "Measurement Spec & Reference Height",
                    iconColor = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Standard Unit Selection row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "預設量測單位" else "Default Measurement Unit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "設定相機深度、面積與直尺的基準單位" else "Set default unit for camera, area & screen rulers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("cm", "m", "in", "ft").forEach { unit ->
                                val isSelected = selectedUnit == unit
                                val labelText = when(unit) {
                                    "cm" -> if (currentLang.startsWith("zh")) "公分 (cm)" else "Centimeter (cm)"
                                    "m" -> if (currentLang.startsWith("zh")) "公尺 (m)" else "Meter (m)"
                                    "in" -> if (currentLang.startsWith("zh")) "英吋 (in)" else "Inch (in)"
                                    "ft" -> if (currentLang.startsWith("zh")) "英呎 (ft)" else "Feet (ft)"
                                    else -> unit
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setUnit(unit) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )

                        // Camera hold height selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewModel.getString("height_preset"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = viewModel.getString("height_preset_desc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format("%.0f cm", cameraHeightCm),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
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
                            Text("100 cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(if (currentLang.startsWith("zh")) "140 cm (預設)" else "140 cm (Default)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("200 cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )

                        // ARCore spatial tracking toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ViewInAr,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = viewModel.getString("arcore_3d"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = viewModel.getString("arcore_3d_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = arCoreActive,
                                onCheckedChange = { viewModel.setArCoreActive(it) }
                            )
                        }
                    }
                }
            }

            // Group 2: 感測器配置
            item {
                SettingsGroupHeader(
                    icon = Icons.Default.Tune,
                    title = if (currentLang.startsWith("zh")) "感應器動態與震動反饋" else "Sensors Dynamics & Haptic Feedback",
                    iconColor = MaterialTheme.colorScheme.secondary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Smoothing alpha parameter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewModel.getString("sensor_alpha"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) {
                                        when {
                                            sensorAlpha <= 0.12f -> "強效限幅：極高穩定、反應微緩"
                                            sensorAlpha <= 0.25f -> "標準平滑：平衡雜訊與即時指向"
                                            else -> "高靈敏度：低阻延遲、些微數值抖動"
                                        }
                                    } else {
                                        when {
                                            sensorAlpha <= 0.12f -> "Strong Alpha: Highly stable, slightly slower reaction"
                                            sensorAlpha <= 0.25f -> "Standard Alpha: Optimal noise filtering and instant response"
                                            else -> "Sensitive Alpha: Lowest latency with minor signal fluctuations"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format("%.2f α", sensorAlpha),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = sensorAlpha,
                            onValueChange = { viewModel.setSensorAlpha(it) },
                            valueRange = 0.05f..0.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (currentLang.startsWith("zh")) "高流暢性" else "More Smooth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(if (currentLang.startsWith("zh")) "0.20 (預設)" else "0.20 (Default)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(if (currentLang.startsWith("zh")) "高即時性" else "More Instant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )

                        // Vib alignment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = viewModel.getString("vibrate_on_align"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentLang.startsWith("zh")) "角度為 0.0° 時，發起輕微物理短震震動" else "Emit a momentary short haptic tick once the slope hits perfect 0.0°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = vibrateOnAlignment,
                                onCheckedChange = { viewModel.setVibrateOnAlignment(it) }
                            )
                        }
                    }
                }
            }

            // Group 3: 介面外觀設定
            item {
                SettingsGroupHeader(
                    icon = Icons.Default.Palette,
                    title = if (currentLang.startsWith("zh")) "系統色彩與外觀主題" else "System Palette & Visual Theme",
                    iconColor = MaterialTheme.colorScheme.tertiary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = viewModel.getString("dynamic_color"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentLang.startsWith("zh")) "支援 Android 12+ 系統桌布色調自適應" else "Support dynamic colors synced with Android 12+ wallpaper",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = dynamicColorEnabled,
                                onCheckedChange = { viewModel.setDynamicColorEnabled(it) }
                            )
                        }

                        // Google Pixel Android 14+ specific Monet Dynamic Color System signature badge
                        if (dynamicColorEnabled) {
                            val isPixelDevice = remember {
                                android.os.Build.MANUFACTURER.equals("Google", ignoreCase = true) ||
                                android.os.Build.BRAND.equals("Google", ignoreCase = true) ||
                                android.os.Build.MODEL.startsWith("Pixel", ignoreCase = true) ||
                                android.os.Build.PRODUCT.startsWith("pixel", ignoreCase = true)
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isPixelDevice,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (currentLang.startsWith("zh")) 
                                                    "已偵測到首選 Google Pixel 裝置並完全最佳化！已成功連結系統智慧莫重 (Monet) 壁紙自適應配色引擎。" 
                                                    else "Google Pixel device fully optimized! Connected with system-wide Material You (Monet) wallpaper dynamic accent colors.",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    lineHeight = 15.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Language Selection section
            item {
                SettingsGroupHeader(
                    icon = Icons.Default.Language,
                    title = viewModel.getString("select_lang"),
                    iconColor = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val currentLangName = remember(currentLang) {
                        com.example.logic.TranslationManager.supportedLanguages.find { it.code == currentLang }?.name ?: "English"
                    }
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "顯示語系" else "Display Language",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "設定 33 種多國系統語系對應顯示" else "Switch database and menus between 33 languages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Box {
                                Button(
                                    onClick = { expanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(currentLangName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Icon(
                                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .width(220.dp)
                                        .heightIn(max = 320.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    com.example.logic.TranslationManager.supportedLanguages.forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = lang.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (currentLang == lang.code) FontWeight.ExtraBold else FontWeight.Normal,
                                                    color = if (currentLang == lang.code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                viewModel.setLanguage(lang.code)
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

            // Group 4: 進階物理校準與系統控制
            item {
                SettingsGroupHeader(
                    icon = Icons.Default.Build,
                    title = if (currentLang.startsWith("zh")) "感應器基準校準與歷史備份" else "Sensors Calibration & Local Storage Management",
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Calibrate Zero State Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "設定當前角度為全新零點 (0°)" else "Calibrate current posture as zero point (0°)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "將當前擺放姿勢記為全新的平行參考點" else "Establish present sensor reading values as the flat reference angle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { 
                                    viewModel.calibrateSensors()
                                    android.widget.Toast.makeText(context, viewModel.getString("toast_calibrated"), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(viewModel.getString("calibration_zero"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Reset offsets to true manufacturer defaults
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "還原出廠硬體姿態偏置" else "Restore default physical sensor offsets",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "清除所有自定義的零度偏置基準數據" else "Wipe custom zero-level alignment criteria",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { 
                                    viewModel.resetCalibration()
                                    android.widget.Toast.makeText(context, viewModel.getString("toast_reset"), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(viewModel.getString("reset_deviation"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Trigger Tutorial Guide View Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "重新閱讀量測操作教學與導覽" else "Relaunch Interactive Interface Tutorial",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "重新開啟首頁互動指引與全套工具操作指南" else "Unlock full onboarding and tutorial user guidelines",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { 
                                    viewModel.setFirstTimeUser(true)
                                    onDismiss()
                                    android.widget.Toast.makeText(context, viewModel.getString("toast_tutorial"), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(viewModel.getString("tutorial_title"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Danger Zone Cleanse Data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang.startsWith("zh")) "永久清除所有歷史量測紀錄" else "Permanently Clear All History Records",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (currentLang.startsWith("zh")) "包含全部已保存的3D相機及直尺條目，此操作不可回復" else "Includes all saved 3D camera measurements & screen rulers; this cannot be undone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showClearConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(viewModel.getString("clear_all"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentLang.startsWith("zh")) "設定皆會自動儲存於本地。如遇水平感應數值有偏誤，請將手機平放於桌面上，並點擊「校準歸零」建立全新基準線。" else "All preferences are auto-saved. If sensor metrics mismatch, place the phone flat on a workspace and tap 'Calibrate Zero' to configure raw bias references.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        viewModel.getString("clear_confirm_title"),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = { Text(viewModel.getString("clear_confirm_desc")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllRecords()
                        showClearConfirm = false
                        val msg = if (currentLang.startsWith("zh")) "所有歷史數據已成功清除！" else "All historical records successfully wiped!"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(viewModel.getString("ok_clear"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(viewModel.getString("cancel"))
                }
            }
        )
    }
}

@Composable
fun SettingsGroupHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = iconColor,
            letterSpacing = 0.5.sp
        )
    }
}
