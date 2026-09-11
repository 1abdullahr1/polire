package com.lagradost.cloudstream3.ui.download

import android.net.Uri

data class LocalVideo(
    val id: Long,
    val uri: Uri,
    val title: String,
    val path: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModified: Long,
    val bucketId: String,
    val bucketName: String,
    val width: Int = 0,
    val height: Int = 0,
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

data class VideoFolder(
    val bucketId: String,
    val name: String,
    val videos: List<LocalVideo>,
) {
    val videoCount: Int get() = videos.size
    val totalSizeBytes: Long get() = videos.sumOf { it.sizeBytes }
    val latestVideo: LocalVideo? get() = videos.firstOrNull()

    val formattedSize: String
        get() {
            val bytes = totalSizeBytes
            if (bytes <= 0) return ""
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
