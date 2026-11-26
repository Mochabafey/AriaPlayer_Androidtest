package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.model.MediaTrack
import com.example.model.MvItem
import com.example.model.PlaylistSummary
import com.example.ui.components.CoverArtwork
import com.example.ui.components.TrackRowItem

private enum class LibraryTab(val label: String) {
    FAVORITES("我喜欢"),
    LOCAL("本地与直链"),
    HISTORY("最近播放"),
    PLAYLISTS("云端歌单")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    favorites: List<MediaTrack>,
    localMedia: List<MediaTrack>,
    history: List<MediaTrack>,
    userPlaylists: List<PlaylistSummary>,
    currentTrackId: String?,
    onPlayTrack: (MediaTrack, List<MediaTrack>) -> Unit,
    onPlayMv: (MvItem) -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onPickLocalAudio: () -> Unit,
    onPickLocalVideo: () -> Unit,
    onOpenStreamUrlDialog: () -> Unit,
    onClearHistory: () -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.FAVORITES) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = 12.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "我的曲库",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "收藏曲目、本地音视频文件与网易云个人歌单",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickLocalAudio,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_pick_audio_button")
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("本地音频")
                }
                FilledTonalButton(
                    onClick = onPickLocalVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_pick_video_button")
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("本地视频")
                }
                FilledTonalButton(
                    onClick = onOpenStreamUrlDialog,
                    modifier = Modifier.testTag("library_stream_url_button")
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = "网络直链", modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                LibraryTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedTab) {
            LibraryTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = "还没有收藏曲目",
                            subtitle = "在播放页点击红心图标即可将歌曲加入此处"
                        )
                    }
                } else {
                    items(favorites, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            onClick = { onPlayTrack(track, favorites) },
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

            LibraryTab.LOCAL -> {
                if (localMedia.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Rounded.AudioFile,
                            title = "尚未导入本地媒体",
                            subtitle = "使用上方按钮从手机文件或相册选择音频、视频，无需繁琐存储权限"
                        )
                    }
                } else {
                    items(localMedia, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            onClick = { onPlayTrack(track, localMedia) }
                        )
                    }
                }
            }

            LibraryTab.HISTORY -> {
                if (history.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Rounded.History,
                            title = "暂无播放记录",
                            subtitle = "播放过的歌曲与 MV 会自动保存在这里"
                        )
                    }
                } else {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = onClearHistory) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("清空历史")
                            }
                        }
                    }
                    items(history, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            onClick = { onPlayTrack(track, history) },
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

            LibraryTab.PLAYLISTS -> {
                if (userPlaylists.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Rounded.LibraryMusic,
                            title = "暂无同步歌单",
                            subtitle = "在“账户”页面配置 api-NetEasyCloud 并登录后即可同步您的网易云歌单"
                        )
                    }
                } else {
                    items(userPlaylists, key = { it.id }) { playlist ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenPlaylist(playlist) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                CoverArtwork(
                                    coverUrl = playlist.coverUrl,
                                    contentDescription = playlist.name,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.trackCount} 首曲目",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
