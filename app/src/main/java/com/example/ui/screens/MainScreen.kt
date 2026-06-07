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
import com.example.ui.components.BubbleLevelComponent
import com.example.ui.components.CameraViewComponent
import com.example.ui.components.RulerComponent
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasureViewModel
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Slate900)) {
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
                    color = Slate900,
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
                                else -> "高精度水平儀"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MeasureYellow
                        )

                        // Quick Unit Selector Capsule HUD
                        Row(
                            modifier = Modifier
                                .background(Slate800, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("cm", "m", "in", "ft").forEach { unit ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedUnit == unit) MeasureYellow else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setUnit(unit) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = unit,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedUnit == unit) Color.Black else CadetBlue
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
                            2 -> BubbleLevelComponent(pitch = pitch, roll = roll)
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
                        icon = { Icon(Icons.Default.Explore, contentDescription = "水平儀") },
                        label = { Text("水平儀") },
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

        Spacer(modifier = Modifier.height(12.dp))

        if (savedRecords.isEmpty()) {
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
                    "尚無儲存的測量數據",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CadetBlue
                )
                Text(
                    "使用相機 AR 與螢幕尺子測量，並點擊「儲存」按鈕來記錄您的數據。",
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
                items(savedRecords, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate700)
                    ) {
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
                                    onClick = { onDeleteRecord(record.id) },
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
                    }
                }
            }
        }
    }
}
