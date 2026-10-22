package com.aura.app.feature.journal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.VoiceNoteEntity
import com.aura.app.core.designsystem.*
import com.aura.app.core.journal.JournalIntelligence
import com.aura.app.core.recording.AndroidSpeechTranscriptionEngine
import com.aura.app.core.recording.SpeechTranscriptionEngine
import com.aura.app.core.recording.TranscriptSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun JournalDetailScreen(
    note: VoiceNoteEntity?,
    transcriptionEngine: SpeechTranscriptionEngine,
    onUpdateNote: (VoiceNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var editableTitle by remember(note) { mutableStateOf(note?.title ?: "") }
    var editableTranscript by remember(note) { mutableStateOf(note?.transcript ?: "") }
    var editableNotes by remember(note) { mutableStateOf(note?.writtenNotes ?: "") }

    var isTranscribing by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(note?.language ?: "en-US") }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val segments = remember(note?.timestampsJson) {
        AndroidSpeechTranscriptionEngine.parseSegmentsFromJson(note?.timestampsJson ?: "")
    }

    val summary = remember(note?.transcript, note?.writtenNotes) {
        JournalIntelligence.summarizeNote(note?.transcript ?: "", note?.writtenNotes ?: "")
    }

    // Initialize MediaPlayer safely
    DisposableEffect(note) {
        val file = note?.filePath?.let { File(it) }
        if (file != null && file.exists()) {
            val mp = MediaPlayer().apply {
                try {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = 0
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open audio file", Toast.LENGTH_SHORT).show()
                }
            }
            mediaPlayer = mp
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Polling progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPositionMs = mp.currentPosition
                }
            }
            delay(300)
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
                    text = "REFLECTION DETAIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Row {
                    IconButton(onClick = {
                        note?.let { n ->
                            val textToExport = JournalIntelligence.generateExportText(n)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, n.title)
                                putExtra(Intent.EXTRA_TEXT, textToExport)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Reflection"))
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export", tint = SecondaryText)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            note?.let { n ->
                // Editable Title
                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = {
                        editableTitle = it
                        onUpdateNote(n.copy(title = it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = SageBrand,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${Formatters.formatDate(n.timestamp)} • ${Formatters.formatDurationMinutes(n.durationMs)} • Mood: ${n.mood}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                if (n.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "#" + n.tags.split(",").joinToString(" #") { it.trim() },
                        style = MaterialTheme.typography.labelSmall,
                        color = SageBrand
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Audio Playback Card with Speed Controls
                AuraCard(
                    backgroundColor = SoftIvory
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                mediaPlayer?.let { mp ->
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = SageBrand,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = SoftIvory,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val duration = (n.durationMs.coerceAtLeast(1L)).toInt()
                            val progress = (currentPositionMs.toFloat() / duration).coerceIn(0f, 1f)

                            Slider(
                                value = progress,
                                onValueChange = { frac ->
                                    val target = (frac * duration).toInt()
                                    mediaPlayer?.seekTo(target)
                                    currentPositionMs = target
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = SageBrand,
                                    activeTrackColor = SageBrand,
                                    inactiveTrackColor = BorderDivider
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Formatters.formatTimer(currentPositionMs.toLong() / 1000),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                                Text(
                                    text = Formatters.formatTimer(n.durationMs / 1000),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Playback Speed Controls (0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speed:",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                AuraPill(
                                    text = "${speed}x",
                                    selected = currentSpeed == speed,
                                    onClick = {
                                        currentSpeed = speed
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            mediaPlayer?.let { mp ->
                                                try {
                                                    mp.playbackParams = PlaybackParams().apply {
                                                        this.speed = speed
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Automatic Transcription Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTOMATIC TRANSCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = StoneNeutral
                    )

                    // Language Selector Dropdown
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(
                                text = "Lang: $selectedLanguage",
                                style = MaterialTheme.typography.labelSmall,
                                color = SageBrand
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(SoftIvory)
                        ) {
                            AndroidSpeechTranscriptionEngine.SUPPORTED_LANGUAGES.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedLanguage = code
                                        onUpdateNote(n.copy(language = code))
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isTranscribing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = SageBrand, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analyzing speech acoustics and generating alignment...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isTranscribing = true
                                    val file = File(n.filePath)
                                    val result = transcriptionEngine.transcribeAudioFile(file, n.durationMs, selectedLanguage)
                                    isTranscribing = false
                                    if (result.isSuccess) {
                                        editableTranscript = result.fullText
                                        val jsonSegments = AndroidSpeechTranscriptionEngine.segmentsToJson(result.segments)
                                        onUpdateNote(n.copy(transcript = result.fullText, timestampsJson = jsonSegments))
                                        Toast.makeText(context, "Transcription completed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, result.errorMessage ?: "Transcription error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SageBrand),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (editableTranscript.isBlank()) "Transcribe Recording" else "Re-Transcribe")
                        }

                        if (editableTranscript.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", editableTranscript))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Copy TXT", color = SageBrand)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timestamped Segments (Tap to jump audio position!)
                if (segments.isNotEmpty()) {
                    Text(
                        text = "TIMESTAMPS (Tap to jump to position)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StoneNeutral
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        segments.forEach { seg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        mediaPlayer?.seekTo(seg.startMs.toInt())
                                        currentPositionMs = seg.startMs.toInt()
                                        if (mediaPlayer?.isPlaying == false) {
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        }
                                    },
                                color = SoftIvory,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = seg.timestampFormatted,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SageBrand,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = seg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Editable Transcript Field
                OutlinedTextField(
                    value = editableTranscript,
                    onValueChange = {
                        editableTranscript = it
                        onUpdateNote(n.copy(transcript = it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Full Transcript") },
                    placeholder = {
                        Text(
                            "Transcription text will appear here. Tap to edit...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SoftIvory,
                        unfocusedContainerColor = SoftIvory,
                        focusedBorderColor = SageBrand,
                        unfocusedBorderColor = BorderDivider
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Generated transcripts may be imperfect. Always preserved locally on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Written Notes Section
                Text(
                    text = "WRITTEN REFLECTIONS & NOTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableNotes,
                    onValueChange = {
                        editableNotes = it
                        onUpdateNote(n.copy(writtenNotes = it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(18.dp),
                    placeholder = {
                        Text(
                            "Add additional thoughts, questions, or journaling notes alongside this audio...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SoftIvory,
                        unfocusedContainerColor = SoftIvory,
                        focusedBorderColor = SageBrand,
                        unfocusedBorderColor = BorderDivider
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Journal Intelligence (Key Takeaways & Reflection Intention)
                Text(
                    text = "JOURNAL INTELLIGENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                Spacer(modifier = Modifier.height(8.dp))

                AuraCard(
                    backgroundColor = SageBrand.copy(alpha = 0.08f),
                    borderColor = SageBrand
                ) {
                    Text(
                        text = "Key Themes & Takeaways",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepBotanical,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    summary.keyTakeaways.forEach { takeaway ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = SageBrand, fontWeight = FontWeight.Bold)
                            Text(takeaway, style = MaterialTheme.typography.bodyMedium, color = CharcoalText)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderDivider)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Suggested Daily Intention:",
                        style = MaterialTheme.typography.labelMedium,
                        color = StoneNeutral
                    )
                    Text(
                        text = summary.suggestedIntention,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AuraCard {
                    Text(
                        text = "🔒 Stored locally in app-private storage. Recordings and transcripts never leave this device without your explicit export command.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Thought", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Are you sure you want to permanently delete this voice recording?") },
            confirmButton = {
                Button(
                    onClick = {
                        note?.let {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            File(it.filePath).delete()
                            onDeleteNote(it.id)
                        }
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }
}
