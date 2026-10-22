package com.aura.app.feature.focus

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
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    activeSession: Pair<Long, String>?,
    availableSounds: List<AudioTrackEntity>,
    onStartSession: (Int, String, String?) -> Unit,
    onEndSession: (Long, Long, Boolean) -> Unit,
    onNavigateToAppBlocker: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val durations = listOf(5, 15, 25, 45, 60)
    var selectedDurationMinutes by remember { mutableStateOf(25) }
    var intentionInput by remember { mutableStateOf("") }
    var selectedSound by remember { mutableStateOf<AudioTrackEntity?>(null) }

    // Check if session is active
    val isSessionActive = activeSession != null
    var secondsRemaining by remember { mutableStateOf(0L) }
    var totalTargetSeconds by remember { mutableStateOf(25 * 60L) }

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            val (endTimeMs, intention) = activeSession
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = (endTimeMs - now) / 1000
                if (remaining <= 0) {
                    secondsRemaining = 0
                    onEndSession(totalTargetSeconds, totalTargetSeconds, true)
                    break
                } else {
                    secondsRemaining = remaining
                }
                delay(1000)
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
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "DEEP FOCUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                IconButton(onClick = onNavigateToAppBlocker) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield),
                        contentDescription = "App Restrictor",
                        tint = SageBrand,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSessionActive) {
                // Setup Mode
                Text(
                    text = "Intentional Focus",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Protect your attention and do your most meaningful work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Duration Selector
                Text(
                    text = "SESSION LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.forEach { mins ->
                        AuraPill(
                            text = "$mins m",
                            selected = selectedDurationMinutes == mins,
                            onClick = { selectedDurationMinutes = mins },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Intention Input
                Text(
                    text = "WHAT ARE YOU FOCUSING ON?",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = intentionInput,
                    onValueChange = { intentionInput = it },
                    placeholder = { Text("E.g., Finish system architecture, writing chapter...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SoftIvory,
                        unfocusedContainerColor = SoftIvory,
                        focusedBorderColor = SageBrand,
                        unfocusedBorderColor = BorderDivider
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Ambient Sound Selection
                Text(
                    text = "BACKGROUND SOUNDSCAPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuraCard(
                    onClick = {
                        val focusTracks = availableSounds.filter { it.category == "Focus" || it.category == "Nature sounds" }
                        val nextIndex = (focusTracks.indexOf(selectedSound) + 1) % (focusTracks.size + 1)
                        selectedSound = if (nextIndex < focusTracks.size) focusTracks[nextIndex] else null
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedSound?.title ?: "No background audio (Silent)",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepBotanical
                        )
                        Text(
                            text = "Tap to cycle",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AuraButton(
                    text = "Begin Focus Session",
                    onClick = {
                        totalTargetSeconds = selectedDurationMinutes * 60L
                        onStartSession(
                            selectedDurationMinutes,
                            intentionInput.ifBlank { "Deep Work" },
                            selectedSound?.id
                        )
                    }
                )
            } else {
                // Active Focus Mode
                Spacer(modifier = Modifier.height(20.dp))

                val intention = activeSession?.second ?: "Deep Work"
                Text(
                    text = intention,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Timer Circular Display
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (totalTargetSeconds > 0) {
                        (secondsRemaining.toFloat() / totalTargetSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AuraCircularProgress(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 10.dp,
                        progressColor = SageBrand
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Formatters.formatTimer(secondsRemaining),
                            style = MaterialTheme.typography.displayLarge,
                            color = CharcoalText,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = StoneNeutral
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                AuraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = null,
                            tint = SageBrand,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "App Blocker active. Selected distracting apps will be gently intercepted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                AuraOutlinedButton(
                    text = "End Session Early",
                    onClick = {
                        val elapsed = totalTargetSeconds - secondsRemaining
                        onEndSession(elapsed, totalTargetSeconds, false)
                    },
                    borderColor = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
