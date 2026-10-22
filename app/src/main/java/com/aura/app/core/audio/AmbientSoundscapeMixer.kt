package com.aura.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import com.aura.app.core.common.ProceduralAudioGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AmbientMixerState(
    val rainVolume: Float = 0f,
    val forestVolume: Float = 0f,
    val oceanVolume: Float = 0f,
    val windVolume: Float = 0f,
    val whiteNoiseVolume: Float = 0f
) {
    val isAnyActive: Boolean
        get() = rainVolume > 0f || forestVolume > 0f || oceanVolume > 0f || windVolume > 0f || whiteNoiseVolume > 0f
}

class AmbientSoundscapeMixer(private val context: Context) {

    private val _mixerState = MutableStateFlow(AmbientMixerState())
    val mixerState: StateFlow<AmbientMixerState> = _mixerState.asStateFlow()

    private val playerMap = mutableMapOf<ProceduralAudioGenerator.SoundType, MediaPlayer>()

    fun setLayerVolume(soundType: ProceduralAudioGenerator.SoundType, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)

        when (soundType) {
            ProceduralAudioGenerator.SoundType.RAIN ->
                _mixerState.value = _mixerState.value.copy(rainVolume = clamped)
            ProceduralAudioGenerator.SoundType.FOREST_STREAM ->
                _mixerState.value = _mixerState.value.copy(forestVolume = clamped)
            ProceduralAudioGenerator.SoundType.OCEAN_WAVES ->
                _mixerState.value = _mixerState.value.copy(oceanVolume = clamped)
            ProceduralAudioGenerator.SoundType.GENTLE_WIND ->
                _mixerState.value = _mixerState.value.copy(windVolume = clamped)
            ProceduralAudioGenerator.SoundType.WHITE_NOISE ->
                _mixerState.value = _mixerState.value.copy(whiteNoiseVolume = clamped)
            else -> {}
        }

        updatePlayer(soundType, clamped)
    }

    private fun updatePlayer(soundType: ProceduralAudioGenerator.SoundType, volume: Float) {
        if (volume > 0.01f) {
            val player = playerMap.getOrPut(soundType) {
                createPlayer(soundType) ?: return
            }
            try {
                player.setVolume(volume, volume)
                if (!player.isPlaying) {
                    player.start()
                }
            } catch (e: Exception) {
                // If player is invalid, recreate
                playerMap.remove(soundType)?.release()
                createPlayer(soundType)?.let {
                    it.setVolume(volume, volume)
                    it.start()
                    playerMap[soundType] = it
                }
            }
        } else {
            playerMap.remove(soundType)?.let {
                try {
                    it.stop()
                    it.release()
                } catch (e: Exception) {}
            }
        }
    }

    private fun createPlayer(soundType: ProceduralAudioGenerator.SoundType): MediaPlayer? {
        val file: File = ProceduralAudioGenerator.getAudioFile(context, soundType)
        if (!file.exists()) return null

        return try {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                isLooping = true
                prepare()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun stopAll() {
        _mixerState.value = AmbientMixerState()
        for ((_, player) in playerMap) {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {}
        }
        playerMap.clear()
    }

    fun release() {
        stopAll()
    }
}
