package com.aura.app.feature.journal

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.aura.app.core.database.VoiceFolderEntity
import com.aura.app.core.database.VoiceNoteEntity
import com.aura.app.core.designsystem.*
import com.aura.app.core.permissions.PermissionHelper
import com.aura.app.core.recording.RecordingState
import com.aura.app.core.recording.VoiceRecorder
import java.io.File
import java.util.UUID

@Composable
fun VoiceJournalScreen(
    notes: List<VoiceNoteEntity>,
    folders: List<VoiceFolderEntity>,
    recorder: VoiceRecorder,
    onSaveNote: (VoiceNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val recordingState by recorder.recordingState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastRecordedFile by remember { mutableStateOf<File?>(null) }
    var lastRecordedDuration by remember { mutableStateOf(0L) }

    var noteTitleInput by remember { mutableStateOf("") }
    var noteMoodInput by remember { mutableStateOf("Reflective") }
    var noteTagsInput by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recorder.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes.take(8) // Recent 8 in main journal view
        } else {
            notes.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.transcript.contains(searchQuery, ignoreCase = true) ||
                note.tags.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VOICE JOURNAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = StoneNeutral
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Capture a thought",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Quick Link to Full Voice Library
                TextButton(onClick = onNavigateToLibrary) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder),
                        contentDescription = null,
                        tint = SageBrand,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vault",
                        color = SageBrand,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search notes, tags, or transcripts...", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SecondaryText)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SecondaryText)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SoftIvory,
                    unfocusedContainerColor = SoftIvory,
                    focusedBorderColor = SageBrand,
                    unfocusedBorderColor = BorderDivider
                ),
                singleLine = true
            )
        }

        // Active Recording Panel (Recording or Paused)
        if (recordingState is RecordingState.Recording || recordingState is RecordingState.Paused) {
            val isPaused = recordingState is RecordingState.Paused
            val durationMs = if (isPaused) {
                (recordingState as RecordingState.Paused).durationMs
            } else {
                (recordingState as RecordingState.Recording).durationMs
            }
            val amplitudes = if (isPaused) {
                (recordingState as RecordingState.Paused).amplitudes
            } else {
                (recordingState as RecordingState.Recording).amplitudes
            }

            AuraCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                backgroundColor = SoftIvory,
                borderColor = SageBrand
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) StoneNeutral else MutedAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPaused) "Paused: ${Formatters.formatTimer(durationMs / 1000)}" else "Recording: ${Formatters.formatTimer(durationMs / 1000)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CharcoalText
                        )
                    }

                    IconButton(onClick = { recorder.cancelRecording() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Discard", tint = SecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AuraWaveformCanvas(
                    amplitudes = amplitudes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pause / Resume Button
                    OutlinedButton(
                        onClick = {
                            if (isPaused) recorder.resumeRecording() else recorder.pauseRecording()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = if (isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SageBrand
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPaused) "Resume" else "Pause", color = SageBrand)
                    }

                    // Done Recording Button
                    Button(
                        onClick = {
                            val file = recorder.stopRecording()
                            if (file != null) {
                                lastRecordedFile = file
                                lastRecordedDuration = durationMs
                                noteTitleInput = "Reflection on ${Formatters.formatDate(System.currentTimeMillis())}"
                                showSaveDialog = true
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = SageBrand),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done Recording")
                    }
                }
            }
        }

        // Recent Notes List
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_journal),
                        contentDescription = null,
                        tint = StoneNeutral,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No thoughts recorded yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the record button below to speak freely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    AuraCard(
                        onClick = { onNoteClick(note.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatDate(note.timestamp)} • ${Formatters.formatDurationMinutes(note.durationMs)} • ${note.mood}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                                if (note.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "#" + note.tags.split(",").joinToString(" #") { it.trim() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SageBrand
                                    )
                                }
                                if (note.transcript.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = note.transcript,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SecondaryText,
                                        maxLines = 2
                                    )
                                }
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

        // Floating Record Bar at Bottom
        if (recordingState !is RecordingState.Recording && recordingState !is RecordingState.Paused) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(24.dp),
                color = SoftIvory,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Spoken Reflection",
                            style = MaterialTheme.typography.titleMedium,
                            color = CharcoalText
                        )
                        Text(
                            text = "Private on-device recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }

                    Surface(
                        onClick = {
                            if (PermissionHelper.isMicrophoneGranted(context)) {
                                recorder.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = CircleShape,
                        color = SageBrand,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_soundscape),
                                contentDescription = "Record",
                                tint = SoftIvory,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Save Note Dialog
    if (showSaveDialog && lastRecordedFile != null) {
        val file = lastRecordedFile!!
        AlertDialog(
            onDismissRequest = {
                file.delete()
                showSaveDialog = false
            },
            title = { Text("Save Thought", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteTagsInput,
                        onValueChange = { noteTagsInput = it },
                        label = { Text("Tags (comma separated, e.g. morning, calm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Mood:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Calm", "Reflective", "Unfocused", "Grateful").forEach { mood ->
                            AuraPill(
                                text = mood,
                                selected = noteMoodInput == mood,
                                onClick = { noteMoodInput = mood }
                            )
                        }
                    }

                    if (folders.isNotEmpty()) {
                        Text("Folder (Optional):", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AuraPill(
                                text = "None",
                                selected = selectedFolderId == null,
                                onClick = { selectedFolderId = null }
                            )
                            folders.forEach { f ->
                                AuraPill(
                                    text = f.name,
                                    selected = selectedFolderId == f.id,
                                    onClick = { selectedFolderId = f.id }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newNote = VoiceNoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = noteTitleInput.ifBlank { "Untitled Note" },
                            filePath = file.absolutePath,
                            durationMs = lastRecordedDuration,
                            timestamp = System.currentTimeMillis(),
                            mood = noteMoodInput,
                            tags = noteTagsInput.trim(),
                            folderId = selectedFolderId
                        )
                        onSaveNote(newNote)
                        recorder.resetState()
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageBrand)
                ) {
                    Text("Save to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    file.delete()
                    recorder.resetState()
                    showSaveDialog = false
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }
}
