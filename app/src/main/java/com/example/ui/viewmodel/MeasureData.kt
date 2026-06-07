package com.example.ui.viewmodel

/**
 * 代表 AR 空間中的一個 3D 座標點
 */
data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val pitch: Float,
    val yaw: Float
)
