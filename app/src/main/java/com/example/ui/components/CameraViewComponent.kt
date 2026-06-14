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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import android.app.Activity
import com.example.logic.ShareUtility
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasureViewModel
import com.example.ui.viewmodel.Point3D
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraViewComponent(
    viewModel: MeasureViewModel,
    onShowHistoryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()
    val yaw by viewModel.yaw.collectAsState()

    val subMode by viewModel.cameraMeasureSubMode.collectAsState()
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val lockedBaseDist by viewModel.lockedBaseDistance.collectAsState()
    val cameraHeightCm by viewModel.cameraHeightCm.collectAsState()
    
    val arCoreState by viewModel.arCoreState.collectAsState()
    val arCoreActive by viewModel.arCoreActive.collectAsState()
    val arTrackingState by viewModel.arTrackingState.collectAsState()
    val arPointCloud by viewModel.arPointCloud.collectAsState()
    val viewMatrix by viewModel.viewMatrix.collectAsState()
    val projectionMatrix by viewModel.projectionMatrix.collectAsState()
    val arPlanes = viewModel.arPlanes
    
    val currentLang by viewModel.currentLanguage.collectAsState()
    
    val activePoints = viewModel.capturedPoints
    
    // Manage dynamic ripple pings at capture coordinates
    val pings = remember { mutableStateListOf<Pair<Offset, Animatable<Float, AnimationVector1D>>>() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Observe haptic events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.hapticEvent.collect {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val colorOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorSurface = MaterialTheme.colorScheme.surface
    val colorSurfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorOutline = MaterialTheme.colorScheme.outline
    val colorBackground = MaterialTheme.colorScheme.background

    // Infinite transitions for smooth breathing and radar scan ripples
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    val radarWaveRadius by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarWaveRadius"
    )
    val radarWaveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarWaveAlpha"
    )

    // Bouncy physical entry feedback for the most recently added point
    val scaleAnimState = remember { Animatable(0f) }
    LaunchedEffect(activePoints.size) {
        if (activePoints.isNotEmpty()) {
            scaleAnimState.snapTo(0.2f)
            scaleAnimState.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Save states for AR Measurements
    var showSaveMeasurementDialog by remember { mutableStateOf(false) }
    var saveTitleTextState by remember { mutableStateOf("") }
    var saveNotesTextState by remember { mutableStateOf("") }

    // Active point label editing states
    var editingPointIndex by remember { mutableStateOf<Int?>(null) }
    var pointLabelInputState by remember { mutableStateOf("") }

    // ARCore Lifecycle sync
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ARCore Frame update loop
    LaunchedEffect(arCoreState, arCoreActive) {
        if (arCoreState == "SUPPORTED_INSTALLED" && !arCoreActive) {
            while (true) {
                val session = viewModel.arSession
                if (session != null) {
                    try {
                        val frame = session.update()
                        viewModel.updateArFrame(frame)
                    } catch (e: Exception) {}
                }
                kotlinx.coroutines.delay(16)
            }
        }
    }

    // Start listening to physical sensors
    DisposableEffect(Unit) {
        viewModel.startListening()
        onDispose {
            viewModel.destroyArCore()
        }
    }

    // Start ARCore checking only after camera permission is granted
     LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.checkArCoreSupport(context)
        }
    }

    // CameraX Lifecycle management
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {}
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {}
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Camera Permission Request HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = colorPrimary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = viewModel.getString("perm_camera_title"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorOnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val status = cameraPermissionState.status
                val infoText = if (status.shouldShowRationale) {
                    viewModel.getString("perm_camera_rationale")
                } else {
                    viewModel.getString("perm_camera_desc")
                }
                
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                ) {
                    Text(viewModel.getString("btn_grant_perm"), fontWeight = FontWeight.Bold)
                }
                
                if (!status.shouldShowRationale) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }) {
                        Text(
                            viewModel.getString("btn_open_settings"),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = viewModel.getString("perm_camera_denied"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorOnSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    } else {
        // Safe UI viewport container
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Conditionally render ARCore GLView or CameraX preview layout
            val arSession = viewModel.arSession
            if (arCoreActive && arCoreState == "SUPPORTED_INSTALLED" && arSession != null) {
                // 解除 CameraX 綁定以避免相機權限衝突
                LaunchedEffect(Unit) {
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        Log.e("CameraViewComponent", "Failed to force unbind CameraX on ARCore startup", e)
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        com.example.logic.ArCoreGLView(ctx, arSession, viewModel)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                
                                // Trigger persistent visual multi-ripple ping
                                val pingAnim1 = Animatable(0f)
                                val pingAnim2 = Animatable(0f)
                                val pingPair1 = offset to pingAnim1
                                val pingPair2 = offset to pingAnim2
                                pings.add(pingPair1)
                                coroutineScope.launch {
                                    pingAnim1.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
                                    pings.remove(pingPair1)
                                }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(180)
                                    pings.add(pingPair2)
                                    pingAnim2.animateTo(1f, animationSpec = tween(600, easing = LinearOutSlowInEasing))
                                    pings.remove(pingPair2)
                                }
                                
                                // Sync geometry info before hitTest to ensure pixel-to-world accuracy
                                val display = (context as android.app.Activity).windowManager.defaultDisplay
                                viewModel.updateDisplayGeometry(display.rotation, view.width, view.height)

                                // Using pixel coordinates directly for ARCore hitTest
                                viewModel.addPoint(offset.x, offset.y)
                            }
                        }
                )
            } else {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                
                                // Trigger persistent visual multi-ripple ping
                                val pingAnim1 = Animatable(0f)
                                val pingAnim2 = Animatable(0f)
                                val pingPair1 = offset to pingAnim1
                                val pingPair2 = offset to pingAnim2
                                pings.add(pingPair1)
                                coroutineScope.launch {
                                    pingAnim1.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
                                    pings.remove(pingPair1)
                                }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(180)
                                    pings.add(pingPair2)
                                    pingAnim2.animateTo(1f, animationSpec = tween(600, easing = LinearOutSlowInEasing))
                                    pings.remove(pingPair2)
                                }
                                
                                // Sync geometry info before hitTest to ensure pixel-to-world accuracy
                                val display = (context as android.app.Activity).windowManager.defaultDisplay
                                viewModel.updateDisplayGeometry(display.rotation, view.width, view.height)

                                // Using pixel coordinates directly for ARCore hitTest
                                viewModel.addPoint(offset.x, offset.y)
                            }
                        }
                )
            }

            // Dynamic dash offset for crawling animation
            val dashOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "DashAnim"
            )

            // 2. Hardware 3D Projected Points draw plane
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // Draw active pings
                pings.forEach { (offset, anim) ->
                    val progress = anim.value
                    drawCircle(
                        color = colorPrimary.copy(alpha = 1f - progress),
                        radius = 120f * progress,
                        center = offset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f * (1f - progress))
                    )
                }

                // Lens FOV estimation (Horizontal: 60deg, Vertical: 80deg)
                val fovH = Math.toRadians(60.0)
                val focalH = (w / 2.0) / tan(fovH / 2.0)
                val fovV = Math.toRadians(80.0)
                val focalV = (h / 2.0) / tan(fovV / 2.0)

                // Project 3D points relative to camera rotation pitch & yaw onto 2D screen coordinates
                fun projectPoint(p: Point3D): Offset? {
                    // Try ARCore precision projection if tracked
                    if (arTrackingState == com.google.ar.core.TrackingState.TRACKING) {
                        val offset = com.example.logic.ARProjectionUtils.projectPoint(
                            p.x.toFloat(), p.y.toFloat(), p.z.toFloat(),
                            viewMatrix, projectionMatrix, w, h
                        )
                        if (offset != null) return offset
                    }

                    // Fallback to orientation-based projection for non-AR or lost tracking
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

                // 2.05 Draw closed polygon filling if >= 3 points on horizontal ground mode
                if (activePoints.size >= 3 && !viewModel.isAutoMeasuringHeight(pitch)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        var first = true
                        screenPoints.forEach { pt ->
                            if (pt != null) {
                                if (first) {
                                    moveTo(pt.x, pt.y)
                                    first = false
                                } else {
                                    lineTo(pt.x, pt.y)
                                }
                            }
                        }
                        close()
                    }
                    drawPath(
                        path = path,
                        color = colorPrimary.copy(alpha = 0.15f)
                    )
                    
                    // Also draw a dashed line connecting last point back to first point
                    val pFirst = screenPoints.first()
                    val pLast = screenPoints.last()
                    if (pFirst != null && pLast != null) {
                        drawLine(
                            color = colorPrimary.copy(alpha = 0.5f),
                            start = pLast,
                            end = pFirst,
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), dashOffset)
                        )
                    }
                }

                // Draw existing segments
                for (i in 0 until activePoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    if (p1 != null && p2 != null) {
                        // Dark backing line to provide absolute high contrast in any lighting
                        drawLine(
                            color = colorSurface.copy(alpha = 0.6f),
                            start = p1,
                            end = p2,
                            strokeWidth = 12f
                        )
                        // Precise solid line with crawling dash pulse
                        drawLine(
                            color = colorTertiary,
                            start = p1,
                            end = p2,
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), dashOffset)
                        )
                        // Label segment length precisely on screen
                        val midpoint = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                        val segmentLength = sqrt(
                            (activePoints[i].x - activePoints[i+1].x).pow(2.0) +
                            (activePoints[i].y - activePoints[i+1].y).pow(2.0) +
                            (activePoints[i].z - activePoints[i+1].z).pow(2.0)
                        )
                        val lengthStr = viewModel.formatLengthValue(segmentLength)

                        // Draw a stylish small dark bubble plaque for the line distance value for maximum legibility
                        val distPaint = android.text.TextPaint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            isAntiAlias = true
                        }
                        val distTextWidth = distPaint.measureText(lengthStr)
                        val distTextHeight = distPaint.textSize
                        val pX = 10f
                        val pY = 6f
                        
                        // Bubble container
                        drawRoundRect(
                            color = colorSurfaceContainer.copy(alpha = 0.9f),
                            topLeft = Offset(midpoint.x - distTextWidth / 2f - pX, midpoint.y - distTextHeight / 2f - pY),
                            size = Size(distTextWidth + pX * 2, distTextHeight + pY * 2),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                        // Border edge
                        drawRoundRect(
                            color = colorTertiary.copy(alpha = 0.8f),
                            topLeft = Offset(midpoint.x - distTextWidth / 2f - pX, midpoint.y - distTextHeight / 2f - pY),
                            size = Size(distTextWidth + pX * 2, distTextHeight + pY * 2),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )
                        // Center draw text
                        drawContext.canvas.nativeCanvas.drawText(
                            lengthStr,
                            midpoint.x - distTextWidth / 2f,
                            midpoint.y + distTextHeight / 2f - 4f,
                            distPaint
                        )
                    }
                }

                // Draw live connection stretching to central target
                if (activePoints.isNotEmpty() && !viewModel.isAutoMeasuringHeight(pitch)) {
                    val lastProjected = screenPoints.last()
                    if (lastProjected != null) {
                        // High-contrast back dashes
                        drawLine(
                            color = colorSurface.copy(alpha = 0.6f),
                            start = lastProjected,
                            end = Offset(cx, cy),
                            strokeWidth = 8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                        // Foreground bright dashes with animated crawling
                        drawLine(
                            color = colorPrimary,
                            start = lastProjected,
                            end = Offset(cx, cy),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), -dashOffset)
                        )
                    }
                }

                // Create native paint for custom AR text labels
                val labelPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }

                // Draw solid pointer dots on projected coordinates
                screenPoints.forEachIndexed { idx, offset ->
                    if (offset != null) {
                        val isLast = (idx == activePoints.lastIndex)
                        val baseColor = if (isLast) colorPrimary else colorTertiary
                        
                        // Calculate scale for this point (entrance spring scale if it is the latest point)
                        val currentScale = if (isLast && activePoints.isNotEmpty()) scaleAnimState.value else 1.0f
                        
                        // 1. Draw glowing/pulsing background halo
                        if (isLast) {
                            drawCircle(
                                color = baseColor.copy(alpha = radarWaveAlpha),
                                radius = radarWaveRadius * currentScale,
                                center = offset
                            )
                        } else {
                            // Breathe scale for existing points
                            drawCircle(
                                color = baseColor.copy(alpha = 0.25f),
                                radius = 22f * breatheScale,
                                center = offset
                            )
                        }

                        // 2. Main Marker dot container
                        drawCircle(
                            color = colorSurface.copy(alpha = 0.8f), 
                            radius = 18f * currentScale,
                            center = offset
                        )

                        // Outer ring stroke for technical scope appearance
                        drawCircle(
                            color = baseColor,
                            radius = 18f * currentScale,
                            center = offset,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )

                        // Inner solid point focal dot
                        drawCircle(
                            color = baseColor,
                            radius = 8f * currentScale,
                            center = offset
                        )

                        // 3. Render modern legibility plaque capsule with custom title label next to circle
                        val point = activePoints[idx]
                        val displayLabel = if (point.label.isNotBlank()) "${idx + 1}. ${point.label}" else "標註點 ${idx + 1}"
                        
                        val textWidth = labelPaint.measureText(displayLabel)
                        val textHeight = labelPaint.textSize
                        val paddingX = 14f
                        val paddingY = 8f
                        
                        // Floating capsule background with absolute 100% lighting contrast
                        val capsuleTopLeft = Offset(offset.x + 28f, offset.y - textHeight / 2f - paddingY)
                        
                        drawRoundRect(
                            color = colorSurface.copy(alpha = 0.85f),
                            topLeft = capsuleTopLeft,
                            size = Size(textWidth + paddingX * 2, textHeight + paddingY * 2),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        
                        // Colored sidebar accent color bar on capsule
                        drawRoundRect(
                            color = baseColor,
                            topLeft = capsuleTopLeft,
                            size = Size(5f, textHeight + paddingY * 2),
                            cornerRadius = CornerRadius(2.5f, 2.5f)
                        )

                        // Outline contour border for perfect separation
                        drawRoundRect(
                            color = baseColor.copy(alpha = 0.4f),
                            topLeft = capsuleTopLeft,
                            size = Size(textWidth + paddingX * 2, textHeight + paddingY * 2),
                            cornerRadius = CornerRadius(10f, 10f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )

                        // Render the crystal clear text above the capsule plaque
                        drawContext.canvas.nativeCanvas.drawText(
                            displayLabel,
                            offset.x + 28f + paddingX + 2f,
                            offset.y + textHeight / 2f - 4f,
                            labelPaint
                        )
                    }
                }

                // 2.2 Beautiful real ARCore point-cloud tracking dots
                if (arCoreActive && arTrackingState == com.google.ar.core.TrackingState.TRACKING) {
                    arPointCloud?.let { points ->
                        val stride = 4 // x, y, z, confidence
                        for (i in 0 until points.size step stride) {
                            val px = points[i]
                            val py = points[i + 1]
                            val pz = points[i + 2]
                            val confidence = points[i + 3]
                            
                            if (confidence > 0.3f) {
                                val screenOffset = com.example.logic.ARProjectionUtils.projectPoint(
                                    px, py, pz,
                                    viewMatrix, projectionMatrix, w, h
                                )
                                
                                if (screenOffset != null) {
                                    drawCircle(
                                        color = colorPrimary.copy(alpha = 0.4f * confidence),
                                        radius = 2f,
                                        center = screenOffset
                                    )
                                }
                            }
                        }
                    }
                } else if (arCoreActive) {
                    // Fallback simulation if tracking not ready/available
                    val arPointsSimulation = listOf(
                        Pair(7f, -12f),
                        Pair(-14f, 6f),
                        Pair(18f, 14f),
                        Pair(-9f, -18f),
                        Pair(22f, -8f),
                        Pair(-20f, -5f),
                        Pair(10f, 25f),
                        Pair(-25f, 15f)
                    )
                    arPointsSimulation.forEach { (offsetYaw, offsetPitch) ->
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
                            
                            // Draw tiny AR tracking point + crosshair
                            drawCircle(
                                color = colorPrimary.copy(alpha = 0.6f),
                                radius = 2.5f,
                                center = Offset(px, py)
                            )
                            // Faint tick line indicators around the feature point
                            drawLine(
                                color = colorPrimary.copy(alpha = 0.4f),
                                start = Offset(px - 5f, py),
                                end = Offset(px + 5f, py),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = colorPrimary.copy(alpha = 0.4f),
                                start = Offset(px, py - 5f),
                                end = Offset(px, py + 5f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }

                // 2.3 AR Planes visualization
                if (arCoreActive && arTrackingState == com.google.ar.core.TrackingState.TRACKING) {
                    arPlanes.forEach { plane ->
                        val boundary = plane.polygon
                        for (i in 0 until boundary.limit() step 2) {
                            val localX = boundary.get(i)
                            val localZ = boundary.get(i + 1)
                            
                            // Convert local plane coords to world coords
                            val pose = plane.centerPose
                            val worldPos = pose.transformPoint(floatArrayOf(localX, 0f, localZ))
                            
                            val screenOffset = com.example.logic.ARProjectionUtils.projectPoint(
                                worldPos[0], worldPos[1], worldPos[2],
                                viewMatrix, projectionMatrix, w, h
                            )
                            
                            if (screenOffset != null) {
                                drawCircle(
                                    color = colorTertiary.copy(alpha = 0.3f),
                                    radius = 3f,
                                    center = screenOffset
                                )
                                
                                // Connect to next point in polygon
                                val nextIdx = if (i + 2 < boundary.limit()) i + 2 else 0
                                val nextWorldPos = pose.transformPoint(floatArrayOf(boundary.get(nextIdx), 0f, boundary.get(nextIdx + 1)))
                                val nextScreenOffset = com.example.logic.ARProjectionUtils.projectPoint(
                                    nextWorldPos[0], nextWorldPos[1], nextWorldPos[2],
                                    viewMatrix, projectionMatrix, w, h
                                )
                                
                                if (nextScreenOffset != null) {
                                    drawLine(
                                        color = colorTertiary.copy(alpha = 0.2f),
                                        start = screenOffset,
                                        end = nextScreenOffset,
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AR Calibration Overlay
            AnimatedVisibility(
                visible = arCoreActive && arTrackingState != com.google.ar.core.TrackingState.TRACKING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ARCalibrationOverlay(accentColor = colorPrimary, viewModel = viewModel)
            }

    // 3. Central Target Crosshair Viewport Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val isLeveled = abs(roll) < 1.0f
                val animatedRoll by animateFloatAsState(
                    targetValue = roll,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "rollAnim"
                )

                Canvas(modifier = Modifier.size(100.dp)) {
                    val w = size.width
                    val h = size.height
                    val currentAccent = if (isLeveled) colorPrimary else colorTertiary
                    val accentAlpha = if (isLeveled) 1.0f else 0.7f

                    // Outer targeting ring
                    drawCircle(
                        color = currentAccent.copy(alpha = accentAlpha),
                        radius = 28f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Central precise crosshair reticle dot
                    drawCircle(
                        color = currentAccent.copy(alpha = accentAlpha),
                        radius = 4f,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // Mechanical level balance lines with rotation animation
                    val angleOffset = Math.toRadians(animatedRoll.toDouble())
                    val len = 16f
                    val dx = cos(angleOffset).toFloat() * len
                    val dy = sin(angleOffset).toFloat() * len

                    // Left wing balance path
                    drawLine(
                        color = currentAccent.copy(alpha = if (isLeveled) 1f else 0.5f),
                        start = Offset(w/2f - 45f - dx, h/2f - dy),
                        end = Offset(w/2f - 24f - dx, h/2f - dy),
                        strokeWidth = if (isLeveled) 5f else 3f,
                        cap = StrokeCap.Round
                    )
                    // Right wing balance path
                    drawLine(
                        color = currentAccent.copy(alpha = if (isLeveled) 1f else 0.5f),
                        start = Offset(w/2f + 24f + dx, h/2f + dy),
                        end = Offset(w/2f + 45f + dx, h/2f + dy),
                        strokeWidth = if (isLeveled) 5f else 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Live Floating Header HUD Info Panels
            val hudOffset by animateDpAsState(
                targetValue = if (cameraPermissionState.status.isGranted) 0.dp else (-100).dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "hudEntry"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = hudOffset)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // AI Auto Mode Badge
                    Surface(
                        color = colorSurface.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoMode,
                                contentDescription = "自動測量模式",
                                tint = colorTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = viewModel.getString("mode_simulation"),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorOnSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                        // Large Live Value
                        Column(horizontalAlignment = Alignment.End) {
                            // Tracking Accuracy / Status HUD
                            val isAutoCalibrating = arTrackingState == com.google.ar.core.TrackingState.TRACKING
                            
                            val trackingLevelTitle = when (arTrackingState) {
                                com.google.ar.core.TrackingState.TRACKING -> viewModel.getString("diagnostics_status_tracking")
                                com.google.ar.core.TrackingState.PAUSED -> viewModel.getString("mode_paused")
                                else -> viewModel.getString("diagnostics_status_searching")
                            }
                            val trackingColor = when (arTrackingState) {
                                com.google.ar.core.TrackingState.TRACKING -> colorPrimary
                                else -> colorTertiary
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(trackingColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = trackingLevelTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = trackingColor
                                )
                            }
                            
                            if (isAutoCalibrating) {
                                Text(
                                    text = "${viewModel.getString("diagnostics_auto_height")}: ${cameraHeightCm.toInt()}cm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorOnSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 9.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                        if (activePoints.size >= 2) {
                            Text(
                                text = "${viewModel.getString("ar_perimeter")}: ${viewModel.formatLengthValue(viewModel.totalPathDistance)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (activePoints.size >= 3 && !viewModel.isAutoMeasuringHeight(pitch)) {
                            Text(
                                text = "${viewModel.getString("ar_area")}: ${viewModel.formatAreaValue(viewModel.totalEnclosedArea)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        val isHeight = viewModel.isAutoMeasuringHeight(pitch)
                        Text(
                            text = if (activePoints.isEmpty()) {
                                viewModel.getString("diagnostics_status_searching")
                            } else if (isHeight) {
                                if (currentLang.startsWith("zh")) "正在測量高度..." else "Measuring Height..."
                            } else {
                                if (currentLang.startsWith("zh")) "正在測量地面距離..." else "Measuring Ground..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colorTertiary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.getLiveDistanceText(),
                            style = MaterialTheme.typography.displaySmall,
                            color = colorPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (selectedUnit == "m") viewModel.getString("unit_meter") else viewModel.getString("unit_cm"),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorOnSurfaceVariant
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
                                color = when {
                                    arCoreState != "SUPPORTED_INSTALLED" -> colorPrimary.copy(alpha = 0.7f)
                                    arTrackingState == com.google.ar.core.TrackingState.TRACKING -> colorPrimary
                                    else -> colorPrimary.copy(alpha = 0.5f)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            arCoreState != "SUPPORTED_INSTALLED" -> viewModel.getString("mode_simulation")
                            arTrackingState == com.google.ar.core.TrackingState.TRACKING -> viewModel.getString("mode_tracking")
                            arTrackingState == com.google.ar.core.TrackingState.PAUSED -> viewModel.getString("mode_paused")
                            else -> viewModel.getString("mode_initializing")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colorOnSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            // 5. Floating Bottom Controller Hud Panel
            val bottomPanelOffset by animateDpAsState(
                targetValue = if (cameraPermissionState.status.isGranted) 0.dp else 150.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "bottomEntry"
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = bottomPanelOffset)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Horizontally scrollable list of active nodes for label editing
                if (activePoints.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(activePoints) { idx, pt ->
                            val isLast = idx == activePoints.lastIndex
                            val badgeColor = if (isLast) colorPrimary else colorTertiary
                            
                            SuggestionChip(
                                onClick = {
                                    editingPointIndex = idx
                                    pointLabelInputState = pt.label
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(badgeColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (idx + 1).toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (pt.label.isNotBlank()) pt.label else viewModel.getString("add_label"),
                                            color = colorOnSurface,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = viewModel.getString("edit_label"),
                                            tint = colorOnSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = colorSurface.copy(alpha = 0.8f),
                                    labelColor = colorOnSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = badgeColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                val hasPoints = activePoints.isNotEmpty()
                val addButtonScale by animateFloatAsState(
                    targetValue = if (hasPoints) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "addButtonScale"
                )
                val rightButtonScale by animateFloatAsState(
                    targetValue = if (hasPoints) 1.10f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "rightButtonScale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left column: Take Photo Button
                    IconButton(
                        onClick = {
                            val activity = context as? Activity
                            val window = activity?.window
                            if (window != null) {
                                ShareUtility.captureScreen(window, view) { uri ->
                                    if (uri != null) {
                                        Toast.makeText(context, viewModel.getString("photo_saved"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, viewModel.getString("photo_save_failed"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(colorSurface.copy(alpha = 0.6f), CircleShape)
                            .testTag("ar_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = viewModel.getString("take_photo"),
                            tint = colorPrimary
                        )
                    }

                    // Main Action (+)
                    LargeFloatingActionButton(
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.addPoint() 
                        },
                        containerColor = colorPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .graphicsLayer(scaleX = addButtonScale, scaleY = addButtonScale)
                            .size(84.dp)
                            .testTag("add_point_fab")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, viewModel.getString("add_anchor"), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Right column: Undo / Clear / Save Action
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activePoints.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.removeLastPoint() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(colorSurface.copy(alpha = 0.6f), CircleShape)
                                    .testTag("undo_button")
                            ) {
                                Icon(Icons.Default.Refresh, viewModel.getString("undo"), tint = colorOnSurface)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        FloatingActionButton(
                            onClick = {
                                if (activePoints.isNotEmpty()) {
                                    saveTitleTextState = ""
                                    saveNotesTextState = ""
                                    showSaveMeasurementDialog = true
                                } else {
                                    onShowHistoryClick()
                                }
                            },
                            containerColor = if (activePoints.isNotEmpty()) colorTertiary else colorSurface.copy(alpha = 0.6f),
                            contentColor = if (activePoints.isNotEmpty()) MaterialTheme.colorScheme.onTertiary else colorOnSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .graphicsLayer(scaleX = rightButtonScale, scaleY = rightButtonScale)
                                .size(54.dp)
                                .testTag("save_or_history_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (activePoints.isNotEmpty()) Icons.Default.Save else Icons.Default.History,
                                    contentDescription = if (activePoints.isNotEmpty()) viewModel.getString("save_measurement") else viewModel.getString("view_history"),
                                    tint = if (activePoints.isNotEmpty()) MaterialTheme.colorScheme.onTertiary else colorOnSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Point Label Dialog Modal
    editingPointIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { editingPointIndex = null },
            containerColor = colorSurfaceContainer,
            title = { Text("${viewModel.getString("edit_label_title")} #${index + 1}", color = colorOnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        viewModel.getString("edit_label_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pointLabelInputState,
                        onValueChange = { pointLabelInputState = it },
                        label = { Text(viewModel.getString("label_placeholder")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPrimary,
                            unfocusedBorderColor = colorOutline,
                            focusedLabelColor = colorPrimary,
                            unfocusedLabelColor = colorOnSurfaceVariant,
                            focusedTextColor = colorOnSurface,
                            unfocusedTextColor = colorOnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePointLabel(index, pointLabelInputState)
                        editingPointIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(viewModel.getString("save_label"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPointIndex = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorOnSurface)
                ) {
                    Text(viewModel.getString("cancel"))
                }
            }
        )
    }

    // Save Complete Measurement Modal
    if (showSaveMeasurementDialog) {
        AlertDialog(
            onDismissRequest = { showSaveMeasurementDialog = false },
            containerColor = colorSurfaceContainer,
            title = { Text(viewModel.getString("save_record_title"), color = colorOnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        viewModel.getString("save_record_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = saveTitleTextState,
                        onValueChange = { saveTitleTextState = it },
                        label = { Text(viewModel.getString("record_name_placeholder")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPrimary,
                            unfocusedBorderColor = colorOutline,
                            focusedLabelColor = colorPrimary,
                            unfocusedLabelColor = colorOnSurfaceVariant,
                            focusedTextColor = colorOnSurface,
                            unfocusedTextColor = colorOnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveNotesTextState,
                        onValueChange = { saveNotesTextState = it },
                        label = { Text(viewModel.getString("record_notes_placeholder")) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPrimary,
                            unfocusedBorderColor = colorOutline,
                            focusedLabelColor = colorPrimary,
                            unfocusedLabelColor = colorOnSurfaceVariant,
                            focusedTextColor = colorOnSurface,
                            unfocusedTextColor = colorOnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = if (saveTitleTextState.isBlank()) {
                            if (activePoints.size >= 2) viewModel.getString("default_record_name_long") else viewModel.getString("default_record_name_dist")
                        } else saveTitleTextState
                        viewModel.saveCurrentMeasurement(finalTitle, saveNotesTextState)
                        showSaveMeasurementDialog = false
                        viewModel.clearActivePoints()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorTertiary, contentColor = MaterialTheme.colorScheme.onTertiary)
                ) {
                    Text(viewModel.getString("confirm_save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveMeasurementDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorOnSurface)
                ) {
                    Text(viewModel.getString("cancel"))
                }
            }
        )
    }
}

@Composable
fun ARCalibrationOverlay(accentColor: Color, viewModel: com.example.ui.viewmodel.MeasureViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "CalibAnim")
    
    // Animation for the phone icon moving side to side
    val phoneTranslation by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phoneX"
    )

    // Animation for the scanning arc alpha
    val arcAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arcAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                
                // 1. Draw scanning fan/arc to represent FOV
                drawArc(
                    color = accentColor.copy(alpha = arcAlpha * 0.3f),
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = true,
                    topLeft = Offset(cx - 80f, cy - 100f),
                    size = Size(160f, 160f)
                )

                // 2. Draw animated "Phone" icon
                withTransform({
                    translate(phoneTranslation * 1.5f, 0f)
                    rotate(phoneTranslation / 4f, Offset(cx, cy))
                }) {
                    // Phone body
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - 30f, cy - 55f),
                        size = Size(60f, 110f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Screen area
                    drawRect(
                        color = accentColor.copy(alpha = 0.1f),
                        topLeft = Offset(cx - 24f, cy - 45f),
                        size = Size(48f, 75f)
                    )
                    // Camera lens
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = 4f,
                        center = Offset(cx, cy - 50f)
                    )
                    // Home indicator
                    drawLine(
                        color = Color.Black.copy(alpha = 0.3f),
                        start = Offset(cx - 10f, cy + 45f),
                        end = Offset(cx + 10f, cy + 45f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // 3. Draw footprints/target area on "ground"
                drawOval(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(cx - 60f, cy + 70f),
                    size = Size(120f, 30f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.large,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.getString("initializing_ar"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("scan_environment"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
