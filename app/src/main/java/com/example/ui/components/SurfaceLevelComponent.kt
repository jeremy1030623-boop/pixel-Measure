package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun SurfaceLevelComponent(
    pitch: Float,  // 前後傾斜 (前後)
    roll: Float,   // 左右偏擺 (旋轉)
    vibrateOnAlignment: Boolean = true,
    onCalibrate: () -> Unit = {},
    onReset: () -> Unit = {},
    viewModel: com.example.ui.viewmodel.MeasureViewModel
) {
    // 判定水平標準
    val isPerfectLevel = abs(roll) < 0.2f && abs(pitch) < 0.2f
    val isNearLevel = abs(roll) < 1.0f && abs(pitch) < 1.0f
    
    val displayColor = if (isPerfectLevel) MaterialTheme.colorScheme.primary 
                      else if (isNearLevel) MaterialTheme.colorScheme.secondary 
                      else MaterialTheme.colorScheme.tertiary
                      
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // 當達到完美水平時觸發觸覺反饋
    LaunchedEffect(isPerfectLevel) {
        if (isPerfectLevel && vibrateOnAlignment) {
            try {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                } else {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            } catch (e: Exception) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // 背景裝飾格點與圓環
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = min(size.width, size.height) * 0.35f
            
            // 十字準星
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 2f
            )
            
            // 參考圓環
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 40.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = baseRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
        }

        // 主數值顯示
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = roll,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "rollText"
            ) { targetRoll ->
                Text(
                    text = String.format("%.1f°", targetRoll),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    ),
                    color = displayColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = if (isPerfectLevel) viewModel.getString("mode_tracking") else viewModel.getString("onboarding_title_3"),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPerfectLevel) displayColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 極簡水平線元件
        LaserLevelLine(roll = roll, pitch = pitch, isLevel = isPerfectLevel, color = displayColor)

        // 2D 氣泡水平儀感應器
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            val animatedPitch by animateFloatAsState(
                targetValue = pitch.coerceIn(-15f, 15f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "pitchBubble"
            )
            val animatedRollBubble by animateFloatAsState(
                targetValue = roll.coerceIn(-15f, 15f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "rollBubble"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxOffset = 80.dp.toPx()
                
                // 氣泡位置計算 (將角度映射到偏移量)
                val offsetX = (animatedRollBubble / 15f) * maxOffset
                val offsetY = (animatedPitch / 15f) * maxOffset
                
                // 繪製移動氣泡
                drawCircle(
                    color = displayColor.copy(alpha = if (isPerfectLevel) 0.8f else 0.4f),
                    radius = 16.dp.toPx(),
                    center = Offset(cx + offsetX, cy + offsetY)
                )
                
                // 完美水平時的中心閃爍環
                if (isPerfectLevel) {
                    drawCircle(
                        color = displayColor.copy(alpha = 0.3f),
                        radius = 20.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // 底部校準控制
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = viewModel.getString("reset_deviation"), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(viewModel.getString("undo"))
            }
            Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary),
                elevation = null
            ) {
                Icon(Icons.Default.Balance, contentDescription = viewModel.getString("calibration_zero"), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(viewModel.getString("calibration_zero"), fontWeight = FontWeight.Bold)
            }
        }
        
        AnimatedVisibility(
            visible = isPerfectLevel,
            enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(viewModel.getString("mode_tracking"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LaserLevelLine(roll: Float, pitch: Float, isLevel: Boolean, color: Color) {
    val animatedRoll by animateFloatAsState(
        targetValue = if (isLevel) 0f else roll,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "roll"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        withTransform({
            rotate(degrees = -animatedRoll, pivot = Offset(cx, cy))
        }) {
            // 繪製雷射平準線
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color, color, Color.Transparent),
                    startX = 0f,
                    endX = size.width
                ),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = if (isLevel) 8f else 4f,
                cap = StrokeCap.Round
            )

            // 完美的 0.0 度指示點
            if (isLevel) {
                drawCircle(
                    color = color,
                    radius = 12f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = 30f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun AngleDisplay(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%.1f°", value),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            if (abs(value) < 0.5f) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = "完美對齊狀態", tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}
