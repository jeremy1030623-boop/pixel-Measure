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
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MeasureViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InitialWelcomeScreen(
    viewModel: MeasureViewModel,
    onEnterApp: () -> Unit
) {
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    
    // Scale and alpha animation on entry
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        MaterialTheme.colorScheme.surfaceDim
                    )
                )
            )
            .semantics { contentDescription = "智慧測量應用程式歡迎初始畫面" }
    ) {
        // Enhanced Ambient Glows
        val infiniteTransition = rememberInfiniteTransition(label = "BackgroundGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.4f * glowScale }
        ) {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 120.dp, y = (-120).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(500.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-180).dp, y = 180.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 30.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. App Logo and Header Concept Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1.3f)
                    .wrapContentHeight()
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Beautiful Animated Custom CAD Measurement Logo
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DrawMeasurementLaserLogo()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // App Titles
                Text(
                    text = viewModel.getString("welcome_title_main"),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = viewModel.getString("welcome_title_sub"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Feature highlights / Quick Summary Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FeatureHighlightRow(
                    index = 0,
                    icon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.Camera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = viewModel.getString("welcome_feature_1_title"),
                    description = viewModel.getString("welcome_feature_1_desc")
                )

                FeatureHighlightRow(
                    index = 1,
                    icon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.LinearScale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = viewModel.getString("welcome_feature_2_title"),
                    description = viewModel.getString("welcome_feature_2_desc")
                )

                FeatureHighlightRow(
                    index = 2,
                    icon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.FilterCenterFocus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = viewModel.getString("welcome_feature_3_title"),
                    description = viewModel.getString("welcome_feature_3_desc")
                )
            }

            // 3. CTA Action Launcher Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Feature badge
                AnimatedVisibility(
                    visible = dynamicColorEnabled,
                    enter = fadeIn() + expandVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = viewModel.getString("welcome_dynamic_color"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Main Premium Entry Button
                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Button(
                    onClick = onEnterApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .semantics { 
                            contentDescription = "按鈕：開啟精密智慧測量工具套件，進入主控制面板"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = viewModel.getString("welcome_btn_start"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Developer / Version Signature Footer Info
                Text(
                    text = viewModel.getString("welcome_footer"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FeatureHighlightRow(
    index: Int,
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400L + (index * 150L))
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DrawMeasurementLaserLogo() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Rotating crosshair sweep
    val angleDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Pulsing crosshair laser scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        
        // 1. Draw glowing background guide rings
        drawCircle(
            color = primaryColor.copy(alpha = 0.05f),
            radius = cx * 0.9f,
            center = Offset(cx, cy)
        )
        
        drawCircle(
            color = secondaryColor.copy(alpha = 0.1f),
            radius = cx * 0.7f,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1.5f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            )
        )

        drawCircle(
            color = tertiaryColor.copy(alpha = 0.15f),
            radius = cx * 0.45f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. Draw precision horizontal-vertical alignment cross lines (ruler-like ticks)
        val tickLength = 6.dp.toPx()
        for (i in -4..4) {
            if (i == 0) continue
            val tickOffset = i * 15.dp.toPx()
            
            // X-axis ticks
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(cx + tickOffset, cy - tickLength / 2),
                end = Offset(cx + tickOffset, cy + tickLength / 2),
                strokeWidth = 1.dp.toPx()
            )
            // Y-axis ticks
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(cx - tickLength / 2, cy + tickOffset),
                end = Offset(cx + tickLength / 2, cy + tickOffset),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 3. Draw outer target brackets (CAD Style corner locks)
        val cornerOffset = cx * 0.75f
        val cornerLineLen = 14.dp.toPx()
        
        // Top-left corner
        drawLine(color = primaryColor, start = Offset(cx - cornerOffset, cy - cornerOffset), end = Offset(cx - cornerOffset + cornerLineLen, cy - cornerOffset), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = primaryColor, start = Offset(cx - cornerOffset, cy - cornerOffset), end = Offset(cx - cornerOffset, cy - cornerOffset + cornerLineLen), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        
        // Top-right corner
        drawLine(color = primaryColor, start = Offset(cx + cornerOffset, cy - cornerOffset), end = Offset(cx + cornerOffset - cornerLineLen, cy - cornerOffset), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = primaryColor, start = Offset(cx + cornerOffset, cy - cornerOffset), end = Offset(cx + cornerOffset, cy - cornerOffset + cornerLineLen), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)

        // Bottom-left corner
        drawLine(color = primaryColor, start = Offset(cx - cornerOffset, cy + cornerOffset), end = Offset(cx - cornerOffset + cornerLineLen, cy + cornerOffset), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = primaryColor, start = Offset(cx - cornerOffset, cy + cornerOffset), end = Offset(cx - cornerOffset, cy + cornerOffset - cornerLineLen), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)

        // Bottom-right corner
        drawLine(color = primaryColor, start = Offset(cx + cornerOffset, cy + cornerOffset), end = Offset(cx + cornerOffset - cornerLineLen, cy + cornerOffset), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = primaryColor, start = Offset(cx + cornerOffset, cy + cornerOffset), end = Offset(cx + cornerOffset, cy + cornerOffset - cornerLineLen), strokeWidth = 2.5f.dp.toPx(), cap = StrokeCap.Round)

        // 4. Draw dynamic rotating angle scanner laser line
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        val rx = (cx + cx * 0.7f * cos(angleRad)).toFloat()
        val ry = (cy + cy * 0.7f * sin(angleRad)).toFloat()
        
        drawLine(
            color = tertiaryColor.copy(alpha = 0.75f),
            start = Offset(cx, cy),
            end = Offset(rx, ry),
            strokeWidth = 1.5f.dp.toPx()
        )
        
        // Glowing laser point at end of rotated line
        drawCircle(
            color = tertiaryColor,
            radius = 3.dp.toPx(),
            center = Offset(rx, ry)
        )

        // 5. Draw center target reticle (glowing and pulsing)
        val centerPulseRadius = 11.dp.toPx() * pulseScale
        drawCircle(
            color = secondaryColor.copy(alpha = 0.25f),
            radius = centerPulseRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = secondaryColor,
            radius = 6.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
