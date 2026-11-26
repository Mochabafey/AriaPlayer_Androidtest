package com.example.model

import kotlin.math.abs

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translatedText: String? = null
)

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val INLINE_WORD_TIME_REGEX = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    fun parse(
        rawLrc: String?,
        rawTranslatedLrc: String? = null
    ): List<LyricLine> {
        if (rawLrc.isNullOrBlank()) return emptyList()

        val primaryEntries = parseRawEntries(rawLrc)
        if (primaryEntries.isEmpty()) return emptyList()

        val translatedEntries = if (!rawTranslatedLrc.isNullOrBlank()) {
            parseRawEntries(rawTranslatedLrc)
        } else {
            emptyList()
        }

        return primaryEntries.map { (timeMs, text) ->
            val matchedTranslation = findNearestTranslation(timeMs, translatedEntries)
            LyricLine(
                timeMs = timeMs,
                text = text,
                translatedText = matchedTranslation?.takeIf {
                    it.isNotBlank() && !it.equals(text, ignoreCase = true)
                }
            )
        }
    }

    private fun parseRawEntries(raw: String): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        raw.lineSequence().forEach { line ->
            val matches = TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            val cleanText = line
                .replace(TIMESTAMP_REGEX, "")
                .replace(INLINE_WORD_TIME_REGEX, "")
                .trim()

            if (cleanText.isEmpty()) return@forEach

            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val millis = when (fracRaw.length) {
                    0 -> 0L
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    else -> fracRaw.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = minutes * 60_000L + seconds * 1_000L + millis
                result.add(totalMs to cleanText)
            }
        }
        return result.sortedBy { it.first }
    }

    private fun findNearestTranslation(
        targetMs: Long,
        translations: List<Pair<Long, String>>
    ): String? {
        if (translations.isEmpty()) return null
        var best: Pair<Long, String>? = null
        var bestDiff = Long.MAX_VALUE
        for (entry in translations) {
            val diff = abs(entry.first - targetMs)
            if (diff < bestDiff) {
                bestDiff = diff
                best = entry
            }
            if (entry.first > targetMs + 600L) break
        }
        return if (bestDiff <= 500L) best?.second else null
    }

    fun findActiveIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (positionMs < lines.first().timeMs) return 0
        var low = 0
        var high = lines.lastIndex
        var ans = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return ans
    }
}
