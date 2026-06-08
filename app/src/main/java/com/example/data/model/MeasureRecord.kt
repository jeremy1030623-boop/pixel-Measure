package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measure_records")
data class MeasureRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val value: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "CAM" (Camera Mode) or "RULER" (Screen Ruler Mode)
    val notes: String? = null,
    val pointsData: String? = null // Holds serialized Point3D elements
)
