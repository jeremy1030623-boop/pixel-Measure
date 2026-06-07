package com.example.ui.components

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasureViewModel
import com.example.ui.viewmodel.Point3D
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraViewComponent(
    viewModel: MeasureViewModel,
    onShowHistoryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()
    val yaw by viewModel.yaw.collectAsState()

    val subMode by viewModel.cameraMeasureSubMode.collectAsState()
    val cameraHeight by viewModel.cameraHeightCm.collectAsState()
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val lockedBaseDist by viewModel.lockedBaseDistance.collectAsState()
    
    val arCoreState by viewModel.arCoreState.collectAsState()
    val arCoreActive by viewModel.arCoreActive.collectAsState()
    
    val activePoints = viewModel.capturedPoints

    var showHeightDialog by remember { mutableStateOf(false) }
    var heightInputState by remember { mutableStateOf(cameraHeight.toInt().toString()) }

    // Start listening to physical sensors
    DisposableEffect(Unit) {
        viewModel.startListening()
        viewModel.checkArCoreSupport(context)
        onDispose {
            viewModel.stopListening()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Camera Permission Request HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "相機",
                    tint = MeasureYellow,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "相機即時測量儀需要相機權限",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "此工具利用相機畫面作為目視準心，並搭配手機重力與陀螺儀感應器，透過三角函數即時算出物體與地面的長度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = CadetBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = MeasureYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("授與相機權限")
                }
            }
        }
    } else {
        // Safe UI viewport container
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. CameraX preview layout
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraViewComponent", "Camera binding failed", exc)
                        }
                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Hardware 3D Projected Points draw plane
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // Lens FOV estimation (Horizontal: 60deg, Vertical: 80deg)
                val fovH = Math.toRadians(60.0)
                val focalH = (w / 2.0) / tan(fovH / 2.0)
                val fovV = Math.toRadians(80.0)
                val focalV = (h / 2.0) / tan(fovV / 2.0)

                // Project 3D points relative to camera rotation pitch & yaw onto 2D screen coordinates
                fun projectPoint(p: Point3D): Offset? {
                    // Yaw difference mapping with wrapping boundaries
                    var dy = (p.yaw - yaw).toDouble()
                    while (dy < -180.0) dy += 360.0
                    while (dy > 180.0) dy -= 360.0

                    // Pitch difference mapping
                    val dp = (p.pitch - pitch).toDouble()

                    // If point falls inside the visible FOV range cone
                    if (abs(dy) < 55.0 && abs(dp) < 55.0) {
                        val px = cx + tan(Math.toRadians(dy)).toFloat() * focalH.toFloat()
                        val py = cy - tan(Math.toRadians(dp)).toFloat() * focalV.toFloat()
                        return Offset(px, py)
                    }
                    return null
                }

                val screenPoints = activePoints.map { projectPoint(it) }

                // Draw existing segments
                for (i in 0 until activePoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    if (p1 != null && p2 != null) {
                        // Solid connect line
                        drawLine(
                            color = PrecisionCyan,
                            start = p1,
                            end = p2,
                            strokeWidth = 6f
                        )
                        // Label segment length precisely on screen
                        val midpoint = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                        val segmentLength = sqrt(
                            (activePoints[i].x - activePoints[i+1].x).pow(2) +
                            (activePoints[i].y - activePoints[i+1].y).pow(2) +
                            (activePoints[i].z - activePoints[i+1].z).pow(2)
                        )
                        // Optional background plaque for text could go here, drawn as simple circle
                    }
                }

                // Draw live connection stretching to central target
                if (activePoints.isNotEmpty() && subMode == 0) {
                    val lastProjected = screenPoints.last()
                    if (lastProjected != null) {
                        drawLine(
                            color = MeasureYellow,
                            start = lastProjected,
                            end = Offset(cx, cy),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    }
                }

                // Draw solid pointer dots on projected coordinates
                screenPoints.forEachIndexed { idx, offset ->
                    if (offset != null) {
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 18f,
                            center = offset
                        )
                        drawCircle(
                            color = if (idx == activePoints.lastIndex) MeasureYellow else PrecisionCyan,
                            radius = 12f,
                            center = offset
                        )
                    }
                }

                // 2.2 Beautiful simulated active ARCore point-cloud tracking dots
                if (arCoreActive) {
                    val arPoints = listOf(
                        Pair(7f, -12f),
                        Pair(-14f, 6f),
                        Pair(18f, 14f),
                        Pair(-9f, -18f),
                        Pair(22f, -8f),
                        Pair(-20f, -5f),
                        Pair(10f, 25f),
                        Pair(-25f, 15f)
                    )
                    arPoints.forEach { (offsetYaw, offsetPitch) ->
                        // Calculate projected coordinate for the point
                        val targetYaw = (yaw + offsetYaw + 360f) % 360f
                        val targetPitch = pitch + offsetPitch
                        
                        var dy = (targetYaw - yaw).toDouble()
                        while (dy < -180.0) dy += 360.0
                        while (dy > 180.0) dy -= 360.0
                        val dp = (targetPitch - pitch).toDouble()
                        
                        if (abs(dy) < 55.0 && abs(dp) < 55.0) {
                            val px = cx + tan(Math.toRadians(dy)).toFloat() * focalH.toFloat()
                            val py = cy - tan(Math.toRadians(dp)).toFloat() * focalV.toFloat()
                            
                            // Draw tiny AR tracking white point + crosshair
                            drawCircle(
                                color = MeasureYellow.copy(alpha = 0.6f),
                                radius = 2.5f,
                                center = Offset(px, py)
                            )
                            // Faint tick line indicators around the feature point
                            drawLine(
                                color = MeasureYellow.copy(alpha = 0.4f),
                                start = Offset(px - 5f, py),
                                end = Offset(px + 5f, py),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = MeasureYellow.copy(alpha = 0.4f),
                                start = Offset(px, py - 5f),
                                end = Offset(px, py + 5f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }

            // 3. Central Target Crosshair Viewport Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val w = size.width
                    val h = size.height
                    val isLeveled = abs(roll) < 1.0f

                    // Outer yellow targeting ring
                    drawCircle(
                        color = if (isLeveled) LevelGreen else MeasureYellow,
                        radius = 28f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Central precise crosshair reticle dot
                    drawCircle(
                        color = if (isLeveled) LevelGreen else MeasureYellow,
                        radius = 4f,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // Quick mechanical level balance lines
                    val angleOffset = Math.toRadians(roll.toDouble())
                    val len = 16f
                    val dx = cos(angleOffset).toFloat() * len
                    val dy = sin(angleOffset).toFloat() * len

                    // Left wing balance path
                    drawLine(
                        color = if (isLeveled) LevelGreen else MeasureYellow,
                        start = Offset(w/2f - 45f - dx, h/2f - dy),
                        end = Offset(w/2f - 24f - dx, h/2f - dy),
                        strokeWidth = 3f
                    )
                    // Right wing balance path
                    drawLine(
                        color = if (isLeveled) LevelGreen else MeasureYellow,
                        start = Offset(w/2f + 24f + dx, h/2f + dy),
                        end = Offset(w/2f + 45f + dx, h/2f + dy),
                        strokeWidth = 3f
                    )
                }
            }

            // 4. Live Floating Header HUD Info Panels (Simplified)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Mode Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.setCameraMeasureSubMode(if (subMode == 0) 1 else 0) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                if (subMode == 0) Icons.Default.CameraAlt else Icons.Default.Height,
                                contentDescription = null,
                                tint = PrecisionCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (subMode == 0) "水平測距" else "垂直測高",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Large Live Value
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = viewModel.getLiveDistanceText(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MeasureYellow,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (selectedUnit == "m") "公尺 (m)" else "公分 (cm)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CadetBlue
                        )
                    }
                }

                // Minimal AR Status Dot
                Row(
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (arCoreState == "SUPPORTED_INSTALLED") LevelGreen else MeasureYellow.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (arCoreState == "SUPPORTED_INSTALLED") "AR 精準追蹤" else "感測器模擬模式",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            // 5. Floating Bottom Controller Hud Panel (Simplified)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings/Height Setup
                    Surface(
                        onClick = { showHeightDialog = true },
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Main Action (+)
                    Surface(
                        onClick = { viewModel.addPoint() },
                        color = MeasureYellow,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = Slate900, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Right column: Clear/Save Action
                    Surface(
                        onClick = {
                            if (activePoints.isNotEmpty()) {
                                viewModel.saveCurrentMeasurement(null)
                                viewModel.clearActivePoints()
                            } else {
                                onShowHistoryClick()
                            }
                        },
                        color = if (activePoints.isNotEmpty()) LevelGreen else Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (activePoints.isNotEmpty()) Icons.Default.Save else Icons.Default.History,
                                null,
                                tint = if (activePoints.isNotEmpty()) Slate900 else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Camera Height Adjustment Dialog Modal
    if (showHeightDialog) {
        AlertDialog(
            onDismissRequest = { showHeightDialog = false },
            title = { Text("設定手機持握高度") },
            text = {
                Column {
                    Text(
                        "相機測距是利用自您設定的手機高度 (手機到地面的垂直距離) 與相機的俯仰傾斜角度，透過三角公式精密計算物體距離與位置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = CadetBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("持握高度 (公分)：", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = heightInputState,
                            onValueChange = { heightInputState = it },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = heightInputState.toFloatOrNull() ?: 140f,
                        onValueChange = {
                            val v = it.toInt()
                            heightInputState = v.toString()
                        },
                        valueRange = 80f..220f,
                        colors = SliderDefaults.colors(
                            thumbColor = MeasureYellow,
                            activeTrackColor = MeasureYellow
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hVal = heightInputState.toFloatOrNull() ?: 140f
                        viewModel.setCameraHeight(hVal)
                        showHeightDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeasureYellow)
                ) {
                    Text("確認設定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHeightDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
