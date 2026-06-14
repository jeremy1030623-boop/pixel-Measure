package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MeasureViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingTutorialOverlay(
    viewModel: MeasureViewModel,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 5
    
    // Smooth page content fade transition
    val transitionState = remember { MutableTransitionState(0) }
    LaunchedEffect(currentPage) {
        transitionState.targetState = currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {} // block click propagation
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main glassmorphic card container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header (Progress Dots & Skip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalPages) {
                            val isActive = i == currentPage
                            val width by animateDpAsState(
                                targetValue = if (isActive) 16.dp else 6.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                            Box(
                                modifier = Modifier
                                    .size(height = 6.dp, width = width)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }
                    
                    // Skip button
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(viewModel.getString("skip"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Animated Illustration Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentPage) {
                        0 -> DrawWelcomeIllustration()
                        1 -> DrawArCameraIllustration()
                        2 -> DrawRulerIllustration()
                        3 -> DrawLevelIllustration()
                        4 -> DrawSettingsIllustration(viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title & Instructions Content
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                         scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "textTransition"
                ) { page ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val titleText = when (page) {
                            0 -> viewModel.getString("onboarding_title_1")
                            1 -> viewModel.getString("onboarding_title_2")
                            2 -> viewModel.getString("onboarding_title_3")
                            3 -> viewModel.getString("onboarding_title_4")
                            else -> viewModel.getString("onboarding_title_5")
                        }

                        val descText = when (page) {
                            0 -> viewModel.getString("onboarding_desc_1")
                            1 -> viewModel.getString("perm_camera_desc") // Reusing perm desc as it matches context well enough or I can use the long one
                            2 -> viewModel.getString("onboarding_desc_3")
                            3 -> viewModel.getString("onboarding_desc_4")
                            else -> viewModel.getString("onboarding_desc_5")
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Buttons (Prev / Next)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { currentPage-- },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = viewModel.getString("prev_step"), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("prev_step"), style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        // Dummy spacer to keep Next button biased right
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    if (currentPage < totalPages - 1) {
                        Button(
                            onClick = { currentPage++ },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(viewModel.getString("next_step"), style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = viewModel.getString("next_step"), modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = viewModel.getString("get_started"), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("get_started"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawWelcomeIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val baseRadius = 60.dp.toPx()

        // Radar grid circles
        drawCircle(
            color = primaryColor.copy(alpha = 0.08f),
            radius = baseRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = baseRadius * 0.6f,
            center = Offset(cx, cy)
        )

        // Radar pulse ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.4f * (1f - radarPulse)),
            radius = baseRadius * radarPulse,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // Index axes
        drawLine(
            color = primaryColor.copy(alpha = 0.2f),
            start = Offset(cx - baseRadius * 1.2f, cy),
            end = Offset(cx + baseRadius * 1.2f, cy),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.2f),
            start = Offset(cx, cy - baseRadius * 1.2f),
            end = Offset(cx, cy + baseRadius * 1.2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
        )

        // Rotating Sweep line
        val angleRad = Math.toRadians(sweepAngle.toDouble())
        val tx = (cx + baseRadius * cos(angleRad)).toFloat()
        val ty = (cy + baseRadius * sin(angleRad)).toFloat()
        
        drawLine(
            color = primaryColor.copy(alpha = 0.8f),
            start = Offset(cx, cy),
            end = Offset(tx, ty),
            strokeWidth = 2.5f.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center hub glow
        drawCircle(
            color = primaryColor,
            radius = 6.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun DrawArCameraIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val cursorOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        
        // Ground grid background lines
        val gridY = cy + 20.dp.toPx()
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(cx - 100.dp.toPx(), gridY),
            end = Offset(cx + 100.dp.toPx(), gridY),
            strokeWidth = 1.5f.dp.toPx()
        )
        
        // Perspective virtual grid planes
        for (i in -4..4) {
            val offsetStartX = cx + (i * 24.dp.toPx())
            val offsetEndX = cx + (i * 36.dp.toPx())
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(offsetStartX, gridY),
                end = Offset(offsetEndX, gridY + 35.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Measure Points (A & B) on ground plane
        val ptAX = cx - 40.dp.toPx()
        val ptAY = gridY + 12.dp.toPx()
        val ptBX = cx + 40.dp.toPx() + cursorOffset
        val ptBY = gridY + 12.dp.toPx()

        // Line connecting A and B
        drawLine(
            color = primaryColor,
            start = Offset(ptAX, ptAY),
            end = Offset(ptBX, ptBY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Point handles (Pulsing targets)
        drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(ptAX, ptAY))
        drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(ptBX, ptBY))

        // Virtual camera reticle / cursor crosshair tracking point B
        val rSize = 12.dp.toPx()
        drawArc(
            color = primaryColor.copy(alpha = 0.7f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 1.5f.dp.toPx()),
            size = Size(rSize * 2, rSize * 2),
            topLeft = Offset(ptBX - rSize, ptBY - rSize)
        )
        
        // Cross lines
        drawLine(color = primaryColor, start = Offset(ptBX - 16f, ptBY), end = Offset(ptBX - 4f, ptBY), strokeWidth = 1.5f.dp.toPx())
        drawLine(color = primaryColor, start = Offset(ptBX + 4f, ptBY), end = Offset(ptBX + 16f, ptBY), strokeWidth = 1.5f.dp.toPx())
        drawLine(color = primaryColor, start = Offset(ptBX, ptBY - 16f), end = Offset(ptBX, ptBY - 4f), strokeWidth = 1.5f.dp.toPx())
        drawLine(color = primaryColor, start = Offset(ptBX, ptBY + 4f), end = Offset(ptBX, ptBY + 16f), strokeWidth = 1.5f.dp.toPx())
    }
}

@Composable
fun DrawRulerIllustration() {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val targetOffset = with(density) { 28.dp.toPx() }
    val infiniteTransition = rememberInfiniteTransition()
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val cy = size.height / 2
        val rulerY = cy - 10.dp.toPx()

        // Draw ruler plate
        drawRect(
            color = primaryColor.copy(alpha = 0.1f),
            topLeft = Offset(0f, rulerY),
            size = Size(width, 40.dp.toPx())
        )
        // Draw top borderline
        drawLine(
            color = primaryColor.copy(alpha = 0.3f),
            start = Offset(0f, rulerY),
            end = Offset(width, rulerY),
            strokeWidth = 2.dp.toPx()
        )

        // Ruler ticks & markings
        var tickX = -scrollOffset
        var counter = 0
        while (tickX < width + 40.dp.toPx()) {
            val isMajor = counter % 10 == 0
            val isMedium = counter % 5 == 0 && !isMajor
            val tickLen = when {
                isMajor -> 20.dp.toPx()
                isMedium -> 14.dp.toPx()
                else -> 8.dp.toPx()
            }
            drawLine(
                color = primaryColor.copy(alpha = if (isMajor) 0.8f else 0.4f),
                start = Offset(tickX, rulerY),
                end = Offset(tickX, rulerY + tickLen),
                strokeWidth = (if (isMajor) 1.5f else 1f).dp.toPx()
            )
            tickX += 4.dp.toPx()
            counter++
        }

        // Object simulation being measured
        drawRoundRect(
            color = secondaryColor.copy(alpha = 0.55f),
            topLeft = Offset(width / 3f, rulerY + 22.dp.toPx()),
            size = Size(width / 3f, 25.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
    }
}

@Composable
fun DrawLevelIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isBalanced = angle in -1f..1f
    val bubbleColor = if (isBalanced) Color.Green else MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val glassRadius = 50.dp.toPx()

        // Outer circular glass container
        drawCircle(
            color = Color.Gray.copy(alpha = 0.15f),
            radius = glassRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = bubbleColor.copy(alpha = 0.25f),
            radius = glassRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )

        // Center zero-balance circle
        drawCircle(
            color = bubbleColor.copy(alpha = 0.4f),
            radius = 16.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
        )

        // Level bubble moving according to dynamic sensor angle
        val tiltOffsetLen = (angle / 35f) * (glassRadius * 0.65f)
        val bubbleX = cx + tiltOffsetLen
        val bubbleY = cy // horizontal tilt representation

        drawCircle(
            color = bubbleColor.copy(alpha = 0.85f),
            radius = 10.dp.toPx(),
            center = Offset(bubbleX, bubbleY)
        )
        
        // Inner specular reflection bubble spot
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(bubbleX - 3.dp.toPx(), bubbleY - 3.dp.toPx())
        )
    }
}

@Composable
fun DrawSettingsIllustration(viewModel: MeasureViewModel) {
    val selectedUnit by viewModel.selectedUnit.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("cm", "m", "in", "ft").forEach { unit ->
                    val isSelected = selectedUnit == unit
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unit,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Icon(
                Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
