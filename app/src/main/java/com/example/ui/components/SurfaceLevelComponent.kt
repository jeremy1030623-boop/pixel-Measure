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
    onCalibrate: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    // 判定水平標準
    val isPerfectLevel = abs(roll) < 0.2f && abs(pitch) < 0.2f
    val isNearLevel = abs(roll) < 1.0f && abs(pitch) < 1.0f
    
    val displayColor = if (isPerfectLevel) MaterialTheme.colorScheme.primary 
                      else if (isNearLevel) MaterialTheme.colorScheme.secondary 
                      else MaterialTheme.colorScheme.tertiary
                      
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 當達到完美水平時觸發觸覺反饋
    LaunchedEffect(isPerfectLevel) {
        if (isPerfectLevel) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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

        // 2D 氣泡水平儀
        BubbleLevelVisual(pitch = pitch, roll = roll, color = displayColor)

        // 數值顯示
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "表面水平儀",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AngleInfoBox(label = "左右 (Roll)", value = roll, color = displayColor)
                AngleInfoBox(label = "前後 (Pitch)", value = pitch, color = displayColor)
            }
        }

        // 底部校準控制
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置")
            }
            Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary),
                elevation = null
            ) {
                Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("目前位置設為零點", fontWeight = FontWeight.Bold)
            }
        }
        
        if (isPerfectLevel) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("已達到水平點", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BubbleLevelVisual(pitch: Float, roll: Float, color: Color) {
    val maxOffset = 140.dp
    
    // 將角度映射到偏移量 (限制在圓形範圍內)
    val sensitivity = 5f // 敏感度：幾度偏移一個單位
    
    val targetOffsetX = (roll * sensitivity).dp
    val targetOffsetY = (pitch * sensitivity).dp
    
    val animatedOffsetX by animateDpAsState(
        targetValue = targetOffsetX.coerceIn(-maxOffset, maxOffset),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bubbleX"
    )
    
    val animatedOffsetY by animateDpAsState(
        targetValue = targetOffsetY.coerceIn(-maxOffset, maxOffset),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bubbleY"
    )

    Canvas(modifier = Modifier.size(300.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        // 氣泡主體
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, color.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(cx + animatedOffsetX.toPx(), cy + animatedOffsetY.toPx()),
                radius = 28.dp.toPx()
            ),
            radius = 28.dp.toPx(),
            center = Offset(cx + animatedOffsetX.toPx(), cy + animatedOffsetY.toPx())
        )
        
        // 氣泡邊框與高光
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 28.dp.toPx(),
            center = Offset(cx + animatedOffsetX.toPx(), cy + animatedOffsetY.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun AngleInfoBox(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = String.format("%.1f°", value),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            ),
            color = color
        )
    }
}
