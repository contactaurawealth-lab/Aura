package com.aura.app.feature.meditation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.delay

@Composable
fun MeditationScreen(
    track: AudioTrackEntity?,
    onSaveSession: (String?, String, Long, Long, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isSessionRunning by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableStateOf(0L) }
    val targetDurationSeconds = remember(track) { (track?.durationMs ?: 600000L) / 1000 }
    var keepScreenAwake by remember { mutableStateOf(false) }

    LaunchedEffect(isSessionRunning) {
        while (isSessionRunning) {
            delay(1000)
            secondsElapsed += 1
            if (secondsElapsed >= targetDurationSeconds) {
                isSessionRunning = false
                onSaveSession(track?.id, track?.title ?: "Guided Meditation", secondsElapsed, targetDurationSeconds, true)
                break
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (secondsElapsed > 15) {
                        onSaveSession(track?.id, track?.title ?: "Meditation", secondsElapsed, targetDurationSeconds, false)
                    }
                    onNavigateBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "MEDITATION PRACTICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botanical Artwork Card
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(DeepBotanical),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_meditation),
                    contentDescription = null,
                    tint = SoftIvory,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = track?.title ?: "Inner Stillness",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track?.artist ?: "Aura Guided Practice",
                style = MaterialTheme.typography.titleMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!isSessionRunning && secondsElapsed == 0L) {
                // Intro state
                track?.description?.let {
                    AuraCard {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AuraButton(
                    text = "Begin Meditation",
                    onClick = { isSessionRunning = true }
                )
            } else {
                // Active session
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (targetDurationSeconds > 0) {
                        (secondsElapsed.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AuraCircularProgress(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        progressColor = SageBrand
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Formatters.formatTimer(secondsElapsed),
                            style = MaterialTheme.typography.displayMedium,
                            color = CharcoalText,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "of ${Formatters.formatTimer(targetDurationSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StoneNeutral
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraOutlinedButton(
                        text = if (isSessionRunning) "Pause" else "Resume",
                        onClick = { isSessionRunning = !isSessionRunning },
                        modifier = Modifier.weight(1f)
                    )
                    AuraButton(
                        text = "Complete",
                        onClick = {
                            isSessionRunning = false
                            onSaveSession(track?.id, track?.title ?: "Meditation", secondsElapsed, targetDurationSeconds, true)
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
