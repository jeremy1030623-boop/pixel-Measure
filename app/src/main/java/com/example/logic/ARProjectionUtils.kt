package com.example.logic

import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset

object ARProjectionUtils {
    /**
     * 將 AR 空間中的 3D 點投影到 2D 螢幕座標
     */
    fun projectPoint(
        pointX: Float,
        pointY: Float,
        pointZ: Float,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        width: Float,
        height: Float
    ): Offset? {
        val modelViewProjection = FloatArray(16)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, viewMatrix, 0)

        val vertex = floatArrayOf(pointX, pointY, pointZ, 1f)
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, modelViewProjection, 0, vertex, 0)

        if (result[3] <= 0) return null // 點在相機後方

        val x = result[0] / result[3]
        val y = result[1] / result[3]

        // 歸一化座標 (-1 to 1) 轉換為螢幕座標
        val screenX = (x + 1f) * 0.5f * width
        val screenY = (1f - y) * 0.5f * height

        return Offset(screenX, screenY)
    }
}
