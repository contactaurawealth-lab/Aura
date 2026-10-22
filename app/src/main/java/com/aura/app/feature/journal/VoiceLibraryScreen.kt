package com.aura.app.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.VoiceFolderEntity
import com.aura.app.core.database.VoiceNoteEntity
import com.aura.app.core.designsystem.*
import java.io.File
import java.util.UUID

enum class VoiceViewMode {
    TIMELINE,
    FOLDERS,
    FAVORITES
}

@Composable
fun VoiceLibraryScreen(
    notes: List<VoiceNoteEntity>,
    folders: List<VoiceFolderEntity>,
    onNoteClick: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(VoiceViewMode.TIMELINE) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf<String?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var noteToDelete by remember { mutableStateOf<VoiceNoteEntity?>(null) }

    val filteredNotes = remember(notes, viewMode, selectedFolderId, searchQuery, selectedMoodFilter) {
        notes.filter { note ->
            val matchesView = when (viewMode) {
                VoiceViewMode.TIMELINE -> true
                VoiceViewMode.FOLDERS -> selectedFolderId == null || note.folderId == selectedFolderId
                VoiceViewMode.FAVORITES -> note.isFavorite
            }
            val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.transcript.contains(searchQuery, ignoreCase = true) ||
                note.tags.contains(searchQuery, ignoreCase = true)
            val matchesMood = selectedMoodFilter == null || note.mood.equals(selectedMoodFilter, ignoreCase = true)

            matchesView && matchesSearch && matchesMood
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "PERSONAL VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = StoneNeutral
                            )
                            Text(
                                text = "Voice Library",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    if (viewMode == VoiceViewMode.FOLDERS) {
                        FilledTonalButton(
                            onClick = { showCreateFolderDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SageBrand.copy(alpha = 0.15f),
                                contentColor = DeepBotanical
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Folder",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Folder", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search voice notes, tags, transcripts...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SecondaryText
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = SecondaryText
                                )
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

                Spacer(modifier = Modifier.height(12.dp))

                // View Mode Tabs (Timeline, Folders, Favorites)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraPill(
                        text = "Timeline (${notes.size})",
                        selected = viewMode == VoiceViewMode.TIMELINE,
                        onClick = {
                            viewMode = VoiceViewMode.TIMELINE
                            selectedFolderId = null
                        }
                    )
                    AuraPill(
                        text = "Folders (${folders.size})",
                        selected = viewMode == VoiceViewMode.FOLDERS,
                        onClick = { viewMode = VoiceViewMode.FOLDERS }
                    )
                    AuraPill(
                        text = "Favorites",
                        selected = viewMode == VoiceViewMode.FAVORITES,
                        onClick = {
                            viewMode = VoiceViewMode.FAVORITES
                            selectedFolderId = null
                        }
                    )
                }

                // If Folders Mode: Folder Chips
                if (viewMode == VoiceViewMode.FOLDERS && folders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { selectedFolderId = null },
                            label = { Text("All Folders") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageBrand.copy(alpha = 0.2f),
                                selectedLabelColor = DeepBotanical
                            )
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = { Text(folder.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SageBrand.copy(alpha = 0.2f),
                                    selectedLabelColor = DeepBotanical
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onDeleteFolder(folder.id) },
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Folder",
                                            tint = SecondaryText
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                        text = "No thoughts match this filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record reflections to populate your vault timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (note.isFavorite) {
                                        Text(
                                            text = "★",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MutedAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${Formatters.formatDate(note.timestamp)} • ${Formatters.formatDurationMinutes(note.durationMs)} • ${note.mood}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )

                                if (note.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
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

                            IconButton(onClick = { noteToDelete = note }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = StoneNeutral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageBrand)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }

    // Delete Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Thought", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text("Are you sure you want to permanently delete '${note.title}' and its audio recording?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        File(note.filePath).delete()
                        onDeleteNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }
}
