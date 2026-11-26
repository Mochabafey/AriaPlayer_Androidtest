package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DynamicPaletteState
import com.example.model.LyricLine
import com.example.model.formatDuration
import com.example.ui.theme.AriaMonoTimeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    isLoading: Boolean,
    showTranslated: Boolean,
    fontScale: Float,
    palette: DynamicPaletteState,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
        } else if (isUserScrolling) {
            delay(3000L)
            isUserScrolling = false
        }
    }

    LaunchedEffect(activeIndex, isUserScrolling, lyrics.size) {
        if (!isUserScrolling && activeIndex in lyrics.indices) {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val centerOffset = if (viewportHeight > 0) -(viewportHeight / 3) else -220
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = centerOffset
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = palette.accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "正在同步歌词...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedOnContainerColor
                    )
                }
            }

            lyrics.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = palette.mutedOnContainerColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "纯音乐或暂无滚动歌词",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onContainerColor
                    )
                    Text(
                        text = "尽情享受纯净旋律",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.mutedOnContainerColor
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 120.dp, horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.12f to Color.Black,
                                    0.86f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        .testTag("lyrics_list")
                ) {
                    itemsIndexed(
                        items = lyrics,
                        key = { idx, line -> "${idx}_${line.timeMs}" }
                    ) { index, line ->
                        val isActive = index == activeIndex
                        val scale by animateFloatAsState(
                            targetValue = if (isActive) 1.04f else 0.96f,
                            animationSpec = tween(durationMillis = 240),
                            label = "lyricScale"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isActive) 1.0f else 0.44f,
                            animationSpec = tween(durationMillis = 240),
                            label = "lyricAlpha"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    isUserScrolling = false
                                    onSeekToLine(line.timeMs)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .testTag("lyric_line_$index"),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (isUserScrolling) {
                                Text(
                                    text = formatDuration(line.timeMs),
                                    style = AriaMonoTimeStyle,
                                    color = palette.accentColor.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = (22f * fontScale).sp,
                                    lineHeight = (30f * fontScale).sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isActive) palette.onContainerColor else palette.onContainerColor.copy(alpha = 0.85f),
                                textAlign = TextAlign.Start
                            )
                            if (showTranslated && !line.translatedText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = line.translatedText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (15f * fontScale).sp,
                                        lineHeight = (22f * fontScale).sp,
                                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                                    ),
                                    color = if (isActive) palette.accentColor else palette.mutedOnContainerColor
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isUserScrolling && activeIndex in lyrics.indices,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            isUserScrolling = false
                            coroutineScope.launch {
                                val viewportHeight = listState.layoutInfo.viewportSize.height
                                val centerOffset = if (viewportHeight > 0) -(viewportHeight / 3) else -220
                                listState.animateScrollToItem(activeIndex, centerOffset)
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.accentColor,
                            contentColor = palette.onAccentColor
                        ),
                        modifier = Modifier.testTag("lyric_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "定位当前歌词",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("定位当前句")
                    }
                }
            }
        }
    }
}
