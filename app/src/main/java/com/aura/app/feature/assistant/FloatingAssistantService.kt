package com.aura.app.feature.assistant

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aura.app.AuraApplication
import com.aura.app.MainActivity
import com.aura.app.R
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloatingAssistantService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var isExpanded = false

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "aura_floating_assistant"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "action_stop_floating_assistant"

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingAssistantService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingAssistantService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("RtlHardcoded")
    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 350
        }

        overlayView = ComposeView(this).apply {
            setContent {
                AuraTheme {
                    var expanded by remember { mutableStateOf(false) }

                    if (!expanded) {
                        // Collapsed Floating Orb (Gemini / Botanical Orb)
                        Surface(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .clickable {
                                    expanded = true
                                    updateLayoutParamsForExpanded(true, layoutParams)
                                },
                            shape = CircleShape,
                            color = DeepBotanical,
                            border = androidx.compose.foundation.BorderStroke(2.dp, SageBrand),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_aura_mark),
                                    contentDescription = "Aura Assistant",
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } else {
                        // Gemini-Inspired Floating Panel with Ambient Glow
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GlowCyan.copy(alpha = 0.35f), GlowPurple.copy(alpha = 0.35f))
                                    )
                                )
                                .padding(2.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = SoftIvory,
                                shadowElevation = 12.dp
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(SageBrand)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AURA COMPANION",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StoneNeutral
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                expanded = false
                                                updateLayoutParamsForExpanded(false, layoutParams)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = SecondaryText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "How can I support your space?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CharcoalText
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Quick Assistant Actions
                                    AssistantActionButton(
                                        title = "Play Rain Sounds",
                                        subtitle = "Instant calming soundscape",
                                        onClick = {
                                            val app = application as AuraApplication
                                            scope.launch {
                                                val track = app.container.database.audioDao().getTrackById("track_rainfall")
                                                track?.let { app.container.audioPlaybackManager.playTrack(it) }
                                            }
                                            expanded = false
                                            updateLayoutParamsForExpanded(false, layoutParams)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    AssistantActionButton(
                                        title = "5-Min Box Breathing",
                                        subtitle = "Reset your nervous system",
                                        onClick = {
                                            openAppScreen("breathing")
                                            expanded = false
                                            updateLayoutParamsForExpanded(false, layoutParams)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    AssistantActionButton(
                                        title = "Start 25-Min Focus",
                                        subtitle = "Block distractions now",
                                        onClick = {
                                            val app = application as AuraApplication
                                            scope.launch {
                                                val endTime = System.currentTimeMillis() + (25 * 60 * 1000L)
                                                app.container.userPreferencesRepository.setActiveFocusSession(endTime, "Deep Focus")
                                            }
                                            openAppScreen("focus")
                                            expanded = false
                                            updateLayoutParamsForExpanded(false, layoutParams)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    AssistantActionButton(
                                        title = "Record a Spoken Thought",
                                        subtitle = "Quick private voice note",
                                        onClick = {
                                            openAppScreen("journal")
                                            expanded = false
                                            updateLayoutParamsForExpanded(false, layoutParams)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun updateLayoutParamsForExpanded(expanded: Boolean, params: WindowManager.LayoutParams) {
        isExpanded = expanded
        if (expanded) {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        windowManager?.updateViewLayout(overlayView, params)
    }

    private fun openAppScreen(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", route)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Aura Floating Companion",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, FloatingAssistantService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_aura_mark)
            .setContentTitle("AURA Companion Active")
            .setContentText("Tap to open assistant or dismiss")
            .addAction(R.drawable.ic_stop, "Disable Overlay", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // View already detached
            }
            overlayView = null
        }
        super.onDestroy()
    }
}

@Composable
private fun AssistantActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DeepBotanical
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}
