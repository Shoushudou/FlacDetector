package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScanHistoryEntity
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CompareScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val compareState by viewModel.compareState.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    var showPickerSlot by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FORENSIC COMPARISON DECK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Analyze waveform matching and spectral differences side by side",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Slot selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Slot A
            Box(modifier = Modifier.weight(1f)) {
                CompareSlotContainer(
                    slotLabel = "Slot A",
                    entityName = compareState.fileAMetadata?.fileName,
                    format = compareState.fileAMetadata?.format,
                    verdictLabel = compareState.fileAResult?.verdict?.label,
                    onClick = { showPickerSlot = "A" }
                )
            }

            // Slot B
            Box(modifier = Modifier.weight(1f)) {
                CompareSlotContainer(
                    slotLabel = "Slot B",
                    entityName = compareState.fileBMetadata?.fileName,
                    format = compareState.fileBMetadata?.format,
                    verdictLabel = compareState.fileBResult?.verdict?.label,
                    onClick = { showPickerSlot = "B" }
                )
            }
        }

        if (compareState.fileAMetadata != null && compareState.fileBMetadata != null) {
            // Differential Metrics
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DIFFERENTIAL METRICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DiffRow(
                        label = "Effective Cutoff Diff",
                        valA = "${String.format("%.1f", (compareState.fileAResult?.cutoffFrequencyHz ?: 0.0) / 1000.0)} kHz",
                        valB = "${String.format("%.1f", (compareState.fileBResult?.cutoffFrequencyHz ?: 0.0) / 1000.0)} kHz"
                    )

                    DiffRow(
                        label = "Lossless Congruency Score",
                        valA = "${compareState.fileAResult?.losslessProbability ?: 0}%",
                        valB = "${compareState.fileBResult?.losslessProbability ?: 0}%"
                    )

                    DiffRow(
                        label = "Spectral Distortion Index",
                        valA = "${compareState.fileAResult?.spectralIntegrity ?: 0}/100",
                        valB = "${compareState.fileBResult?.spectralIntegrity ?: 0}/100"
                    )
                }
            }

            // Differential Heatmap Canvas Visualization
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPECTRAL DELTA DIFFERENCE HEATMAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Brighter highlights identify severe compression gaps, upsampling interpolation noise, and transcoded spectral missing energy.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw procedural delta comparison waveform or heat line
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val samplesA = compareState.fileASamples ?: FloatArray(0)
                        val samplesB = compareState.fileBSamples ?: FloatArray(0)
                        val maxLen = Math.min(samplesA.size, samplesB.size).coerceAtMost(200)

                        if (maxLen > 1) {
                            val step = canvasWidth / maxLen
                            for (i in 0 until maxLen - 1) {
                                val x1 = i * step
                                val x2 = (i + 1) * step

                                // Compute delta difference factor
                                val diff = Math.abs(samplesA[i] - samplesB[i])
                                val heightFactor = diff * canvasHeight * 1.5f
                                val y1 = canvasHeight / 2f - heightFactor
                                val y2 = canvasHeight / 2f + heightFactor

                                // Palette: glow orange/pink depending on intensity of mismatch
                                val color = if (diff > 0.4f) {
                                    Color(0xFFFF0D72) // high mismatch
                                } else if (diff > 0.1f) {
                                    Color(0xFFFFAB40) // medium
                                } else {
                                    Color(0xFF00FF87) // closely matched lossless signals
                                }

                                drawLine(
                                    color = color,
                                    start = Offset(x1, y1),
                                    end = Offset(x1, y2),
                                    strokeWidth = 2f
                                )
                            }
                        } else {
                            // draw standard grid line
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(0f, canvasHeight / 2),
                                end = Offset(canvasWidth, canvasHeight / 2),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp)),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Please load both Slot A and Slot B audio records to unlock the cross-matching heatmap delta analysis.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Modal popup to select items from local SQLite history lists
    if (showPickerSlot != null) {
        AlertDialog(
            onDismissRequest = { showPickerSlot = null },
            title = { Text(text = "Select Track for ${showPickerSlot}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (historyList.isEmpty()) {
                        Text("No scan history found. Please run a scan first in the Scanner view.")
                    } else {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(historyList) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            viewModel.selectForCompare(showPickerSlot!!, item)
                                            showPickerSlot = null
                                        }
                                        .padding(12.dp)
                                        .border(1.dp, Color.Transparent)
                                ) {
                                    Column {
                                        Text(
                                            text = item.fileName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.format} • ${item.sampleRate}Hz • verdict: ${item.verdict}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerSlot = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CompareSlotContainer(
    slotLabel: String,
    entityName: String?,
    format: String?,
    verdictLabel: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slotLabel,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            if (entityName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = entityName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Format: $format",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = verdictLabel ?: "",
                    fontSize = 10.sp,
                    color = Color(0xFFB4E28D),
                    fontWeight = FontWeight.Black
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Load file",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Cari Audio file",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
fun DiffRow(label: String, valA: String, valB: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            modifier = Modifier.weight(1.2f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Text(
            text = valA,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            modifier = Modifier.size(16.dp).weight(0.4f),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )

        Text(
            text = valB,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
