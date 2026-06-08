package com.example.ui.screens

import androidx.compose.animation.*
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
    
    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWideScreen = maxWidth >= 680.dp

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
                        Text(
                            text = when (currentMode) {
                                0 -> "相機 AR 測量"
                                1 -> "螢幕卡鉗直尺"
                                else -> "表面水平校正"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Quick Unit Selector Capsule HUD
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("cm", "m", "in", "ft").forEach { unit ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedUnit == unit) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setUnit(unit) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = unit,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedUnit == unit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            fadeIn() togetherWith fadeOut()
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
                            2 -> SurfaceLevelComponent(pitch = pitch, roll = roll)
                        }
                    }
                }

                // Visual Bottom Navigation tabs bar
                NavigationBar(
                    containerColor = Slate900,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentMode == 0,
                        onClick = { viewModel.setMode(0) },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "相機") },
                        label = { Text("相機 AR") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MeasureYellow,
                            unselectedIconColor = CadetBlue,
                            unselectedTextColor = CadetBlue,
                            indicatorColor = MeasureYellow
                        )
                    )
                    NavigationBarItem(
                        selected = currentMode == 1,
                        onClick = { viewModel.setMode(1) },
                        icon = { Icon(Icons.Default.Straighten, contentDescription = "直尺") },
                        label = { Text("螢幕尺") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MeasureYellow,
                            unselectedIconColor = CadetBlue,
                            unselectedTextColor = CadetBlue,
                            indicatorColor = MeasureYellow
                        )
                    )
                    NavigationBarItem(
                        selected = currentMode == 2,
                        onClick = { viewModel.setMode(2) },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "表面水平") },
                        label = { Text("表面水平") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MeasureYellow,
                            unselectedIconColor = CadetBlue,
                            unselectedTextColor = CadetBlue,
                            indicatorColor = MeasureYellow
                        )
                    )
                }
            }

            // Wide Tablet side-by-side History Panel (Material 3 Supporting Pane Layout)
            if (isWideScreen) {
                VerticalDivider(color = Slate800, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(Slate800)
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

        // Phone standard Bottom sheet overlay
        if (showHistorySheet && !isWideScreen) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                containerColor = Slate800,
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
                color = Color.White
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
                placeholder = { Text("搜尋名稱或備註描述...", color = CadetBlue) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CadetBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = CadetBlue)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeasureYellow,
                    unfocusedBorderColor = Slate600,
                    focusedContainerColor = Slate700,
                    unfocusedContainerColor = Slate700
                ),
                shape = RoundedCornerShape(12.dp),
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
                    tint = CadetBlue.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (savedRecords.isEmpty()) "尚無儲存的測量數據" else "無匹配的搜尋結果",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CadetBlue
                )
                Text(
                    if (savedRecords.isEmpty()) "使用相機 AR 與螢幕尺子測量，並點擊「儲存」按鈕來記錄您的數據。" else "嘗試更換其他搜尋關鍵字後再行重試。",
                    style = MaterialTheme.typography.bodySmall,
                    color = CadetBlue.copy(alpha = 0.6f),
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
                            .clickable { expandedRecordId = if (isExpanded) null else record.id },
                        colors = CardDefaults.cardColors(containerColor = Slate700)
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
                                                if (record.type == "CAM") Color(0x330EA5E9) else Color(0x33F59E0B),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (record.type == "CAM") Icons.Default.CameraAlt else Icons.Default.Straighten,
                                            contentDescription = "模式",
                                            tint = if (record.type == "CAM") PrecisionCyan else MeasureYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateTimeFormatter.format(Date(record.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CadetBlue
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
                                        color = MeasureYellow
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
                                    color = Slate600.copy(alpha = 0.5f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!record.notes.isNullOrBlank()) {
                                        Text(
                                            "備註分析:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MeasureYellow
                                        )
                                        Text(
                                            record.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f)
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
                                                color = PrecisionCyan
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            points.forEachIndexed { pIdx, pt ->
                                                val pointName = if (pt.label.isNotBlank()) pt.label else "標註點 ${pIdx + 1}"
                                                Text(
                                                    " • $pointName: (X:${String.format("%.2f", pt.x)}, Y:${String.format("%.2f", pt.y)}, Z:${String.format("%.2f", pt.z)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = CadetBlue
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
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { com.example.logic.ShareUtility.shareTextReport(context, record) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate600, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate600, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate600, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
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
