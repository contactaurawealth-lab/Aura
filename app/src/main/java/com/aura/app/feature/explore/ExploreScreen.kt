package com.aura.app.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.common.Formatters
import com.aura.app.core.database.AudioTrackEntity
import com.aura.app.core.designsystem.*

@Composable
fun ExploreScreen(
    tracks: List<AudioTrackEntity>,
    onTrackSelected: (AudioTrackEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var onlyGuided by remember { mutableStateOf(false) }

    val categories = listOf("All", "Meditation", "Nature sounds", "Focus", "Sleep")

    val filteredTracks = remember(tracks, searchQuery, selectedCategory, onlyGuided) {
        tracks.filter { track ->
            val matchesCategory = selectedCategory == "All" || track.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.description.contains(searchQuery, ignoreCase = true)
            val matchesGuided = !onlyGuided || track.isGuided
            matchesCategory && matchesSearch && matchesGuided
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = "EXPLORE SANCTUARY",
                style = MaterialTheme.typography.labelSmall,
                color = StoneNeutral
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Practices & Sounds",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search rain, bells, meditation...",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    AuraPill(
                        text = cat,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }
        }

        // List of Tracks
        if (filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_soundscape),
                        contentDescription = null,
                        tint = StoneNeutral,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No practices found",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    AuraCard(
                        onClick = { onTrackSelected(track) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DeepBotanical),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = when (track.category) {
                                            "Meditation" -> R.drawable.ic_meditation
                                            "Sleep" -> R.drawable.ic_sleep
                                            "Focus" -> R.drawable.ic_nav_focus
                                            else -> R.drawable.ic_soundscape
                                        }
                                    ),
                                    contentDescription = track.category,
                                    tint = SoftIvory,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${track.category} • ${track.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }

                            IconButton(
                                onClick = { onToggleFavorite(track.id, !track.isFavorite) }
                            ) {
                                Text(
                                    text = if (track.isFavorite) "★" else "☆",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (track.isFavorite) MutedAmber else StoneNeutral
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
