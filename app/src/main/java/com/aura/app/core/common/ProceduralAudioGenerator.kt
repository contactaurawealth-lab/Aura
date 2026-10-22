package com.aura.app.core.common

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object ProceduralAudioGenerator {

    enum class SoundType {
        RAIN,
        FOREST_STREAM,
        SINGING_BOWL,
        BROWN_NOISE,
        MEDITATION_BELL,
        OCEAN_WAVES,
        GENTLE_WIND,
        WHITE_NOISE
    }

    fun ensureStarterAudioFiles(context: Context): Map<SoundType, File> {
        val audioDir = File(context.filesDir, "starter_audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val map = mutableMapOf<SoundType, File>()
        for (type in SoundType.values()) {
            val file = File(audioDir, "${type.name.lowercase()}.wav")
            if (!file.exists() || file.length() < 1000) {
                generateWav(file, type, durationSeconds = 12)
            }
            map[type] = file
        }
        return map
    }

    fun getAudioFile(context: Context, type: SoundType): File {
        val audioDir = File(context.filesDir, "starter_audio")
        val file = File(audioDir, "${type.name.lowercase()}.wav")
        if (!file.exists() || file.length() < 1000) {
            audioDir.mkdirs()
            generateWav(file, type, durationSeconds = 12)
        }
        return file
    }

    private fun generateWav(outputFile: File, type: SoundType, durationSeconds: Int) {
        val sampleRate = 22050
        val numSamples = sampleRate * durationSeconds
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)

        val fos = FileOutputStream(outputFile)
        // Placeholder for 44-byte WAV header
        fos.write(ByteArray(44))

        val buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        var lastBrown = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal: Double = when (type) {
                SoundType.RAIN -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    lastBrown = (lastBrown * 0.85) + (white * 0.15)
                    val drop = if (Random.nextInt(1000) < 5) sin(2 * PI * 1200 * t) * 0.4 else 0.0
                    (lastBrown * 0.6 + drop).coerceIn(-1.0, 1.0)
                }
                SoundType.FOREST_STREAM -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    lastBrown = (lastBrown * 0.92) + (white * 0.08)
                    val wave = sin(2 * PI * 0.25 * t) * 0.2
                    (lastBrown * 0.7 + wave).coerceIn(-1.0, 1.0)
                }
                SoundType.SINGING_BOWL -> {
                    val envelope = exp(-t * 0.25)
                    val fundamental = sin(2 * PI * 432.0 * t)
                    val harmonic = sin(2 * PI * 864.0 * t) * 0.25
                    val beat = sin(2 * PI * 1.5 * t) * 0.1
                    ((fundamental + harmonic + beat) * envelope * 0.75).coerceIn(-1.0, 1.0)
                }
                SoundType.BROWN_NOISE -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    lastBrown = (lastBrown * 0.96) + (white * 0.04)
                    (lastBrown * 0.8).coerceIn(-1.0, 1.0)
                }
                SoundType.MEDITATION_BELL -> {
                    val envelope = exp(-t * 0.4)
                    val bell = sin(2 * PI * 528.0 * t) + sin(2 * PI * 1056.0 * t) * 0.3
                    (bell * envelope * 0.8).coerceIn(-1.0, 1.0)
                }
                SoundType.OCEAN_WAVES -> {
                    // Periodic swell with surf cresting
                    val white = Random.nextDouble(-1.0, 1.0)
                    lastBrown = (lastBrown * 0.94) + (white * 0.06)
                    val surge = (sin(2 * PI * 0.12 * t) + 1.0) * 0.5 // 0.0 to 1.0 swell
                    (lastBrown * (0.3 + 0.7 * surge)).coerceIn(-1.0, 1.0)
                }
                SoundType.GENTLE_WIND -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    lastBrown = (lastBrown * 0.90) + (white * 0.10)
                    val gust = sin(2 * PI * 0.18 * t) * sin(2 * PI * 0.05 * t) * 0.35
                    (lastBrown * 0.6 + gust).coerceIn(-1.0, 1.0)
                }
                SoundType.WHITE_NOISE -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    (white * 0.35).coerceIn(-1.0, 1.0)
                }
            }

            val shortVal = (sampleVal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            buffer.clear()
            buffer.putShort(shortVal)
            fos.write(buffer.array())
        }
        fos.close()

        val totalAudioLen = (numSamples * numChannels * (bitsPerSample / 8)).toLong()
        val totalDataLen = totalAudioLen + 36
        writeWavHeader(outputFile, totalAudioLen, totalDataLen, sampleRate.toLong(), numChannels, byteRate.toLong())
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        raf.write(header)
        raf.close()
    }
}
