package com.aura.app.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onSaveGoals: suspend (Set<String>) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val selectedGoals = remember { mutableStateListOf<String>() }
    val availableGoals = listOf(
        "Feel calmer",
        "Improve focus",
        "Sleep better",
        "Build a meditation habit",
        "Capture my thoughts",
        "Reduce distractions"
    )

    var preferredDuration by remember { mutableStateOf("15 min") }
    val durations = listOf("5 min", "10 min", "15 min", "25 min")

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
        ) {
            // Step Indicator Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { stepIndex ->
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (currentStep == stepIndex) 28.dp else 12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentStep == stepIndex) SageBrand
                                    else BorderDivider
                                )
                        )
                    }
                }
                if (currentStep < 3) {
                    TextButton(onClick = {
                        scope.launch {
                            onSaveGoals(selectedGoals.toSet())
                            onFinished()
                        }
                    }) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    0 -> StepWelcome()
                    1 -> StepGoals(
                        availableGoals = availableGoals,
                        selectedGoals = selectedGoals,
                        onToggleGoal = { goal ->
                            if (selectedGoals.contains(goal)) selectedGoals.remove(goal)
                            else selectedGoals.add(goal)
                        }
                    )
                    2 -> StepRoutine(
                        durations = durations,
                        selectedDuration = preferredDuration,
                        onSelectDuration = { preferredDuration = it }
                    )
                    3 -> StepPermissions()
                }
            }

            // Bottom Navigation Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    AuraOutlinedButton(
                        text = "Back",
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    )
                }
                AuraButton(
                    text = if (currentStep == 3) "Enter Sanctuary" else "Continue",
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            scope.launch {
                                onSaveGoals(selectedGoals.toSet())
                                onFinished()
                            }
                        }
                    },
                    modifier = Modifier.weight(if (currentStep > 0) 1.5f else 1f)
                )
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(SoftIvory),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_aura_mark),
                contentDescription = "Aura Botanical Mark",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Make space for yourself.",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A calm native sanctuary for daily mindfulness, ambient music, reflective voice journaling, and intentional focus.",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        AuraCard {
            Text(
                text = "• 100% Offline-first & Private\n• No mandatory accounts or subscriptions\n• Thoughtfully engineered for quiet living",
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBotanical,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

@Composable
private fun StepGoals(
    availableGoals: List<String>,
    selectedGoals: List<String>,
    onToggleGoal: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "What brings you here?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select what matters most. Aura adapts quietly to your intentions.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        availableGoals.forEach { goal ->
            val isSelected = selectedGoals.contains(goal)
            AuraCard(
                modifier = Modifier.padding(vertical = 6.dp),
                onClick = { onToggleGoal(goal) },
                backgroundColor = if (isSelected) SoftIvory else MaterialTheme.colorScheme.surface,
                borderColor = if (isSelected) SageBrand else BorderDivider
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = goal,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) DeepBotanical else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onToggleGoal(goal) },
                        colors = RadioButtonDefaults.colors(selectedColor = SageBrand)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRoutine(
    durations: List<String>,
    selectedDuration: String,
    onSelectDuration: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your daily rhythm",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Set a comfortable session length. You can always change this later.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "PREFERRED SESSION LENGTH",
            style = MaterialTheme.typography.labelSmall,
            color = StoneNeutral
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { dur ->
                AuraPill(
                    text = dur,
                    selected = selectedDuration == dur,
                    onClick = { onSelectDuration(dur) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AuraCard {
            Text(
                text = "Gentle consistency",
                style = MaterialTheme.typography.titleMedium,
                color = DeepBotanical
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Short sessions of 5–15 minutes build enduring habits more naturally than infrequent long ones.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun StepPermissions() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Privacy & Permissions",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Aura respects your device. Each permission is optional and only requested when you use that specific feature.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuraCard(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(text = "🎙 Microphone", style = MaterialTheme.typography.titleMedium, color = DeepBotanical)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Used solely for your private voice journal notes. Audio stays strictly on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }

        AuraCard(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(text = "🔔 Notifications", style = MaterialTheme.typography.titleMedium, color = DeepBotanical)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Shows media controls on lock screen and gentle daily reminders when enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }

        AuraCard(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(text = "✨ Floating Assistant & Blocker", style = MaterialTheme.typography.titleMedium, color = DeepBotanical)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Optional overlay and usage access to help you intercept distracting apps during focus sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
    }
}
