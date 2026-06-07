package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun BubbleLevelComponent(
    pitch: Float,  // Up Down (-90 to +90)
    roll: Float    // Left Right (-180 to +180)
) {
    // Normal level: held flat means pitch ≈ 0, roll ≈ 0.
    // If phone is placed flat on a table, gravity points straight down, pitch & roll are near 0.
    // Let's calibrate so perfectly level is pitch = 0, roll = 0.
    val isLevel = abs(pitch) < 0.8f && abs(roll) < 0.8f

    val displayColor = if (isLevel) LevelGreen else MeasureYellow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLevel) Color(0x1F10B981) else Slate800
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLevel) "✦ 已完美水平 ✦" else "請放置在平面上測量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = displayColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("前後傾斜 (Pitch)", style = MaterialTheme.typography.bodySmall, color = CadetBlue)
                            Text(
                                text = String.format("%.1f°", pitch),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (abs(pitch) < 0.8f) LevelGreen else Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("左右偏擺 (Roll)", style = MaterialTheme.typography.bodySmall, color = CadetBlue)
                            Text(
                                text = String.format("%.1f°", roll),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (abs(roll) < 0.8f) LevelGreen else Color.White
                            )
                        }
                    }
                }
            }

            // Visual Level Instrument
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerDiameter = min(size.width, size.height)
                    val instrumentRadius = outerDiameter / 2.3f

                    // Draw outer instrument disc with a solid blueprint circular shade
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Slate800, Slate900),
                            center = Offset(cx, cy),
                            radius = instrumentRadius
                        ),
                        radius = instrumentRadius,
                        center = Offset(cx, cy)
                    )

                    // Draw calibration concentric circle borders (5deg, 10deg, 15deg divisions)
                    val maxAngle = 15f // Anything above 15 degrees lies at the boundary
                    val r5 = (5f / maxAngle) * instrumentRadius
                    val r10 = (10f / maxAngle) * instrumentRadius
                    val r15 = (15f / maxAngle) * instrumentRadius

                    drawCircle(
                        color = Color(0x1F94A3B8),
                        radius = r5,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0x3394A3B8),
                        radius = r10,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0x5294A3B8),
                        radius = r15,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)
                    )

                    // Draw Orthogonal Crosshair scale lines
                    drawLine(
                        color = Color(0x2B94A3B8),
                        start = Offset(cx - instrumentRadius, cy),
                        end = Offset(cx + instrumentRadius, cy),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0x2B94A3B8),
                        start = Offset(cx, cy - instrumentRadius),
                        end = Offset(cx, cy + instrumentRadius),
                        strokeWidth = 2f
                    )

                    // Target level center circle
                    drawCircle(
                        color = displayColor.copy(alpha = if (isLevel) 0.5f else 0.2f),
                        radius = 24f,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Calculate level bubble coordinates
                    // Mapping standard sensor coordinates:
                    // Tilting right (roll > 0) -> bubble goes right
                    // Tilting forward (pitch > 0) -> bubble goes down (or up, let's map pitch directly)
                    val dXRad = roll / maxAngle
                    val dYRad = -pitch / maxAngle // Pitch up tilts bubble downwards

                    // Clamping bubble center to the outer target circle
                    val distRatio = sqrt(dXRad * dXRad + dYRad * dYRad)
                    val clampedRatio = min(distRatio, 1.0f)
                    val scaling = if (distRatio > 0.0001f) clampedRatio / distRatio else 1.0f

                    val bubbleCx = cx + dXRad * scaling * instrumentRadius
                    val bubbleCy = cy + dYRad * scaling * instrumentRadius

                    // Render floating bubble level fluid indicator
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(displayColor.copy(alpha = 0.9f), displayColor.copy(alpha = 0.4f)),
                            center = Offset(bubbleCx, bubbleCy),
                            radius = 32f
                        ),
                        radius = 32f,
                        center = Offset(bubbleCx, bubbleCy)
                    )

                    // Highlight sheen on fluid bubble to look incredibly tactile
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 8f,
                        center = Offset(bubbleCx - 8f, bubbleCy - 8f)
                    )
                }
            }

            // Dual axis Level indicator readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLevel) "PITCH: 0° / ROLL: 0°" else "《 請移動手機使氣泡與中心重合 》",
                    style = MaterialTheme.typography.bodySmall,
                    color = displayColor.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
