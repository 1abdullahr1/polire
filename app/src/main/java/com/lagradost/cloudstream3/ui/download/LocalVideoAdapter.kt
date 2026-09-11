package com.lagradost.cloudstream3.ui.download

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.databinding.ItemLocalVideoBinding
import com.lagradost.cloudstream3.utils.DataStoreHelper

class LocalVideoAdapter(
    private val onVideoClick: (LocalVideo) -> Unit
) : ListAdapter<LocalVideo, LocalVideoAdapter.VideoViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<LocalVideo>() {
        override fun areItemsTheSame(oldItem: LocalVideo, newItem: LocalVideo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LocalVideo, newItem: LocalVideo): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemLocalVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemLocalVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: LocalVideo) {
            binding.videoTitle.text = video.title
            binding.videoDuration.text = video.formattedDuration

            val details = buildString {
                if (video.formattedSize.isNotBlank()) {
                    append(video.formattedSize)
                }
                if (video.bucketName.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(video.bucketName)
                }
            }
            binding.videoDetails.text = details

            val posDur = DataStoreHelper.getViewPos(video.id.hashCode())
            if (posDur != null && posDur.duration > 0) {
                val progress = ((posDur.position.toDouble() / posDur.duration.toDouble()) * 100).toInt()
                binding.videoWatchProgress.isVisible = progress in 1..99
                binding.videoWatchProgress.progress = progress
            } else {
                binding.videoWatchProgress.isVisible = false
            }

            VideoThumbnailHelper.loadThumbnail(binding.videoThumbnail, video)

            binding.videoItemRoot.setOnClickListener {
                onVideoClick(video)
            }
        }
    }
}
