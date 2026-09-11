package com.lagradost.cloudstream3.ui.download

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoRepository {

    suspend fun getAllVideos(context: Context): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<LocalVideo>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketIdCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durationCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateModCol = it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val rawName = it.getString(nameCol) ?: "Video"
                    val path = if (dataCol != -1) it.getString(dataCol) else null
                    val duration = if (durationCol != -1) it.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) it.getLong(sizeCol) else 0L
                    val dateModified = if (dateModCol != -1) it.getLong(dateModCol) else 0L
                    val width = if (widthCol != -1) it.getInt(widthCol) else 0
                    val height = if (heightCol != -1) it.getInt(heightCol) else 0

                    val rawBucketId = if (bucketIdCol != -1) it.getString(bucketIdCol) else null
                    val rawBucketName = if (bucketNameCol != -1) it.getString(bucketNameCol) else null

                    val folderName = when {
                        !rawBucketName.isNullOrBlank() -> rawBucketName
                        !path.isNullOrBlank() -> File(path).parentFile?.name ?: "Videos"
                        else -> "Videos"
                    }
                    val bucketId = rawBucketId ?: folderName.hashCode().toString()

                    videoList.add(
                        LocalVideo(
                            id = id,
                            uri = contentUri,
                            title = rawName,
                            path = path,
                            durationMs = duration,
                            sizeBytes = size,
                            dateModified = dateModified,
                            bucketId = bucketId,
                            bucketName = folderName,
                            width = width,
                            height = height,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    suspend fun getFolders(context: Context): List<VideoFolder> = withContext(Dispatchers.IO) {
        val videos = getAllVideos(context)
        videos.groupBy { it.bucketId }
            .map { (bucketId, folderVideos) ->
                val folderName = folderVideos.firstOrNull()?.bucketName ?: "Videos"
                VideoFolder(
                    bucketId = bucketId,
                    name = folderName,
                    videos = folderVideos
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
