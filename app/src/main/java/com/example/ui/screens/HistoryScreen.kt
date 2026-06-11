package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScanHistoryEntity
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.Exporter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onSelectHistoryItem: (ScanHistoryEntity) -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val context = LocalContext.current
    val historyList by viewModel.filteredHistory.collectAsState()
    val filterState by viewModel.historyFilter.collectAsState()

    val filterOptions = listOf("All", "Only Real", "Only Fake", "Only Upsampled")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search and export toolbar header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL FORENSICS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onNavigateToCompare,
                        modifier = Modifier.testTag("history_compare_button")
                    ) {
                        Icon(imageVector = Icons.Default.Compare, contentDescription = "Navigate to Compare Deck", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            if (historyList.isNotEmpty()) {
                                val reportFile = Exporter.exportToCsv(context, historyList)
                                Toast.makeText(context, "Exported successfully: ${reportFile.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No scan records to export", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV Report")
                    }
                }
            }
        }

        // Filtering Slider selector row
        item {
            ScrollableRow(
                options = filterOptions,
                selectedOption = filterState,
                onSelectedChange = { viewModel.setHistoryFilter(it) }
            )
        }

        // Action controls
        if (historyList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No history matches the current filter.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(historyList) { item ->
                HistoryRecordCard(
                    item = item,
                    onDelete = { viewModel.deleteScanById(item.id) },
                    onLoad = {
                        onSelectHistoryItem(item)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ScrollableRow(
    options: List<String>,
    selectedOption: String,
    onSelectedChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (option in options) {
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelectedChange(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HistoryRecordCard(
    item: ScanHistoryEntity,
    onDelete: () -> Unit,
    onLoad: () -> Unit
) {
    val dateString = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val badgeColor = when {
        item.verdict.contains("REAL") -> Color(0xFF00FF87)
        item.verdict.contains("FAKE") -> Color(0xFFFF5252)
        item.verdict.contains("UPSAMPLED") -> Color(0xFF40C4FF)
        else -> Color(0xFFFFD740)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
            .clickable(onClick = onLoad)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

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
                    text = "${item.format} • ${item.sampleRate / 1000}kHz • ${item.bitDepth}bit • $dateString",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.verdict.replace("_", " "),
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
