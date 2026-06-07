package com.example.logic

import com.example.ui.viewmodel.Point3D

/**
 * AR 測量引擎介面
 * 定義了測量儀所需的核心運算功能，方便未來切換不同的追蹤技術（感測器 vs ARCore）
 */
interface ARMeasureEngine {
    /**
     * 根據當前相機狀態（俯仰角、高度）計算地面投影點
     */
    fun calculateGroundPoint(
        pitch: Float, 
        yaw: Float, 
        cameraHeightMeters: Double
    ): Point3D

    /**
     * 計算兩個 3D 點位之間的歐幾里得距離（米）
     */
    fun calculateDistance(p1: Point3D, p2: Point3D): Double

    /**
     * 計算物體垂直高度
     * @param baseDistance 鏡頭到物體底部的水平距離
     * @param targetPitch 瞄準物體頂部時的俯仰角
     * @param cameraHeightMeters 持機高度
     */
    fun calculateVerticalHeight(
        baseDistance: Double,
        targetPitch: Float,
        cameraHeightMeters: Double
    ): Double
}

/**
 * 基於三角函數的測量實作
 * 適用於沒有深度相機的裝置，利用重力感應器 (Pitch) 與已知高度進行推算
 */
class TrigonometricMeasureEngine : ARMeasureEngine {
    
    override fun calculateGroundPoint(
        pitch: Float,
        yaw: Float,
        cameraHeightMeters: Double
    ): Point3D {
        // 弧度轉換，並確保角度不為 0 避免除零錯誤
        val pitchRad = Math.toRadians(Math.abs(pitch).toDouble().coerceAtLeast(1.0))
        
        // 三角函數：d = h / tan(theta)
        val d = cameraHeightMeters / Math.tan(pitchRad)
        val yawRad = Math.toRadians(yaw.toDouble())
        
        return Point3D(
            x = d * Math.sin(yawRad),
            y = d * Math.cos(yawRad),
            z = 0.0,
            pitch = pitch,
            yaw = yaw
        )
    }

    override fun calculateDistance(p1: Point3D, p2: Point3D): Double {
        return Math.sqrt(
            Math.pow(p1.x - p2.x, 2.0) +
            Math.pow(p1.y - p2.y, 2.0) +
            Math.pow(p1.z - p2.z, 2.0)
        )
    }

    override fun calculateVerticalHeight(
        baseDistance: Double,
        targetPitch: Float,
        cameraHeightMeters: Double
    ): Double {
        val pitchRad = Math.toRadians(targetPitch.toDouble())
        // Z = H + d * tan(pitch)
        val zHeight = cameraHeightMeters + baseDistance * Math.tan(pitchRad)
        return zHeight.coerceAtLeast(0.0)
    }
}
