package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScanHistoryEntity
import com.example.dsp.AudioVerdict
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onSelectHistoryItem: (ScanHistoryEntity) -> Unit
) {
    val historyList by viewModel.historyList.collectAsState()

    // Calculate quick stats
    val totalCount = historyList.size
    val realLosslessCount = historyList.count { it.verdict == AudioVerdict.REAL_LOSSLESS.name || it.verdict == AudioVerdict.LIKELY_LOSSLESS.name }
    val fakeCount = historyList.count { it.verdict == AudioVerdict.FAKE_LOSSLESS.name || it.verdict == AudioVerdict.LIKELY_FAKE.name }
    val upsampledCount = historyList.count { it.verdict == AudioVerdict.UPSAMPLED.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Header banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF381E72),
                                Color(0xFF2B2930)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "FLAC DETECTIVE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD0BCFF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Spectral Audio Digital Forensic Tool",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB4E28D)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("scan_now_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF381E72))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Scan Audio File", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats Module
        item {
            Text(
                text = "Analyzer Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Scanned",
                    count = totalCount.toString(),
                    icon = Icons.Default.LibraryMusic,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                )
                StatCard(
                    title = "Real Lossless",
                    count = realLosslessCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    tint = Color(0xFF00FF87),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                )
                StatCard(
                    title = "Fake Lossless",
                    count = fakeCount.toString(),
                    icon = Icons.Default.Error,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                )
                StatCard(
                    title = "Upsampled",
                    count = upsampledCount.toString(),
                    icon = Icons.Default.ArrowUpward,
                    tint = Color(0xFF40C4FF),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                )
            }
        }

        // Comparison Banner Quick Link
        item {
            Card(
                onClick = onNavigateToCompare,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
                    .testTag("home_compare_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compare Audio Files",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Analyze spectrogram differences between two tracks side-by-side.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        // Virtual Audito Catalog Setup Quick Link
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Virtual Audiophile Sound Library",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { viewModel.createVirtualSoundLibrary() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-populate")
                }
            }
        }

        // Display 4 major virtual items
        val virtualList = historyList.filter { it.filePath.startsWith("/virtual/") }
        if (virtualList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Click 'Re-populate' to load sample sound files.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(virtualList.take(4)) { item ->
                VirtualCompactCard(item = item, onClick = { onSelectHistoryItem(item) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // padding for floating navigation bars
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VirtualCompactCard(
    item: ScanHistoryEntity,
    onClick: () -> Unit
) {
    val badgeColor = when {
        item.verdict.contains("REAL") -> Color(0xFF00FF87)
        item.verdict.contains("FAKE") -> Color(0xFFFF5252)
        item.verdict.contains("UPSAMPLED") -> Color(0xFF40C4FF)
        else -> Color(0xFFFFD740)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.format} • ${item.sampleRate / 1000}kHz • ${item.bitDepth}bit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.verdict.replace("_", " "),
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.confidence}% Confidence",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}
