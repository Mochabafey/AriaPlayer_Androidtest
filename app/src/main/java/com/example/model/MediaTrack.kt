package com.example.model

enum class MediaSourceType {
    NETEASE,
    LOCAL,
    DIRECT_URL
}

enum class MediaKind {
    AUDIO,
    VIDEO
}

enum class AudioQuality(val level: String, val label: String) {
    STANDARD("standard", "标准 128k"),
    EXHIGH("exhigh", "极高 320k"),
    LOSSLESS("lossless", "无损 FLAC"),
    HIRES("hires", "Hi-Res")
}

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String? = null,
    val streamUrl: String? = null,
    val mvId: Long = 0L,
    val durationMs: Long = 0L,
    val sourceType: MediaSourceType = MediaSourceType.NETEASE,
    val mediaKind: MediaKind = MediaKind.AUDIO,
    val isLiked: Boolean = false,
    val qualityLabel: String = ""
) {
    val hasMv: Boolean get() = mvId > 0L
    val isVideo: Boolean get() = mediaKind == MediaKind.VIDEO
}

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val description: String = "",
    val trackCount: Int = 0,
    val playCount: Long = 0L,
    val creatorName: String = ""
)

data class MvItem(
    val id: Long,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val durationMs: Long = 0L,
    val playCount: Long = 0L
)

data class NeteaseUser(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
    val signature: String = "",
    val vipType: Int = 0
)

enum class RepeatModeOption {
    ALL,
    ONE,
    OFF
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatPlayCount(count: Long): String {
    return when {
        count >= 100_000_000L -> String.format("%.1f亿", count / 100_000_000.0)
        count >= 10_000L -> String.format("%.1f万", count / 10_000.0)
        else -> count.toString()
    }
}
