package com.aura.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.designsystem.*

@Composable
fun HistoryScreen(
    totalMeditationSeconds: Long,
    completedMeditationCount: Int,
    totalFocusSeconds: Long,
    completedFocusCount: Int,
    completedBreathingCount: Int,
    voiceNotesCount: Int,
    onNavigateBack: () -> Unit
) {
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
                .verticalScroll(rememberScrollState())
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
                    text = "INSIGHTS & HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Mindful Footprint",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Deterministic, grounded metrics calculated strictly from your local practice records.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Metric 2x2 Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "MEDITATION",
                    value = "${totalMeditationSeconds / 60}m",
                    subtitle = "$completedMeditationCount completed",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "FOCUS TIME",
                    value = "${totalFocusSeconds / 60}m",
                    subtitle = "$completedFocusCount sessions",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "BREATHING",
                    value = "$completedBreathingCount",
                    subtitle = "Breath sessions",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "THOUGHTS",
                    value = "$voiceNotesCount",
                    subtitle = "Journal entries",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AuraCard {
                Text(
                    text = "Quiet Consistency",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBotanical
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aura avoids streak shaming and competitive gamification. Even two minutes of quiet breath is enough to reset your autonomic nervous system.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    AuraCard(
        modifier = modifier,
        backgroundColor = SoftIvory
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = StoneNeutral
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = DeepBotanical
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )
    }
}
