package com.aura.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.database.MoodEntryEntity
import com.aura.app.core.database.VoiceNoteEntity
import com.aura.app.core.designsystem.*
import java.util.Calendar

@Composable
fun HomeScreen(
    featuredTrack: AudioTrackEntity?,
    recentlyPlayed: List<AudioTrackEntity>,
    latestMood: MoodEntryEntity?,
    latestVoiceNote: VoiceNoteEntity?,
    todayFocusedSeconds: Long,
    onNavigateToBreathing: () -> Unit,
    onNavigateToMeditation: (String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToVoiceJournal: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToMoodCheckIn: () -> Unit,
    onPlayTrack: (AudioTrackEntity) -> Unit
) {
    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header with greeting and botanical mark
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.formatDate(System.currentTimeMillis()).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = StoneNeutral
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftIvory),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_aura_mark),
                        contentDescription = "Aura",
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Small Mood Check-in Pill
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onNavigateToMoodCheckIn),
                color = SoftIvory,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mood),
                            contentDescription = "Mood",
                            tint = SageBrand,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (latestMood != null) "Current feeling: ${latestMood.mood}" else "How are you feeling right now?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (latestMood != null) "Update" else "Check in",
                        style = MaterialTheme.typography.labelMedium,
                        color = SageBrand
                    )
                }
            }
        }

        // 3. Featured Recommendation Card
        item {
            featuredTrack?.let { track ->
                AuraCard(
                    onClick = { onPlayTrack(track) },
                    backgroundColor = DeepBotanical,
                    borderColor = SageBrand
                ) {
                    Text(
                        text = "FEATURED PRACTICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = SoftIvory
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StoneNeutral
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${track.category} • ${Formatters.formatDurationMinutes(track.durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftIvory
                        )
                        Surface(
                            shape = CircleShape,
                            color = SageBrand,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play),
                                    contentDescription = "Play",
                                    tint = SoftIvory,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Quick-Action Grid
        item {
            Text(
                text = "PRACTICES",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    label = "Breathe",
                    iconRes = R.drawable.ic_breathing,
                    onClick = onNavigateToBreathing,
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    label = "Meditate",
                    iconRes = R.drawable.ic_meditation,
                    onClick = {
                        featuredTrack?.id?.let { onNavigateToMeditation(it) } ?: onNavigateToExplore()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    label = "Focus",
                    iconRes = R.drawable.ic_nav_focus,
                    onClick = onNavigateToFocus,
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    label = "Journal",
                    iconRes = R.drawable.ic_nav_journal,
                    onClick = onNavigateToVoiceJournal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Continue Listening (from local history)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "CONTINUE LISTENING",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentlyPlayed.forEach { track ->
                        AuraCard(
                            onClick = { onPlayTrack(track) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${track.category} • ${track.artist}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play),
                                    contentDescription = "Play",
                                    tint = SageBrand,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Voice Thought
        latestVoiceNote?.let { note ->
            item {
                Text(
                    text = "RECENT THOUGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuraCard(
                    onClick = onNavigateToVoiceJournal
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepBotanical
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (note.transcript.isNotBlank()) {
                        Text(
                            text = "\"${note.transcript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    } else {
                        Text(
                            text = "Audio recording (${Formatters.formatDurationMinutes(note.durationMs)}) • ${Formatters.formatDate(note.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }
        }

        // 7. Today's Focus Summary
        item {
            AuraCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TODAY'S FOCUSED TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = StoneNeutral
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val mins = todayFocusedSeconds / 60
                        Text(
                            text = if (mins > 0) "$mins minutes" else "No focus sessions yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepBotanical
                        )
                    }
                    Button(
                        onClick = onNavigateToFocus,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageBrand)
                    ) {
                        Text("Start", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = SoftIvory,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = SageBrand,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun rememberGreeting(): String {
    val cal = Calendar.getInstance()
    return when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning."
        in 12..17 -> "Good afternoon."
        in 18..22 -> "Good evening."
        else -> "Quiet night."
    }
}
