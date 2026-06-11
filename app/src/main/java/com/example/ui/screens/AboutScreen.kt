package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FLAC DETECTIVE REFERENCE MANUAL",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Understanding Codec Forensic Signatures", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "A transcoded, fake lossless file is a lossy audio file (like MP3, AAC, or OGG) that was re-encoded and saved inside a lossless container wrapper like FLAC or WAV. The resulting file is bloated, occupies massive space, but contains no high-frequency content above the original lossy compression limits.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        // Analytical boundaries guidelines
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SPECIFICATION MARKERS REFERENCE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                MarkerDescriptionRow(title = "Genuine Studio Lossless", desc = "Natural frequency range continuing above 22.05 kHz without precipitous slope limits.")
                MarkerDescriptionRow(title = "Upsampled high-res", desc = "Container declares 96/192 kHz sample rate, but energy spectrum completely drops to noise floor (-90dB) at exactly 22.05 kHz or 24 kHz (representing CD upsizing).")
                MarkerDescriptionRow(title = "High quality MP3 transcode", desc = "Severe brickwall low-pass filter transition exactly at 16.0 kHz (for 128kbps) or 20.0 kHz (for 320kbps).")
                MarkerDescriptionRow(title = "Standard AAC transcode", desc = "Sharp, block-like horizontal spectral energy cutoffs around 15.0 - 17.0 kHz.")
            }
        }

        // About developers
        Text(
            text = "FLAC Detective v1.2 • Kotlin-Native DSP Engine\nDesigned for audiophiles, producers, and forensic engineers.\nNo external servers requested.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MarkerDescriptionRow(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
    }
}
