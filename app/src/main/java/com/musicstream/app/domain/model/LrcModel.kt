package com.musicstream.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LyricLine(
    val timeMs: Long,
    val text: String
) : Parcelable

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})\]""")

    fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\(from\s+[^)]+\)"""), "")
            .replace(Regex("""(?i)\(official\s+[^)]+\)"""), "")
            .replace(Regex("""(?i)\(feat[^)]+\)"""), "")
            .replace(Regex("""(?i)\(ft[^)]+\)"""), "")
            .replace(Regex("""(?i)\[[^\]]+\]"""), "")
            .replace(Regex("""(?i)- \s*official\s*.*"""), "")
            .replace(Regex("""(?i)320kbps|video|audio|lyric|remix|lofi"""), "")
            .trim()
            .trim('-', '|', ':')
            .trim()
    }

    fun cleanArtist(artist: String): String {
        return artist
            .split(",", "&", "feat", "ft.", "/")
            .firstOrNull()
            ?.trim() ?: artist
    }

    fun parse(lrcContent: String?, durationMs: Long = 0L): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lineText = trimmed.replace(TIMESTAMP_REGEX, "").trim()
                if (lineText.isNotBlank()) {
                    matches.forEach { match ->
                        val (minStr, secStr, msStr) = match.destructured
                        val minutes = minStr.toLongOrNull() ?: 0L
                        val seconds = secStr.toLongOrNull() ?: 0L
                        val millis = if (msStr.length == 2) (msStr.toLongOrNull() ?: 0L) * 10 else msStr.toLongOrNull() ?: 0L
                        val timeMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                        lines.add(LyricLine(timeMs = timeMs, text = lineText))
                    }
                }
            }
        }

        if (lines.isNotEmpty()) {
            return lines.sortedBy { it.timeMs }
        }

        // Fallback: If no timestamps found, parse plain text lines into generated timed lines
        return parsePlainToTimed(lrcContent, durationMs)
    }

    fun parsePlainToTimed(plainContent: String?, totalDurationMs: Long = 180000L): List<LyricLine> {
        if (plainContent.isNullOrBlank()) return emptyList()
        val cleanText = plainContent
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace(Regex("<[^>]*>"), "")

        val rawLines = cleanText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("[") }

        if (rawLines.isEmpty()) return emptyList()

        val totalMs = if (totalDurationMs > 10000L) totalDurationMs else (rawLines.size * 4500L)
        val intervalMs = (totalMs / rawLines.size).coerceIn(2500L, 8000L)

        return rawLines.mapIndexed { index, text ->
            LyricLine(timeMs = index * intervalMs, text = text)
        }
    }
}
