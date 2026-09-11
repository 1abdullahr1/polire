package com.lagradost.cloudstream3.ui.download

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import android.widget.ImageView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.main

object VideoThumbnailHelper {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<Long, Bitmap>(cacheSize) {
        override fun sizeOf(key: Long, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun loadThumbnail(imageView: ImageView, video: LocalVideo) {
        val cached = memoryCache.get(video.id)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        imageView.setImageResource(R.drawable.outline_round_gray)
        val context = imageView.context.applicationContext
        val tag = video.id
        imageView.tag = tag

        ioSafe {
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(video.uri, Size(320, 180), null)
                } else if (!video.path.isNullOrBlank()) {
                    ThumbnailUtils.createVideoThumbnail(video.path, MediaStore.Images.Thumbnails.MINI_KIND)
                } else {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        video.id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (_: Throwable) {
                null
            }

            if (bitmap != null) {
                memoryCache.put(video.id, bitmap)
                main {
                    if (imageView.tag == tag) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}
