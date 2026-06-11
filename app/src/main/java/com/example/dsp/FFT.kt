package com.example.dsp

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

object FFT {
    /**
     * Applies a Hanning (Hann) window to the input signal to reduce spectral leakage.
     */
    fun applyHanningWindow(signal: FloatArray) {
        val n = signal.size
        for (i in 0 until n) {
            val multiplier = 0.5f * (1.0f - cos(2.0f * PI.toFloat() * i / (n - 1)))
            signal[i] *= multiplier
        }
    }

    /**
     * Performs an in-place Radix-2 Cooley-Tukey FFT.
     * The signal arrays 'real' and 'imag' must be of size N, where N is a power of 2.
     */
    fun performFFT(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n and (n - 1) != 0) {
            throw IllegalArgumentException("FFT size must be a power of 2.")
        }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Cooley-Tukey decimation-in-time
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wR = cos(angle).toFloat()
            val wI = sin(angle).toFloat()
            val halfLen = len shr 1

            for (i in 0 until n step len) {
                var tR = 1.0f
                var tI = 0.0f
                for (k in 0 until halfLen) {
                    val targetIdx = i + k
                    val sourceIdx = targetIdx + halfLen

                    // Butterfly update
                    val uR = real[targetIdx]
                    val uI = imag[targetIdx]

                    val vR = real[sourceIdx] * tR - imag[sourceIdx] * tI
                    val vI = real[sourceIdx] * tI + imag[sourceIdx] * tR

                    real[targetIdx] = uR + vR
                    imag[targetIdx] = uI + vI

                    real[sourceIdx] = uR - vR
                    imag[sourceIdx] = uI - vI

                    // Rotate twiddle factor
                    val nextTR = tR * wR - tI * wI
                    val nextTI = tR * wI + tI * wR
                    tR = nextTR
                    tI = nextTI
                }
            }
            len = len shl 1
        }
    }

    /**
     * Computes the magnitude spectrum of the real signal.
     * Automatically applies Hanning window, runs FFT, and yields magnitudes.
     * Returns a FloatArray of size FFT_SIZE / 2 + 1 containing the magnitude in decibels (dB), or standard amplitudes.
     */
    fun computeMagnitudeSpectrum(signal: FloatArray): FloatArray {
        val n = signal.size
        val real = signal.clone()
        val imag = FloatArray(n)

        applyHanningWindow(real)
        performFFT(real, imag)

        val halfSize = n / 2
        val magnitudes = FloatArray(halfSize)
        for (i in 0 until halfSize) {
            val r = real[i]
            val im = imag[i]
            magnitudes[i] = sqrt(r * r + im * im)
        }
        return magnitudes
    }
}
