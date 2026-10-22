package com.aura.app.feature.breathing

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.delay

enum class BreathingPhase {
    INHALE,
    HOLD_IN,
    EXHALE,
    HOLD_OUT
}

data class BreathPattern(
    val name: String,
    val inhaleSec: Int,
    val holdInSec: Int,
    val exhaleSec: Int,
    val holdOutSec: Int
)

@Composable
fun BreathingScreen(
    onSaveSession: (String, Long, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val patterns = listOf(
        BreathPattern("Box Breathing", 4, 4, 4, 4),
        BreathPattern("Relaxing (4-7-8)", 4, 7, 8, 0),
        BreathPattern("Equal Breathing", 4, 0, 4, 0)
    )

    var selectedPattern by remember { mutableStateOf(patterns[0]) }
    var isRunning by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var secondsInPhase by remember { mutableStateOf(0) }
    var totalElapsedSeconds by remember { mutableStateOf(0L) }

    // Phase progression loop
    LaunchedEffect(isRunning, selectedPattern) {
        if (!isRunning) return@LaunchedEffect

        while (isRunning) {
            val phaseDuration = when (phase) {
                BreathingPhase.INHALE -> selectedPattern.inhaleSec
                BreathingPhase.HOLD_IN -> selectedPattern.holdInSec
                BreathingPhase.EXHALE -> selectedPattern.exhaleSec
                BreathingPhase.HOLD_OUT -> selectedPattern.holdOutSec
            }

            if (secondsInPhase >= phaseDuration) {
                // Transition to next phase
                phase = when (phase) {
                    BreathingPhase.INHALE -> {
                        triggerVibration(vibrator)
                        if (selectedPattern.holdInSec > 0) BreathingPhase.HOLD_IN else BreathingPhase.EXHALE
                    }
                    BreathingPhase.HOLD_IN -> {
                        triggerVibration(vibrator)
                        BreathingPhase.EXHALE
                    }
                    BreathingPhase.EXHALE -> {
                        triggerVibration(vibrator)
                        if (selectedPattern.holdOutSec > 0) BreathingPhase.HOLD_OUT else BreathingPhase.INHALE
                    }
                    BreathingPhase.HOLD_OUT -> {
                        triggerVibration(vibrator)
                        BreathingPhase.INHALE
                    }
                }
                secondsInPhase = 0
            }

            delay(1000)
            secondsInPhase += 1
            totalElapsedSeconds += 1
        }
    }

    val targetScale = when (phase) {
        BreathingPhase.INHALE -> 1.35f
        BreathingPhase.HOLD_IN -> 1.35f
        BreathingPhase.EXHALE -> 0.85f
        BreathingPhase.HOLD_OUT -> 0.85f
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isRunning) targetScale else 1f,
        animationSpec = tween(durationMillis = 2000),
        label = "breath_scale"
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (totalElapsedSeconds > 10) {
                        onSaveSession(selectedPattern.name, totalElapsedSeconds, true)
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
                    text = "BREATHING",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pattern Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                patterns.forEach { p ->
                    AuraPill(
                        text = p.name,
                        selected = selectedPattern == p,
                        onClick = {
                            selectedPattern = p
                            isRunning = false
                            secondsInPhase = 0
                            phase = BreathingPhase.INHALE
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botanical Expanding Breath Circle
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .scale(animatedScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SageBrand.copy(alpha = 0.15f))
                )
                // Middle tactile ring
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(SageBrand.copy(alpha = 0.35f))
                )
                // Core Circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(DeepBotanical),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isRunning) "Ready" else when (phase) {
                            BreathingPhase.INHALE -> "Inhale"
                            BreathingPhase.HOLD_IN -> "Hold"
                            BreathingPhase.EXHALE -> "Exhale"
                            BreathingPhase.HOLD_OUT -> "Rest"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftIvory,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Elapsed time indicator
            Text(
                text = Formatters.formatTimer(totalElapsedSeconds),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Follow the rhythm gently. Pause anytime if uncomfortable.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            AuraButton(
                text = if (isRunning) "Pause Exercise" else "Begin Breathing",
                onClick = {
                    isRunning = !isRunning
                    if (!isRunning && totalElapsedSeconds > 10) {
                        onSaveSession(selectedPattern.name, totalElapsedSeconds, false)
                    }
                },
                containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant else SageBrand,
                contentColor = if (isRunning) MaterialTheme.colorScheme.onSurfaceVariant else SoftIvory
            )
        }
    }
}

private fun triggerVibration(vibrator: Vibrator?) {
    vibrator?.let { v ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(45)
        }
    }
}
