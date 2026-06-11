package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10

@Composable
fun SpectrogramScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val spectrogramData by viewModel.spectrogramData.collectAsState()
    val metadata by viewModel.activeMetadata.collectAsState()
    val isLogScale by viewModel.logarithmicScale.collectAsState()
    val paletteName by viewModel.spectrogramColorPalette.collectAsState()

    // Interactive zoom and pan states
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 10f)
        offset += offsetChange
    }

    // Build or render Bitmap from 2D data
    val renderedBitmap = remember(spectrogramData, isLogScale, paletteName) {
        generateSpectrogramBitmap(spectrogramData, isLogScale, paletteName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block with Back action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Navigate Back")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FFT SPECTROGRAM EXPLORER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = metadata?.fileName ?: "No file loaded. Showing baseline model.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    renderedBitmap?.let { bmp ->
                        val savedFile = saveBitmapToGallery(context, bmp)
                        if (savedFile != null) {
                            Toast.makeText(context, "Spectrogram saved to Documents!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.testTag("save_spectrogram_png_button")
            ) {
                Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save as PNG")
            }
        }

        // Settings Selector Row
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scale config Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isLogScale,
                        onCheckedChange = { viewModel.logarithmicScale.value = it }
                    )
                    Text("Logarithmic Scale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Palette toggle Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Palette: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    TextButton(onClick = {
                        val next = if (paletteName == "Inferno") "CyanPink" else "Inferno"
                        viewModel.spectrogramColorPalette.value = next
                    }) {
                        Text(paletteName, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Interactive Spectrogram Visualizer Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .transformable(state = transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            if (renderedBitmap != null) {
                ComposeCanvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawImage(renderedBitmap.asImageBitmap())
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan an audio track to generate spectrogram model.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Reset and Helper block
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tip: Use two-finger pinch to Zoom and Pan",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Button(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Reset View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Builds standard high contrast bitmap out of raw floating power buckets.
 */
fun generateSpectrogramBitmap(
    data: Array<FloatArray>,
    isLogScale: Boolean,
    paletteName: String
): Bitmap? {
    if (data.isEmpty()) return null
    val width = data.size
    val height = data[0].size

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Build Paint resources and draw columns
    for (x in 0 until width) {
        val spectrum = data[x]
        for (y in 0 until height) {
            // Apply scale (spectrograms typically standard coordinates bottom up)
            val mappedY = height - 1 - y
            val amplitude = spectrum[mappedY]

            // Normalize values for visualization colors mapping (dB floor standard is 0 to 100)
            val logVal = if (isLogScale) {
                val db = 20.0 * log10(amplitude.toDouble() + 1e-6)
                // Normalize from -96dB..0dB to 0.0..1.0
                ((db + 96.0) / 96.0).coerceIn(0.0, 1.0)
            } else {
                (amplitude * 10f).toDouble().coerceIn(0.0, 1.0)
            }

            val color = resolvePaletteColor(logVal.toFloat(), paletteName)
            bitmap.setPixel(x, y, color)
        }
    }
    return bitmap
}

/**
 * Maps high resolution spectral density factors to colors.
 */
fun resolvePaletteColor(ratio: Float, name: String): Int {
    if (name == "Inferno") {
        // Inferno color map algorithm (Black -> Red -> Yellow -> White)
        val r = (ratio * 1.5f).coerceIn(0f, 1f)
        val g = (ratio * ratio * 1.2f).coerceIn(0f, 1f)
        val b = (ratio * ratio * ratio * ratio).coerceIn(0f, 1f)
        return android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    } else {
        // Neon Cyan-Pink (Dark slate -> Electric cyan -> Electric Pink -> neon white)
        val r = (ratio * ratio * 1.3f).coerceIn(0f, 1f)
        val g = (1f - ratio).coerceIn(0f, 1f) * ratio
        val b = ratio.coerceIn(0f, 1f)
        return android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }
}

/**
 * Saves generated forensic spectrogram resources to device documents storage safely.
 */
fun saveBitmapToGallery(context: Context, bitmap: Bitmap): File? {
    return try {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
        val file = File(dir, "FLAC_Spectrogram_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    } catch (e: Exception) {
        null
    }
}
