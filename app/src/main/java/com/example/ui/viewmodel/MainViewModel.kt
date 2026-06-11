package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FlacDetectiveApplication
import com.example.data.db.ScanHistoryEntity
import com.example.data.repository.HistoryRepository
import com.example.dsp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Decoding : ScanUiState
    object Analyzing : ScanUiState
    data class Success(val metadata: AudioMetadata, val result: DetectionResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class CompareState(
    val fileAMetadata: AudioMetadata? = null,
    val fileAResult: DetectionResult? = null,
    val fileASamples: FloatArray? = null,
    val fileBMetadata: AudioMetadata? = null,
    val fileBResult: DetectionResult? = null,
    val fileBSamples: FloatArray? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository = (application as FlacDetectiveApplication).historyRepository

    // History Flow
    val historyList: StateFlow<List<ScanHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered History
    private val _historyFilter = MutableStateFlow("All")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    val filteredHistory: StateFlow<List<ScanHistoryEntity>> = combine(historyList, _historyFilter) { list, filter ->
        when (filter) {
            "Only Fake" -> list.filter { it.verdict.contains("FAKE") }
            "Only Real" -> list.filter { it.verdict.contains("REAL") || it.verdict.contains("LIKELY_LOSSLESS") }
            "Only Upsampled" -> list.filter { it.verdict == AudioVerdict.UPSAMPLED.name }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scanning State
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // Loaded spectrogram data
    private val _spectrogramData = MutableStateFlow<Array<FloatArray>>(emptyArray())
    val spectrogramData: StateFlow<Array<FloatArray>> = _spectrogramData.asStateFlow()

    private val _activeMetadata = MutableStateFlow<AudioMetadata?>(null)
    val activeMetadata: StateFlow<AudioMetadata?> = _activeMetadata.asStateFlow()

    private val _activeSamples = MutableStateFlow<FloatArray>(FloatArray(0))
    val activeSamples: StateFlow<FloatArray> = _activeSamples.asStateFlow()

    // Compare State
    private val _compareState = MutableStateFlow(CompareState())
    val compareState: StateFlow<CompareState> = _compareState.asStateFlow()

    // Batch Scanned Files
    private val _batchList = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    val batchList: StateFlow<List<ScanHistoryEntity>> = _batchList.asStateFlow()

    private val _isBatchScanning = MutableStateFlow(false)
    val isBatchScanning: StateFlow<Boolean> = _isBatchScanning.asStateFlow()

    // Settings
    val fftSizeSelected = MutableStateFlow(4096)
    val logarithmicScale = MutableStateFlow(true)
    val spectrogramColorPalette = MutableStateFlow("Inferno") // Inferno, Magma, CyanPink

    init {
        // Pre-populate some historical scans to let first experience look highly populated and ready for action!
        viewModelScope.launch(Dispatchers.IO) {
            repository.allHistory.first().let { current ->
                if (current.isEmpty()) {
                    createVirtualSoundLibrary()
                }
            }
        }
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    /**
     * Decode and run full spectral analysis on a single selected file from Uri.
     */
    fun analyzeUri(context: Context, uri: Uri) {
        _scanUiState.value = ScanUiState.Decoding
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = copyUriToTempFile(context, uri)
                val metadata = AudioDecoder.decodeAudioFile(tempFile)
                
                _scanUiState.value = ScanUiState.Analyzing
                val samples = AudioDecoder.readPcmSamples(tempFile, metadata.sampleRate, 131072) // ~3 seconds for deep spectrum
                val result = Classifier.analyzeAudio(samples, metadata.sampleRate)

                // Save to state
                _activeMetadata.value = metadata
                _activeSamples.value = samples
                computeSpectrogram(samples, fftSizeSelected.value)

                // Save to Database History
                val entity = ScanHistoryEntity(
                    filePath = metadata.filePath,
                    fileName = metadata.fileName,
                    format = metadata.format,
                    fileSize = metadata.fileSize,
                    sampleRate = metadata.sampleRate,
                    bitrateKbps = metadata.bitrateKbps,
                    durationMs = metadata.durationMs,
                    bitDepth = metadata.bitDepth,
                    channels = metadata.channels,
                    verdict = result.verdict.name,
                    confidence = result.confidenceScore,
                    qualityScore = result.qualityScore,
                    cutoffFrequencyHz = result.cutoffFrequencyHz,
                    codecProbabilityLossless = result.losslessProbability,
                    codecProbabilityMp3 = result.mp3Probability,
                    codecProbabilityAac = result.aacProbability
                )
                repository.insertScan(entity)

                _scanUiState.value = ScanUiState.Success(metadata, result)

                // Try cleaning up temp file safely
                try { tempFile.delete() } catch (ex: Exception) {}
            } catch (e: Exception) {
                _scanUiState.value = ScanUiState.Error("Failed to analyze audio: ${e.message}")
            }
        }
    }

    /**
     * Compute Spectrogram frames from real decoded samples using FFT block strides.
     */
    private fun computeSpectrogram(samples: FloatArray, fftSize: Int) {
        if (samples.isEmpty()) return
        val step = fftSize / 2
        val totalSamples = samples.size
        val numFrames = (totalSamples - fftSize) / step
        if (numFrames <= 0) return

        val frames = Array(numFrames) { FloatArray(fftSize / 2) }
        val windowBuffer = FloatArray(fftSize)

        for (f in 0 until numFrames) {
            val startIdx = f * step
            System.arraycopy(samples, startIdx, windowBuffer, 0, fftSize)
            val mags = FFT.computeMagnitudeSpectrum(windowBuffer)
            System.arraycopy(mags, 0, frames[f], 0, fftSize / 2)
        }
        _spectrogramData.value = frames
    }

    /**
     * Adds virtual music entries to database for testing, comparing, and exploration.
     */
    fun createVirtualSoundLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val sampleItems = listOf(
                ScanHistoryEntity(
                    filePath = "/virtual/chopin_nocturne_master.flac",
                    fileName = "Chopin_Nocturne_Op9_StudioMaster.flac",
                    format = "FLAC",
                    fileSize = 48510204L,
                    sampleRate = 96000,
                    bitrateKbps = 2410.0,
                    durationMs = 284000,
                    bitDepth = 24,
                    channels = 2,
                    verdict = AudioVerdict.REAL_LOSSLESS.name,
                    confidence = 98,
                    qualityScore = 96,
                    cutoffFrequencyHz = 42000.0,
                    codecProbabilityLossless = 100,
                    codecProbabilityMp3 = 0,
                    codecProbabilityAac = 0
                ),
                ScanHistoryEntity(
                    filePath = "/virtual/viva_la_vida_fake.flac",
                    fileName = "Viva_La_Vida_Original_Transcode.flac",
                    format = "FLAC",
                    fileSize = 31201402L,
                    sampleRate = 44100,
                    bitrateKbps = 870.0,
                    durationMs = 242000,
                    bitDepth = 16,
                    channels = 2,
                    verdict = AudioVerdict.FAKE_LOSSLESS.name,
                    confidence = 94,
                    qualityScore = 32,
                    cutoffFrequencyHz = 16000.0,
                    codecProbabilityLossless = 2,
                    codecProbabilityMp3 = 92,
                    codecProbabilityAac = 6
                ),
                ScanHistoryEntity(
                    filePath = "/virtual/hotel_california_upsampled.flac",
                    fileName = "Hotel_California_96k_Upsampled.flac",
                    format = "FLAC",
                    fileSize = 78105300L,
                    sampleRate = 96000,
                    bitrateKbps = 2800.0,
                    durationMs = 390000,
                    bitDepth = 24,
                    channels = 2,
                    verdict = AudioVerdict.UPSAMPLED.name,
                    confidence = 96,
                    qualityScore = 44,
                    cutoffFrequencyHz = 22050.0,
                    codecProbabilityLossless = 82,
                    codecProbabilityMp3 = 10,
                    codecProbabilityAac = 8
                ),
                ScanHistoryEntity(
                    filePath = "/virtual/billie_jean_aac.m4a",
                    fileName = "Billie_Jean_iTunesStore.m4a",
                    format = "AAC",
                    fileSize = 8901240L,
                    sampleRate = 44100,
                    bitrateKbps = 256.0,
                    durationMs = 294000,
                    bitDepth = 16,
                    channels = 2,
                    verdict = AudioVerdict.LIKELY_FAKE.name,
                    confidence = 85,
                    qualityScore = 78,
                    cutoffFrequencyHz = 20000.0,
                    codecProbabilityLossless = 12,
                    codecProbabilityMp3 = 10,
                    codecProbabilityAac = 78
                )
            )

            for (item in sampleItems) {
                repository.insertScan(item)
            }
        }
    }

    /**
     * Scan Multiple files in batch mode
     */
    fun scanMultipleUris(context: Context, uris: List<Uri>) {
        _isBatchScanning.value = true
        _batchList.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<ScanHistoryEntity>()
            for (uri in uris) {
                try {
                    val tempFile = copyUriToTempFile(context, uri)
                    val metadata = AudioDecoder.decodeAudioFile(tempFile)
                    val samples = AudioDecoder.readPcmSamples(tempFile, metadata.sampleRate, 65536)
                    val result = Classifier.analyzeAudio(samples, metadata.sampleRate)

                    val entity = ScanHistoryEntity(
                        filePath = metadata.filePath,
                        fileName = metadata.fileName,
                        format = metadata.format,
                        fileSize = metadata.fileSize,
                        sampleRate = metadata.sampleRate,
                        bitrateKbps = metadata.bitrateKbps,
                        durationMs = metadata.durationMs,
                        bitDepth = metadata.bitDepth,
                        channels = metadata.channels,
                        verdict = result.verdict.name,
                        confidence = result.confidenceScore,
                        qualityScore = result.qualityScore,
                        cutoffFrequencyHz = result.cutoffFrequencyHz,
                        codecProbabilityLossless = result.losslessProbability,
                        codecProbabilityMp3 = result.mp3Probability,
                        codecProbabilityAac = result.aacProbability
                    )
                    repository.insertScan(entity)
                    results.add(entity)

                    try { tempFile.delete() } catch (e: Exception) {}
                } catch (e: Exception) {
                    // skip failed files in batch
                }
            }
            _batchList.value = results
            _isBatchScanning.value = false
        }
    }

    /**
     * Delete a single scanned record by its database ID.
     */
    fun deleteScanById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScanById(id)
        }
    }

    /**
     * Clear all scanned results from historical database.
     */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    /**
     * Setup files for Comparison slot A & B
     */
    fun selectForCompare(slot: String, entity: ScanHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val virtualFile = File(entity.filePath)
            val metadata = AudioMetadata(
                fileName = entity.fileName,
                filePath = entity.filePath,
                fileSize = entity.fileSize,
                format = entity.format,
                durationMs = entity.durationMs,
                sampleRate = entity.sampleRate,
                channels = entity.channels,
                bitDepth = entity.bitDepth,
                bitrateKbps = entity.bitrateKbps
            )
            val samples = AudioDecoder.readPcmSamples(virtualFile, entity.sampleRate, 65536)
            val result = DetectionResult(
                verdict = AudioVerdict.valueOf(entity.verdict),
                confidenceScore = entity.confidence,
                qualityScore = entity.qualityScore,
                cutoffFrequencyHz = entity.cutoffFrequencyHz,
                mp3Probability = entity.codecProbabilityMp3,
                aacProbability = entity.codecProbabilityAac,
                losslessProbability = entity.codecProbabilityLossless,
                dynamicRangeDb = 50.0,
                reasons = emptyList(),
                spectralIntegrity = 90,
                codecAuthenticity = 90
            )

            if (slot == "A") {
                _compareState.value = _compareState.value.copy(
                    fileAMetadata = metadata,
                    fileAResult = result,
                    fileASamples = samples
                )
            } else {
                _compareState.value = _compareState.value.copy(
                    fileBMetadata = metadata,
                    fileBResult = result,
                    fileBSamples = samples
                )
            }
        }
    }

    /**
     * Helper to load a ScanHistoryItem directly into the spectrogram viewer
     */
    fun loadHistoryToSpectrogram(entity: ScanHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val virtualFile = File(entity.filePath)
            val metadata = AudioMetadata(
                fileName = entity.fileName,
                filePath = entity.filePath,
                fileSize = entity.fileSize,
                format = entity.format,
                durationMs = entity.durationMs,
                sampleRate = entity.sampleRate,
                channels = entity.channels,
                bitDepth = entity.bitDepth,
                bitrateKbps = entity.bitrateKbps
            )
            val samples = AudioDecoder.readPcmSamples(virtualFile, entity.sampleRate, 131072)
            val result = Classifier.analyzeAudio(samples, entity.sampleRate)

            _activeMetadata.value = metadata
            _activeSamples.value = samples
            computeSpectrogram(samples, fftSizeSelected.value)
            
            _scanUiState.value = ScanUiState.Success(metadata, result)
        }
    }

    /**
     * Copy uri resource to secure local cache file so that MediaExtractor / Decoders can run native operations.
     */
    private fun copyUriToTempFile(context: Context, uri: Uri): File {
        var fileName = "temp_audio_detect"
        val mimeType = context.contentResolver.getType(uri)
        val ext = when (mimeType) {
            "audio/x-wav", "audio/wav" -> "wav"
            "audio/x-flac", "audio/flac" -> "flac"
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/m4a" -> "m4a"
            else -> "audio_temp"
        }

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
