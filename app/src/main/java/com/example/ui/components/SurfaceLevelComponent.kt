package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
    pitch: Float,  // 前後傾斜
    roll: Float    // 左右偏擺
) {
    // 定義水平判斷標準（0.5度以內視為完美水平）
    val isLevel = abs(pitch) < 0.5f && abs(roll) < 0.5f
    val displayColor = if (isLevel) LevelGreen else MeasureYellow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isLevel) "表面已完全水平" else "平面校正進行中",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = displayColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "數位 3D 空間角度補正引擎",
                    style = MaterialTheme.typography.labelMedium,
                    color = CadetBlue
                )
            }

            // 3D Perspective Plane Visualization
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                LevelerPlate3D(pitch = pitch, roll = roll, isLevel = isLevel)
            }

            // Bottom Readout Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AngleDisplay(label = "縱向俯仰 (Pitch)", value = pitch, color = displayColor)
                    VerticalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = Slate700)
                    AngleDisplay(label = "橫向偏擺 (Roll)", value = roll, color = displayColor)
                }
            }
        }
    }
}

@Composable
fun LevelerPlate3D(pitch: Float, roll: Float, isLevel: Boolean) {
    val displayColor = if (isLevel) LevelGreen else MeasureYellow
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val plateSize = min(size.width, size.height) * 0.8f
        
        // 繪製背景參考框
        drawRect(
            color = Slate800.copy(alpha = 0.5f),
            topLeft = Offset(cx - plateSize/2, cy - plateSize/2),
            size = Size(plateSize, plateSize),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )

        // 應用 3D 變換模擬平面傾斜
        withTransform({
            // 將傳感器數據映射到視覺傾斜 (限制角度以維持透視感)
            val rotateX = pitch.coerceIn(-30f, 30f)
            val rotateY = roll.coerceIn(-30f, 30f)
            
            // Compose 旋轉繞中心進行
            rotate(degrees = rotateY, pivot = Offset(cx, cy))
        }) {
            // 繪製流體平面 (Horizon Plane)
            val offsetLimit = plateSize * 0.4f
            val offsetX = (roll / 15f) * offsetLimit
            val offsetY = (-pitch / 15f) * offsetLimit
            
            val planeRect = Rect(
                left = cx - plateSize/2 + offsetX,
                top = cy - plateSize/2 + offsetY,
                right = cx + plateSize/2 + offsetX,
                bottom = cy + plateSize/2 + offsetY
            )

            // 平面主體
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(displayColor.copy(alpha = 0.3f), displayColor.copy(alpha = 0.1f)),
                    start = Offset(planeRect.left, planeRect.top),
                    end = Offset(planeRect.right, planeRect.bottom)
                ),
                topLeft = Offset(planeRect.left, planeRect.top),
                size = Size(planeRect.width, planeRect.height),
                cornerRadius = CornerRadius(16f)
            )

            // 繪製格點網格 (Grid)
            val gridStep = plateSize / 4
            for (i in 0..4) {
                val pos = i * gridStep
                // 橫線
                drawLine(
                    color = displayColor.copy(alpha = 0.2f),
                    start = Offset(planeRect.left, planeRect.top + pos),
                    end = Offset(planeRect.right, planeRect.top + pos),
                    strokeWidth = 1f
                )
                // 直線
                drawLine(
                    color = displayColor.copy(alpha = 0.2f),
                    start = Offset(planeRect.left + pos, planeRect.top),
                    end = Offset(planeRect.right + pos, planeRect.bottom),
                    strokeWidth = 1f
                )
            }

            // 平面邊框
            drawRoundRect(
                color = displayColor,
                topLeft = Offset(planeRect.left, planeRect.top),
                size = Size(planeRect.width, planeRect.height),
                cornerRadius = CornerRadius(16f),
                style = Stroke(width = 4f)
            )

            // 中心瞄準點 (Target Crosshair)
            if (isLevel) {
                drawCircle(
                    color = LevelGreen,
                    radius = 20f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
            }
        }

        // 靜態中心參考線 (十字準星)
        drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(cx - 40f, cy), end = Offset(cx + 40f, cy), strokeWidth = 2f)
        drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(cx, cy - 40f), end = Offset(cx, cy + 40f), strokeWidth = 2f)
    }
}

@Composable
fun AngleDisplay(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = CadetBlue)
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
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LevelGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}
