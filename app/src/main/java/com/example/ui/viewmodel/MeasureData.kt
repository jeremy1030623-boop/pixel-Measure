package com.example.ui.viewmodel

/**
 * 代表 AR 空間中的一個 3D 座標點
 */
data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val pitch: Float,
    val yaw: Float,
    val isArPrecision: Boolean = false,
    val label: String = ""
)

fun Point3D.serialize(): String {
    val lEncoded = label.replace(",", "\\,").replace(";", "\\;")
    return "$x,$y,$z,$pitch,$yaw,$isArPrecision,$lEncoded"
}

fun String.deserializePoint3D(): Point3D? {
    try {
        val parts = this.split(",")
        if (parts.size < 6) return null
        val x = parts[0].toDoubleOrNull() ?: 0.0
        val y = parts[1].toDoubleOrNull() ?: 0.0
        val z = parts[2].toDoubleOrNull() ?: 0.0
        val pitch = parts[3].toFloatOrNull() ?: 0f
        val yaw = parts[4].toFloatOrNull() ?: 0f
        val isAr = parts[5].toBoolean()
        val label = if (parts.size >= 7) parts[6].replace("\\,", ",").replace("\\;", ";") else ""
        return Point3D(x, y, z, pitch, yaw, isAr, label)
    } catch (e: Exception) {
        return null
    }
}

fun List<Point3D>.serializePoints(): String {
    return this.joinToString(";") { it.serialize() }
}

fun String.deserializePoints(): List<Point3D> {
    if (this.isBlank()) return emptyList()
    return this.split(";").mapNotNull { it.deserializePoint3D() }
}
