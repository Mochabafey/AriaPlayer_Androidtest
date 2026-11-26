package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MediaTrack
import com.example.model.MvItem
import com.example.model.formatDuration
import com.example.model.formatPlayCount
import com.example.ui.components.CoverArtwork
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.AriaMonoTimeStyle

@Composable
fun VideosScreen(
    featuredMvs: List<MvItem>,
    localVideos: List<MediaTrack>,
    isLoading: Boolean,
    isServerConfigured: Boolean,
    currentTrackId: String?,
    onPlayMv: (MvItem) -> Unit,
    onPlayLocalVideo: (MediaTrack) -> Unit,
    onPickLocalVideo: () -> Unit,
    onOpenVideoUrlDialog: () -> Unit,
    onOpenAccount: () -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 12.dp,
            start = 20.dp,
            end = 20.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MV 与视频",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "支持网易云高清 MV、本地视频文件与网络直链流媒体播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onPickLocalVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("videos_pick_local_button")
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("导入本地视频")
                }
                FilledTonalButton(
                    onClick = onOpenVideoUrlDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("videos_stream_url_button")
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("在线视频直链")
                }
            }
        }

        if (!isServerConfigured) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "连接 api-NetEasyCloud 解锁在线 MV 播放",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "本地视频与网络 MP4/HLS 直链无需服务端即可播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onOpenAccount) {
                            Text("配置")
                        }
                    }
                }
            }
        }

        if (localVideos.isNotEmpty()) {
            item {
                Text(
                    text = "我的本地与直链视频",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(localVideos, key = { it.id }) { video ->
                TrackRowItem(
                    track = video,
                    isCurrent = video.id == currentTrackId,
                    onClick = { onPlayLocalVideo(video) }
                )
            }
        }

        item {
            Text(
                text = "热门 MV 精选",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (isLoading && featuredMvs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (featuredMvs.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "暂无在线 MV 列表",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "您也可以直接导入手机相册视频或输入 HLS/MP4 直链观看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(featuredMvs, key = { it.id }) { mv ->
                MvFeedCard(
                    mv = mv,
                    onClick = { onPlayMv(mv) }
                )
            }
        }
    }
}

@Composable
private fun MvFeedCard(
    mv: MvItem,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("mv_feed_card_${mv.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                CoverArtwork(
                    coverUrl = mv.coverUrl,
                    contentDescription = mv.title,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "播放 MV",
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                if (mv.durationMs > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = formatDuration(mv.durationMs),
                            style = AriaMonoTimeStyle,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mv.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mv.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (mv.playCount > 0L) {
                    Text(
                        text = "${formatPlayCount(mv.playCount)} 次播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
