package com.aura.app.feature.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.app.core.designsystem.*
import com.aura.app.core.permissions.PermissionHelper
import com.aura.app.feature.appblocker.AppBlockerAccessibilityService

@Composable
fun PermissionCenterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var isMicGranted by remember { mutableStateOf(PermissionHelper.isMicrophoneGranted(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.isNotificationGranted(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionHelper.isOverlayGranted(context)) }
    var isUsageGranted by remember { mutableStateOf(PermissionHelper.isUsageStatsGranted(context)) }
    var isAccessibilityGranted by remember {
        mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java))
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        isMicGranted = PermissionHelper.isMicrophoneGranted(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        isNotificationGranted = PermissionHelper.isNotificationGranted(context)
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
                    text = "PERMISSION CENTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "System Permissions",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Transparent overview of Android permissions used by Aura. Denying any permission never breaks unrelated features.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Microphone
            PermissionItem(
                title = "Microphone",
                description = "Enables private spoken reflection recording in your Voice Journal. Audio never leaves device storage.",
                isGranted = isMicGranted,
                actionLabel = "Grant Microphone",
                onAction = {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Notifications
            PermissionItem(
                title = "Notifications",
                description = "Shows media playback controls on lock screen and triggers gentle daily meditation reminders.",
                isGranted = isNotificationGranted,
                actionLabel = "Grant Notifications",
                onAction = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionHelper.openAppSettings(context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Floating Overlay
            PermissionItem(
                title = "Display Over Other Apps",
                description = "Allows the Gemini-inspired floating companion orb to hover unobtrusively for instant practice access.",
                isGranted = isOverlayGranted,
                actionLabel = "Open Overlay Settings",
                onAction = { PermissionHelper.openOverlaySettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Usage Access
            PermissionItem(
                title = "App Usage Stats",
                description = "Used to detect when you launch apps you explicitly designated to restrict during focus sessions.",
                isGranted = isUsageGranted,
                actionLabel = "Open Usage Access",
                onAction = { PermissionHelper.openUsageAccessSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Accessibility Service
            PermissionItem(
                title = "Focus Interception Service",
                description = "Provides real-time gentle pausing when restricted apps are opened while a focus timer is active.",
                isGranted = isAccessibilityGranted,
                actionLabel = "Open Accessibility Settings",
                onAction = { PermissionHelper.openAccessibilitySettings(context) }
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    AuraCard(
        backgroundColor = SoftIvory,
        borderColor = if (isGranted) SageBrand.copy(alpha = 0.5f) else BorderDivider
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGranted) SageBrand.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isGranted) "Granted" else "Missing",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isGranted) SageBrand else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            AuraOutlinedButton(
                text = actionLabel,
                onClick = onAction
            )
        }
    }
}
