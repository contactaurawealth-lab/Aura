package com.aura.app.feature.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.designsystem.*
import com.aura.app.core.permissions.PermissionHelper
import com.aura.app.feature.assistant.FloatingAssistantService

@Composable
fun SettingsScreen(
    currentTheme: String,
    isReducedMotion: Boolean,
    isAssistantEnabled: Boolean,
    onSetTheme: (String) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetAssistantEnabled: (Boolean) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onResetOnboarding: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlay = remember { PermissionHelper.isOverlayGranted(context) }

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
                    text = "SETTINGS & PRIVACY",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Appearance
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(10.dp))
            AuraCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { themeMode ->
                            AuraPill(
                                text = themeMode.lowercase().replaceFirstChar { it.uppercase() },
                                selected = currentTheme == themeMode,
                                onClick = { onSetTheme(themeMode) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderDivider)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion", style = MaterialTheme.typography.titleMedium)
                        Text("Subtle static transitions instead of animated scaling", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                    Switch(
                        checked = isReducedMotion,
                        onCheckedChange = onSetReducedMotion,
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftIvory, checkedTrackColor = SageBrand)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Floating Assistant
            Text(
                text = "FLOATING ASSISTANT",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(10.dp))
            AuraCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Companion Orb", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (hasOverlay) "Persistent botanical orb inspired by Gemini interaction" else "Requires overlay permission to display on top of apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    Switch(
                        checked = isAssistantEnabled && hasOverlay,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (PermissionHelper.isOverlayGranted(context)) {
                                    onSetAssistantEnabled(true)
                                    FloatingAssistantService.start(context)
                                } else {
                                    PermissionHelper.openOverlaySettings(context)
                                }
                            } else {
                                onSetAssistantEnabled(false)
                                FloatingAssistantService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftIvory, checkedTrackColor = SageBrand)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Permission Center & Privacy
            Text(
                text = "PRIVACY & PERMISSIONS",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(10.dp))
            AuraCard(
                onClick = onNavigateToPermissions
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Permission Center", style = MaterialTheme.typography.titleMedium)
                        Text("Inspect microphone, notifications, and overlay status", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                    Icon(painter = painterResource(id = R.drawable.ic_privacy), contentDescription = null, tint = SageBrand)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AuraCard(
                onClick = onResetOnboarding
            ) {
                Text("Replay Onboarding", style = MaterialTheme.typography.titleMedium, color = DeepBotanical)
                Text("Revisit your goals and introduction walkthrough", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About
            AuraCard {
                Text("AURA Sanctuary — v1.0.0", style = MaterialTheme.typography.titleMedium, color = CharcoalText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crafted with quiet intention. Offline-first, private on-device wellness engineered with Jetpack Compose & Material 3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }
    }
}
