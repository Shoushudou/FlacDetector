package com.example.utils

import android.content.Context
import android.os.Environment
import com.example.data.db.ScanHistoryEntity
import java.io.File
import java.io.FileOutputStream

object Exporter {

    /**
     * Exports the standard scan history list to a clean, readable CSV file format.
     * Returns the generated file resource ready for sharing or local storage.
     */
    fun exportToCsv(context: Context, historyList: List<ScanHistoryEntity>): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
        val file = File(dir, "FLAC_Detective_Scan_Report_${System.currentTimeMillis()}.csv")

        FileOutputStream(file).use { output ->
            val header = "File Name,Format,Bitrate (Kbps),Sample Rate (Hz),Bit Depth,Verdict,Confidence (%),Quality Score (0-100),Cutoff Freq (Hz),Timestamp\n"
            output.write(header.toByteArray())

            for (item in historyList) {
                val row = "${escapeCsv(item.fileName)}," +
                        "${item.format}," +
                        "${String.format("%.1f", item.bitrateKbps)}," +
                        "${item.sampleRate}," +
                        "${item.bitDepth}," +
                        "${item.verdict}," +
                        "${item.confidence}," +
                        "${item.qualityScore}," +
                        "${String.format("%.1f", item.cutoffFrequencyHz)}," +
                        "${item.timestamp}\n"
                output.write(row.toByteArray())
            }
        }
        return file
    }

    /**
     * Escapes standard CSV sequence values safely.
     */
    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
