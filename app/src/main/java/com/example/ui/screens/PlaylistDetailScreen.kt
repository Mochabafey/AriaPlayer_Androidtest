package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MediaTrack
import com.example.model.MvItem
import com.example.ui.components.CoverArtwork
import com.example.ui.components.TrackRowItem
import com.example.ui.viewmodel.PlaylistDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    fallbackTitle: String,
    fallbackCover: String,
    uiState: PlaylistDetailUiState,
    currentTrackId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayTrack: (MediaTrack, List<MediaTrack>) -> Unit,
    onPlayMv: (MvItem) -> Unit,
    bottomPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? PlaylistDetailUiState.Success)?.data?.summary?.name ?: fallbackTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("playlist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (uiState) {
            is PlaylistDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PlaylistDetailUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                ) {
                    Spacer(Modifier.height(60.dp))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }

            is PlaylistDetailUiState.Success -> {
                val summary = uiState.data.summary
                val tracks = uiState.data.tracks
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = bottomPadding.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            CoverArtwork(
                                coverUrl = summary.coverUrl.ifBlank { fallbackCover },
                                contentDescription = summary.name,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.size(124.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = summary.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (summary.creatorName.isNotBlank()) {
                                    Text(
                                        text = "创建者：${summary.creatorName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (summary.description.isNotBlank()) {
                                    Text(
                                        text = summary.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Button(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("playlist_play_all_button")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${stringResource(R.string.action_play_all)} (${tracks.size})")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val shuffled = tracks.shuffled()
                                        onPlayTrack(shuffled.first(), shuffled)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("playlist_shuffle_button")
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_shuffle_play))
                                }
                            }
                        }
                    }

                    itemsIndexed(tracks, key = { idx, t -> "${idx}_${t.id}" }) { idx, track ->
                        TrackRowItem(
                            track = track,
                            index = idx,
                            isCurrent = track.id == currentTrackId,
                            onClick = { onPlayTrack(track, tracks) },
                            onPlayMv = if (track.hasMv) {
                                {
                                    onPlayMv(
                                        MvItem(
                                            id = track.mvId,
                                            title = track.title,
                                            artistName = track.artist,
                                            coverUrl = track.coverUrl.orEmpty()
                                        )
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
