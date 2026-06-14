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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
            .clickable(enabled = false) {} // block click propagation
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main glassmorphic card container sliding from bottom
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header (Progress Dots & Skip)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Page indicator dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until totalPages) {
                                val isActive = i == currentPage
                                val width by animateDpAsState(
                                    targetValue = if (isActive) 24.dp else 8.dp,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(height = 8.dp, width = width)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                )
                            }
                        }
                        
                        // Skip button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = viewModel.getString("skip"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Page Animated Illustration Container - Larger and more prominent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    )
                                )
                            ),
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

                    // Title & Instructions Content
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "pagerTransition"
                    ) { page ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                1 -> viewModel.getString("perm_camera_desc")
                                2 -> viewModel.getString("onboarding_desc_3")
                                3 -> viewModel.getString("onboarding_desc_4")
                                else -> viewModel.getString("onboarding_desc_5")
                            }

                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer Buttons (Prev / Next)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentPage > 0) {
                            FilledTonalIconButton(
                                onClick = { currentPage-- },
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = viewModel.getString("prev_step"))
                            }
                        }

                        Button(
                            onClick = { 
                                if (currentPage < totalPages - 1) currentPage++ else onDismiss() 
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = if (currentPage == totalPages - 1) 
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text(
                                text = if (currentPage == totalPages - 1) viewModel.getString("get_started") else viewModel.getString("next_step"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentPage < totalPages - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
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
