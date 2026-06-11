package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.AudioVerdict
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScanUiState

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onNavigateToSpectrogram: () -> Unit,
    onNavigateToAdvanced: () -> Unit
) {
    val context = LocalContext.current
    val scanUiState by viewModel.scanUiState.collectAsState()
    val batchList by viewModel.batchList.collectAsState()
    val isBatchScanning by viewModel.isBatchScanning.collectAsState()

    // 1 file selector launcher
    val singleAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeUri(context, it) }
    }

    // Multiple files selector launcher
    val multipleAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.scanMultipleUris(context, uris)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Picker actions row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "File Audio Digital Forensic",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports true local decoding for FLAC, WAV, AAC, ALAC, MP3",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { singleAudioLauncher.launch("audio/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_single_audio_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select File", fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { multipleAudioLauncher.launch("audio/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_multiple_audio_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Batch Files", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Active State Handler
        item {
            AnimatedContent(
                targetState = scanUiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "scannerStateTransition"
            ) { state ->
                when (state) {
                    is ScanUiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ready to analyze. Select a file above.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is ScanUiState.Decoding -> {
                        ScanningStateView(label = "Extracting PCM Audio Stream...", hasSpinner = true)
                    }
                    is ScanUiState.Analyzing -> {
                        ScanningStateView(label = "Performing local FFT & Spectral Analysis...", hasSpinner = true)
                    }
                    is ScanUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = state.message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                            }
                        }
                    }
                    is ScanUiState.Success -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            VerdictDisplayModule(
                                metadata = state.metadata,
                                result = state.result,
                                onNavigateToSpectrogram = onNavigateToSpectrogram,
                                onNavigateToAdvanced = onNavigateToAdvanced
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Batch Scanning Progress & Outputs
        if (isBatchScanning || batchList.isNotEmpty()) {
            item {
                Text(
                    text = "Batch Scanning Session",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isBatchScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing batch audio files...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(batchList) { item ->
                BatchScanResultRow(item = item)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ScanningStateView(label: String, hasSpinner: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasSpinner) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VerdictDisplayModule(
    metadata: com.example.dsp.AudioMetadata,
    result: com.example.dsp.DetectionResult,
    onNavigateToSpectrogram: () -> Unit,
    onNavigateToAdvanced: () -> Unit
) {
    val hexColor = result.verdict.levelColorHex
    val badgeColor = Color(android.graphics.Color.parseColor("#$hexColor"))

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Verdict Title Card (Modern glowing border aesthetic)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FORENSIC DIAGNOSIS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = result.verdict.label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Accuracy confidence: ${result.confidenceScore}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { result.confidenceScore / 100f },
                    color = badgeColor,
                    trackColor = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quality report Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Overall Audio Quality Score:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${result.qualityScore} / 100", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Reasons checklist card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Evidence & Fingerprints found:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                for (r in result.reasons) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = r, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f))
                    }
                }
            }
        }

        // Metadata breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Technical Specifications", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                MetadataRow(label = "File Name", value = metadata.fileName)
                MetadataRow(label = "Format / Codec", value = metadata.format)
                MetadataRow(label = "Sample Rate", value = "${metadata.sampleRate / 1000.0} kHz")
                MetadataRow(label = "Bit Depth", value = "${metadata.bitDepth} bit")
                MetadataRow(label = "Bitrate", value = "${String.format("%.1f", metadata.bitrateKbps)} Kbps")
                MetadataRow(label = "Channels", value = if (metadata.channels == 1) "Mono" else "Stereo")
                MetadataRow(
                    label = "File Size",
                    value = String.format("%.2f MB", metadata.fileSize.toDouble() / (1024 * 1024))
                )
            }
        }

        // Fast Screen Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToSpectrogram,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.BarChart, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Spectrogram", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToAdvanced,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(imageVector = Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Advanced", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun BatchScanResultRow(item: com.example.data.db.ScanHistoryEntity) {
    val badgeColor = when {
        item.verdict.contains("REAL") -> Color(0xFF00FF87)
        item.verdict.contains("FAKE") -> Color(0xFFFF5252)
        item.verdict.contains("UPSAMPLED") -> Color(0xFF40C4FF)
        else -> Color(0xFFFFD740)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${item.format} • ${item.sampleRate}Hz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = item.verdict.replace("_", " "), fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
