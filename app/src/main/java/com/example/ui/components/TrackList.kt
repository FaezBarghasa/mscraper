package com.example.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextGray

@Composable
fun TrackList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onTrackDismiss: (Track) -> Unit = {},
    favoriteTrackIds: Set<String> = emptySet(),
    onFavoriteClick: (Track) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    onQueueNext: (Track) -> Unit = {},
    onEditClick: (Track) -> Unit = {},
    onAddToPlaylist: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val isFav = favoriteTrackIds.contains(track.id)
            TrackListItem(
                track = track, 
                onClick = { onTrackClick(track) },
                onDismiss = { onTrackDismiss(track) },
                onAddToQueue = { onAddToQueue(track) },
                onQueueNext = { onQueueNext(track) },
                isFavorite = isFav,
                onFavoriteClick = { onFavoriteClick(track) },
                onEditClick = { onEditClick(track) },
                onAddToPlaylist = onAddToPlaylist?.let { { it(track) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListItem(
    track: Track, 
    onClick: () -> Unit, 
    onDismiss: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onQueueNext: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onAddToPlaylist: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
                true
            } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddToQueue()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberCyan.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to Queue", tint = Color.White)
                }
            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Red.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        },
        content = {
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            // Glitch effect animations when pressed
            val infiniteTransition = rememberInfiniteTransition()
            val offsetX by infiniteTransition.animateFloat(
                initialValue = if (isPressed) -2f else 0f,
                targetValue = if (isPressed) 2f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val colorShift by infiniteTransition.animateColor(
                initialValue = if (isPressed) Color(0x33FF00FF) else Color.Transparent,
                targetValue = if (isPressed) Color(0x3300FFFF) else Color.Transparent,
                animationSpec = infiniteRepeatable(
                    animation = tween(100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = offsetX.dp)
                    .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick() 
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorShift)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(track.artist, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                    }
                    Text(track.duration, style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) NeonMagenta else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuExpanded = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(com.example.ui.theme.HoloBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Queue Next", color = Color.White) },
                                onClick = {
                                    onQueueNext()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Queue", color = Color.White) },
                                onClick = {
                                    onAddToQueue()
                                    menuExpanded = false
                                }
                            )
                            if (onAddToPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", color = Color.White) },
                                    onClick = {
                                        onAddToPlaylist()
                                        menuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit Metadata", color = Color.White) },
                                onClick = {
                                    onEditClick()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CyberCyan, modifier = Modifier.size(24.dp))
                }
            }
        }
    )
}
