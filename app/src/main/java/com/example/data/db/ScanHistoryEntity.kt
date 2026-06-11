package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val filePath: String,
    val fileName: String,
    val format: String,
    val fileSize: Long,
    val sampleRate: Int,
    val bitrateKbps: Double,
    val durationMs: Long,
    val bitDepth: Int,
    val channels: Int,
    val verdict: String, // String representation of AudioVerdict enum
    val confidence: Int, // 0 to 100
    val qualityScore: Int, // 0 to 100
    val cutoffFrequencyHz: Double,
    val codecProbabilityLossless: Int,
    val codecProbabilityMp3: Int,
    val codecProbabilityAac: Int,
    val timestamp: Long = System.currentTimeMillis()
)
