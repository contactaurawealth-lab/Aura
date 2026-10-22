package com.aura.app.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

import com.aura.app.R
import com.aura.app.core.audio.AmbientMixerState
import com.aura.app.core.audio.PlaybackState
import com.aura.app.core.common.Formatters
import com.aura.app.core.common.ProceduralAudioGenerator
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.designsystem.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    track: AudioTrackEntity?,
    playbackState: PlaybackState,
    ambientMixerState: AmbientMixerState,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSleepTimer: (Int, Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onSetAmbientVolume: (ProceduralAudioGenerator.SoundType, Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerFadeOut by remember { mutableStateOf(true) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAmbientMixerSheet by remember { mutableStateOf(false) }

    // Subtle ambient breathing pulse animation on album artwork
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (playbackState.isPlaying) 1.045f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artworkScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Nav Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = (track?.category ?: "NOW PLAYING").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                IconButton(
                    onClick = {
                        track?.let { onToggleFavorite(it.id, !it.isFavorite) }
                    }
                ) {
                    Text(
                        text = if (track?.isFavorite == true) "★" else "☆",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (track?.isFavorite == true) MutedAmber else StoneNeutral
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Immersive Artwork with Pulse
            AuraAlbumArtThumbnail(
                albumArtUri = track?.albumArtUri,
                category = track?.category ?: "",
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(36.dp)),
                iconSize = 80.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Title and Artist
            Text(
                text = track?.title ?: "Select a practice",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track?.artist ?: "Aura Sanctuary",
                style = MaterialTheme.typography.titleMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Slider
            val duration = if (playbackState.durationMs > 0) playbackState.durationMs else (track?.durationMs ?: 1L).coerceAtLeast(1L)
            val progress = (playbackState.currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeekTo((frac * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = SageBrand,
                    activeTrackColor = SageBrand,
                    inactiveTrackColor = BorderDivider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatters.formatTimer(playbackState.currentPositionMs / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
                Text(
                    text = Formatters.formatTimer(duration / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shuffle),
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffleEnabled) SageBrand else StoneNeutral,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Track
                IconButton(onClick = onPlayPrevious) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_prev),
                        contentDescription = "Previous",
                        tint = CharcoalText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Floating Orb
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = SageBrand,
                    modifier = Modifier.size(68.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                id = if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = SoftIvory,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Next Track
                IconButton(onClick = onPlayNext) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        tint = CharcoalText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Button
                IconButton(onClick = onCycleRepeat) {
                    val repeatIconColor = when (playbackState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> MutedAmber
                        Player.REPEAT_MODE_ALL -> SageBrand
                        else -> StoneNeutral
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_repeat),
                        contentDescription = "Repeat",
                        tint = repeatIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auxiliary Actions Bar: Queue, Ambient Layers Mixer, Sleep Timer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = SoftIvory,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Queue Button
                    TextButton(onClick = { showQueueSheet = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_queue),
                            contentDescription = null,
                            tint = SageBrand,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Queue (${playbackState.queue.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = CharcoalText
                        )
                    }

                    // Ambient Mixer Button
                    TextButton(onClick = { showAmbientMixerSheet = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_layers),
                            contentDescription = null,
                            tint = if (ambientMixerState.isAnyActive) MutedAmber else SageBrand,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (ambientMixerState.isAnyActive) "Layers (Active)" else "Layers",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ambientMixerState.isAnyActive) MutedAmber else CharcoalText
                        )
                    }

                    // Sleep Timer Button
                    TextButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = null,
                            tint = if (playbackState.sleepTimerMinutesRemaining != null) MutedAmber else SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playbackState.sleepTimerMinutesRemaining?.let { "${it}m" } ?: "Timer",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playbackState.sleepTimerMinutesRemaining != null) MutedAmber else CharcoalText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description / Ambient Notes
            track?.description?.let { desc ->
                if (desc.isNotBlank()) {
                    AuraCard {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose duration to stop playback automatically:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gentle volume fade-out", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = sleepTimerFadeOut,
                            onCheckedChange = { sleepTimerFadeOut = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SageBrand)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                        AuraOutlinedButton(
                            text = "$mins minutes",
                            onClick = {
                                onSetSleepTimer(mins, sleepTimerFadeOut)
                                showSleepTimerDialog = false
                            }
                        )
                    }

                    if (playbackState.sleepTimerMinutesRemaining != null) {
                        TextButton(
                            onClick = {
                                onCancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn off active timer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }

    // Queue Modal Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = SoftIvory,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "UP NEXT IN QUEUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (playbackState.queue.isEmpty()) {
                    Text(
                        text = "The queue is currently empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playbackState.queue) { index, queueTrack ->
                            val isCurrent = index == playbackState.currentQueueIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                color = if (isCurrent) SageBrand.copy(alpha = 0.12f) else SoftIvory,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onSelectQueueItem(index)
                                                showQueueSheet = false
                                            }
                                    ) {
                                        Text(
                                            text = queueTrack.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isCurrent) SageBrand else CharcoalText,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${queueTrack.artist} • ${Formatters.formatTimer(queueTrack.durationMs / 1000)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                    }

                                    IconButton(onClick = { onRemoveFromQueue(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = SecondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Ambient Soundscape Layers Mixer Bottom Sheet
    if (showAmbientMixerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAmbientMixerSheet = false },
            containerColor = SoftIvory,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "SOUNDSCAPE LAYER MIXER",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Blend nature acoustics with your music",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(18.dp))

                val layers = listOf(
                    Triple("Gentle Rain", ProceduralAudioGenerator.SoundType.RAIN, ambientMixerState.rainVolume),
                    Triple("Forest Stream", ProceduralAudioGenerator.SoundType.FOREST_STREAM, ambientMixerState.forestVolume),
                    Triple("Ocean Waves", ProceduralAudioGenerator.SoundType.OCEAN_WAVES, ambientMixerState.oceanVolume),
                    Triple("Mountain Wind", ProceduralAudioGenerator.SoundType.GENTLE_WIND, ambientMixerState.windVolume),
                    Triple("White Noise", ProceduralAudioGenerator.SoundType.WHITE_NOISE, ambientMixerState.whiteNoiseVolume)
                )

                layers.forEach { (name, type, volume) ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (volume > 0f) SageBrand else SecondaryText
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = { onSetAmbientVolume(type, it) },
                            colors = SliderDefaults.colors(
                                thumbColor = SageBrand,
                                activeTrackColor = SageBrand,
                                inactiveTrackColor = BorderDivider
                            )
                        )
                    }
                }
            }
        }
    }
}
