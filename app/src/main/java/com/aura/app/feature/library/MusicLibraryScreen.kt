package com.aura.app.feature.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.database.PlaylistEntity
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.launch
import java.io.File

enum class LibraryTab(val label: String) {
    ALL("All Music"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favorites"),
    RECENT("Recently Played"),
    IMPORTED("My Imports"),
    AMBIENT("Ambient Sounds")
}

enum class SortOption(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    ARTIST_ASC("Artist"),
    DURATION_DESC("Longest"),
    DATE_DESC("Recently Added")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    allTracks: List<AudioTrackEntity>,
    playlists: List<PlaylistEntity>,
    recentlyPlayed: List<AudioTrackEntity>,
    onPlayTrack: (AudioTrackEntity) -> Unit,
    onPlayQueue: (List<AudioTrackEntity>, Int) -> Unit,
    onAddToQueue: (AudioTrackEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onImportUris: suspend (List<Uri>) -> List<AudioTrackEntity>,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteTrack: (AudioTrackEntity) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(LibraryTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(SortOption.TITLE_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    var trackToAddToPlaylist by remember { mutableStateOf<AudioTrackEntity?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    // SAF Import Launcher for MP3, WAV, M4A, AAC, FLAC, OGG
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isImporting = true
                val imported = onImportUris(uris)
                isImporting = false
                Toast.makeText(
                    context,
                    "Imported ${imported.size} audio track(s)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Filter and Sort tracks
    val filteredTracks = remember(allTracks, selectedTab, searchQuery, selectedSort, recentlyPlayed) {
        val baseList = when (selectedTab) {
            LibraryTab.ALL -> allTracks
            LibraryTab.PLAYLISTS -> emptyList()
            LibraryTab.FAVORITES -> allTracks.filter { it.isFavorite }
            LibraryTab.RECENT -> recentlyPlayed
            LibraryTab.IMPORTED -> allTracks.filter { it.isImported }
            LibraryTab.AMBIENT -> allTracks.filter {
                it.category.contains("Nature", ignoreCase = true) ||
                it.category.contains("Ambient", ignoreCase = true) ||
                it.category.contains("Focus", ignoreCase = true)
            }
        }

        val searched = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedSort) {
            SortOption.TITLE_ASC -> searched.sortedBy { it.title.lowercase() }
            SortOption.ARTIST_ASC -> searched.sortedBy { it.artist.lowercase() }
            SortOption.DURATION_DESC -> searched.sortedByDescending { it.durationMs }
            SortOption.DATE_DESC -> searched.sortedByDescending { it.dateAdded }
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
                                text = "PERSONAL SANCTUARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = StoneNeutral
                            )
                            Text(
                                text = "Music Library",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row {
                        // Import Audio Button
                        FilledTonalButton(
                            onClick = {
                                audioPickerLauncher.launch(
                                    arrayOf(
                                        "audio/*",
                                        "application/ogg"
                                    )
                                )
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SageBrand.copy(alpha = 0.15f),
                                contentColor = DeepBotanical
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar & Sort Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Search tracks or artists...",
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftIvory)
                                .size(50.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_soundscape),
                                contentDescription = "Sort",
                                tint = SageBrand,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(SoftIvory)
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedSort == option) SageBrand else CharcoalText,
                                            fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSort = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tabs Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryTab.values().forEach { tab ->
                        AuraPill(
                            text = tab.label,
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = SageBrand
                )
            }

            if (selectedTab == LibraryTab.PLAYLISTS) {
                // Playlists View
                PlaylistsContentView(
                    playlists = playlists,
                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                    onOpenPlaylist = onOpenPlaylist,
                    onDeletePlaylist = onDeletePlaylist
                )
            } else {
                // Tracks List View
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_music),
                                contentDescription = null,
                                tint = StoneNeutral,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == LibraryTab.IMPORTED) "No imported tracks yet" else "No music found",
                                style = MaterialTheme.typography.titleMedium,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedTab == LibraryTab.IMPORTED)
                                    "Tap 'Import' to bring MP3, WAV, or M4A files from your device."
                                else
                                    "Try adjusting your search query or category filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredTracks.size} tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StoneNeutral
                                )
                                TextButton(onClick = { onPlayQueue(filteredTracks, 0) }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_play),
                                        contentDescription = null,
                                        tint = SageBrand,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play All", color = SageBrand, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        items(filteredTracks, key = { it.id }) { track ->
                            TrackRowCard(
                                track = track,
                                onPlay = { onPlayTrack(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onToggleFavorite = { onToggleFavorite(track.id, !track.isFavorite) },
                                onAddToPlaylist = { trackToAddToPlaylist = track },
                                onDelete = if (track.isImported) { { onDeleteTrack(track) } } else null
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), newPlaylistDesc.trim())
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageBrand)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }

    // Add to Playlist Selection Dialog
    trackToAddToPlaylist?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text("Add to Playlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created yet. Create one first!")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onAddTrackToPlaylist(playlist.id, track.id)
                                        Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                        trackToAddToPlaylist = null
                                    },
                                color = SoftIvory,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }
}

@Composable
fun TrackRowCard(
    track: AudioTrackEntity,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay),
        color = SoftIvory,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Thumbnail
            AuraAlbumArtThumbnail(
                albumArtUri = track.albumArtUri,
                category = track.category,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
                iconSize = 24.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${Formatters.formatTimer(track.durationMs / 1000)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1
                )
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Text(
                    text = if (track.isFavorite) "★" else "☆",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (track.isFavorite) MutedAmber else StoneNeutral
                )
            }

            // More Options Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SoftIvory)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onPlay()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onAddToQueue()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onAddToPlaylist()
                            showMenu = false
                        }
                    )
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete Track", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsContentView(
    playlists: List<PlaylistEntity>,
    onCreatePlaylistClick: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuraCard(
                onClick = onCreatePlaylistClick,
                backgroundColor = SageBrand.copy(alpha = 0.08f),
                borderColor = SageBrand
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SageBrand,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        color = SageBrand,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom playlists yet.\nCreate one to organize your listening rituals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                AuraCard(
                    onClick = { onOpenPlaylist(playlist.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DeepBotanical),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_soundscape),
                                    contentDescription = null,
                                    tint = SoftIvory,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (playlist.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = playlist.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Playlist",
                                tint = SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
