package com.aura.app.core.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aura.app.core.database.AudioTrackEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

data class PlaybackState(
    val currentTrack: AudioTrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<AudioTrackEntity> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val sleepTimerMinutesRemaining: Int? = null,
    val isSleepTimerFadeOut: Boolean = true
)

class AudioPlaybackManager(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerTotalSeconds = 0
    private var originalSleepSeconds = 0

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, AuraMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startPositionPolling()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    updatePositionAndDuration()
                    if (state == Player.STATE_ENDED) {
                        handleTrackCompletion()
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            updatePositionAndDuration()
            if (_playbackState.value.isPlaying) {
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun startPositionPolling() {
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    private fun updatePositionAndDuration() {
        controller?.let { c ->
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = if (c.duration > 0) c.duration else _playbackState.value.currentTrack?.durationMs ?: 0L,
                isPlaying = c.isPlaying
            )
        }
    }

    fun playTrack(track: AudioTrackEntity) {
        val existingIndex = _playbackState.value.queue.indexOfFirst { it.id == track.id }
        val newQueue = if (existingIndex != -1) {
            _playbackState.value.queue
        } else {
            _playbackState.value.queue + track
        }
        val targetIndex = if (existingIndex != -1) existingIndex else newQueue.lastIndex

        _playbackState.value = _playbackState.value.copy(
            queue = newQueue,
            currentQueueIndex = targetIndex
        )
        playInternal(track)
    }

    fun playQueue(tracks: List<AudioTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _playbackState.value = _playbackState.value.copy(
            queue = tracks,
            currentQueueIndex = safeIndex
        )
        playInternal(tracks[safeIndex])
    }

    fun addToQueue(track: AudioTrackEntity) {
        val currentQueue = _playbackState.value.queue
        if (currentQueue.none { it.id == track.id }) {
            _playbackState.value = _playbackState.value.copy(queue = currentQueue + track)
        }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            var newIndex = _playbackState.value.currentQueueIndex
            if (index < newIndex) {
                newIndex = (newIndex - 1).coerceAtLeast(0)
            } else if (newIndex >= currentQueue.size) {
                newIndex = (currentQueue.size - 1).coerceAtLeast(0)
            }
            _playbackState.value = _playbackState.value.copy(
                queue = currentQueue,
                currentQueueIndex = newIndex
            )
        }
    }

    fun playNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled && queue.size > 1) {
            var r = Random.nextInt(queue.size)
            while (r == state.currentQueueIndex) {
                r = Random.nextInt(queue.size)
            }
            r
        } else {
            (state.currentQueueIndex + 1) % queue.size
        }

        _playbackState.value = state.copy(currentQueueIndex = nextIndex)
        playInternal(queue[nextIndex])
    }

    fun playPrevious() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // If played more than 3 seconds, replay current track
        if (state.currentPositionMs > 3000L) {
            seekTo(0)
            return
        }

        val prevIndex = if (state.currentQueueIndex - 1 < 0) {
            queue.lastIndex
        } else {
            state.currentQueueIndex - 1
        }

        _playbackState.value = state.copy(currentQueueIndex = prevIndex)
        playInternal(queue[prevIndex])
    }

    private fun playInternal(track: AudioTrackEntity) {
        val c = controller ?: return
        val file = File(track.filePath)
        val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse("file://${track.filePath}")

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setDescription(track.description)
                    .build()
            )
            .build()

        c.setMediaItem(mediaItem)
        c.prepare()
        c.volume = 1.0f
        c.play()

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            isPlaying = true
        )
    }

    private fun handleTrackCompletion() {
        val state = _playbackState.value
        when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                seekTo(0)
                controller?.play()
            }
            Player.REPEAT_MODE_ALL -> {
                playNext()
            }
            else -> {
                // REPEAT_MODE_OFF: if there is next track in queue, play it
                if (state.currentQueueIndex < state.queue.lastIndex) {
                    playNext()
                } else {
                    _playbackState.value = state.copy(isPlaying = false, currentPositionMs = 0)
                }
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        updatePositionAndDuration()
    }

    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(
            isShuffleEnabled = !_playbackState.value.isShuffleEnabled
        )
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = nextMode
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
    }

    fun stop() {
        controller?.stop()
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0)
        cancelSleepTimer()
    }

    fun setSleepTimer(minutes: Int, withFadeOut: Boolean = true) {
        cancelSleepTimer()
        sleepTimerTotalSeconds = minutes * 60
        originalSleepSeconds = sleepTimerTotalSeconds
        _playbackState.value = _playbackState.value.copy(
            sleepTimerMinutesRemaining = minutes,
            isSleepTimerFadeOut = withFadeOut
        )

        sleepTimerRunnable = object : Runnable {
            override fun run() {
                sleepTimerTotalSeconds -= 1
                if (sleepTimerTotalSeconds <= 0) {
                    controller?.volume = 1.0f
                    stop()
                    _playbackState.value = _playbackState.value.copy(sleepTimerMinutesRemaining = null)
                } else {
                    // Optional gentle fade out during final 30 seconds
                    if (_playbackState.value.isSleepTimerFadeOut && sleepTimerTotalSeconds <= 30) {
                        val fadeFactor = (sleepTimerTotalSeconds / 30f).coerceIn(0.05f, 1.0f)
                        controller?.volume = fadeFactor
                    }

                    _playbackState.value = _playbackState.value.copy(
                        sleepTimerMinutesRemaining = (sleepTimerTotalSeconds + 59) / 60
                    )
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(sleepTimerRunnable!!, 1000)
    }

    fun cancelSleepTimer() {
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable = null
        controller?.volume = 1.0f
        _playbackState.value = _playbackState.value.copy(sleepTimerMinutesRemaining = null)
    }

    fun release() {
        handler.removeCallbacks(pollRunnable)
        cancelSleepTimer()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
