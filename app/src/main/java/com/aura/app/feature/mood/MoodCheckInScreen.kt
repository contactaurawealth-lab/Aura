package com.aura.app.feature.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.database.MoodEntryEntity
import com.aura.app.core.designsystem.*

@Composable
fun MoodCheckInScreen(
    onSaveMood: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val moods = listOf(
        Pair("Calm", "Centered and still"),
        Pair("Happy", "Warm and uplifted"),
        Pair("Tired", "Low energy, needs rest"),
        Pair("Stressed", "Tension in body or mind"),
        Pair("Unfocused", "Scattered thoughts"),
        Pair("Reflective", "Contemplative & inward")
    )

    var selectedMood by remember { mutableStateOf("Calm") }
    var noteInput by remember { mutableStateOf("") }

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
                    text = "CHECK-IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "A gentle moment of honesty with yourself.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mood options grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                moods.forEach { (moodName, moodDesc) ->
                    val isSelected = selectedMood == moodName
                    AuraCard(
                        onClick = { selectedMood = moodName },
                        backgroundColor = if (isSelected) SoftIvory else MaterialTheme.colorScheme.surface,
                        borderColor = if (isSelected) SageBrand else BorderDivider
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = moodName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) DeepBotanical else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = moodDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMood = moodName },
                                colors = RadioButtonDefaults.colors(selectedColor = SageBrand)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional Note
            Text(
                text = "OPTIONAL NOTE",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("What led to this feeling?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SoftIvory,
                    unfocusedContainerColor = SoftIvory,
                    focusedBorderColor = SageBrand,
                    unfocusedBorderColor = BorderDivider
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuraButton(
                text = "Save Check-In",
                onClick = {
                    onSaveMood(selectedMood, noteInput)
                    onNavigateBack()
                }
            )
        }
    }
}
