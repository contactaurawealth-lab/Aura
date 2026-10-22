package com.aura.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.database.PlaylistEntity
import com.aura.app.core.designsystem.*

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity?,
    tracks: List<AudioTrackEntity>,
    availableTracks: List<AudioTrackEntity>,
    onPlayTrack: (AudioTrackEntity) -> Unit,
    onPlayQueue: (List<AudioTrackEntity>, Int) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onAddTrackToPlaylist: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddTracksDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
                    text = "PLAYLIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )

                IconButton(onClick = { showAddTracksDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Tracks",
                        tint = SageBrand
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description
            Text(
                text = playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!playlist?.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlist!!.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Play All & Shuffle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onPlayQueue(tracks, 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SageBrand),
                    shape = RoundedCornerShape(16.dp),
                    enabled = tracks.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All (${tracks.size})")
                }

                OutlinedButton(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onPlayQueue(tracks.shuffled(), 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    enabled = tracks.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track List
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "This playlist is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAddTracksDialog = true }) {
                            Text("Add Tracks", color = SageBrand)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        AuraCard(
                            onClick = { onPlayTrack(track) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${track.artist} • ${Formatters.formatTimer(track.durationMs / 1000)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText
                                    )
                                }

                                playlist?.let { p ->
                                    IconButton(
                                        onClick = { onRemoveTrackFromPlaylist(p.id, track.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
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
        }
    }

    // Add Tracks Dialog
    if (showAddTracksDialog && playlist != null) {
        val tracksToAdd = availableTracks.filter { avail -> tracks.none { it.id == avail.id } }

        AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            title = { Text("Add Tracks to Playlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                if (tracksToAdd.isEmpty()) {
                    Text("All available tracks are already in this playlist.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tracksToAdd) { track ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = SoftIvory,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.titleSmall)
                                        Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                                    }
                                    IconButton(onClick = {
                                        onAddTrackToPlaylist(playlist.id, track.id)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = SageBrand
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksDialog = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SoftIvory
        )
    }
}
