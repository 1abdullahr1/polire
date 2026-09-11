package com.lagradost.cloudstream3.ui.download

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.ItemVideoFolderBinding

class VideoFolderAdapter(
    private val onFolderClick: (VideoFolder) -> Unit
) : ListAdapter<VideoFolder, VideoFolderAdapter.FolderViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<VideoFolder>() {
        override fun areItemsTheSame(oldItem: VideoFolder, newItem: VideoFolder): Boolean {
            return oldItem.bucketId == newItem.bucketId
        }

        override fun areContentsTheSame(oldItem: VideoFolder, newItem: VideoFolder): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemVideoFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(
        private val binding: ItemVideoFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: VideoFolder) {
            binding.folderName.text = folder.name
            val countStr = binding.root.context.getString(R.string.folder_video_count, folder.videoCount)
            binding.folderDetails.text = if (folder.formattedSize.isNotBlank()) {
                "$countStr • ${folder.formattedSize}"
            } else {
                countStr
            }

            val latest = folder.latestVideo
            if (latest != null) {
                binding.folderThumbnail.isVisible = true
                binding.folderIcon.isVisible = false
                VideoThumbnailHelper.loadThumbnail(binding.folderThumbnail, latest)
            } else {
                binding.folderThumbnail.isVisible = false
                binding.folderIcon.isVisible = true
            }

            binding.folderItemRoot.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }
}
