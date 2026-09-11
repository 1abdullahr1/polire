package com.lagradost.cloudstream3.ui.download

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentDownloadsBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.player.OfflinePlaybackHelper
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.attachBackPressedCallback
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.detachBackPressedCallback
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.hideKeyboard
import com.lagradost.cloudstream3.utils.UIHelper.setAppBarNoScrollFlagsOnTV

const val DOWNLOAD_NAVIGATE_TO = "downloadpage"

class DownloadFragment : BaseFragment<FragmentDownloadsBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentDownloadsBinding::inflate)
) {

    companion object {
        const val TAB_ALL_VIDEOS = 0
        const val TAB_FOLDERS = 1
    }

    private var allVideos: List<LocalVideo> = emptyList()
    private var allFolders: List<VideoFolder> = emptyList()
    private var currentFolder: VideoFolder? = null
    private var currentTab: Int = TAB_ALL_VIDEOS

    private val videoAdapter = LocalVideoAdapter { video ->
        activity?.let { act ->
            OfflinePlaybackHelper.playLocalVideo(act, video)
        }
    }

    private val folderAdapter = VideoFolderAdapter { folder ->
        openFolder(folder)
    }

    private fun onStoragePermissionChanged(granted: Boolean) {
        if (granted) {
            loadVideos(showLoading = true)
        }
    }

    override fun onDestroyView() {
        MainActivity.storagePermissionEvent -= ::onStoragePermissionChanged
        activity?.detachBackPressedCallback("Videos")
        super.onDestroyView()
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onResume() {
        super.onResume()
        loadVideos(showLoading = false)
    }

    override fun onBindingCreated(binding: FragmentDownloadsBinding) {
        hideKeyboard()
        binding.downloadAppbar.setAppBarNoScrollFlagsOnTV()

        binding.downloadList.layoutManager = LinearLayoutManager(context)
        binding.downloadList.adapter = videoAdapter

        binding.folderList.layoutManager = LinearLayoutManager(context)
        binding.folderList.adapter = folderAdapter

        binding.videoTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: TAB_ALL_VIDEOS
                updateView()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnRescan.setOnClickListener {
            loadVideos(showLoading = true)
        }
        binding.btnEmptyRescan.setOnClickListener {
            loadVideos(showLoading = true)
        }

        binding.btnGrantPermission.setOnClickListener {
            (activity as? MainActivity)?.checkAndRequestStoragePermission()
        }

        binding.btnBack.setOnClickListener {
            exitFolderView()
        }

        activity?.attachBackPressedCallback("Videos") {
            if (currentFolder != null) {
                exitFolderView()
            } else {
                runDefault()
            }
        }

        MainActivity.storagePermissionEvent += ::onStoragePermissionChanged

        loadVideos(showLoading = true)
    }

    private fun openFolder(folder: VideoFolder) {
        currentFolder = folder
        binding?.apply {
            btnBack.isVisible = true
            videoTabLayout.isVisible = false
            titleText.text = folder.name
            downloadList.isVisible = true
            folderList.isVisible = false
            emptyState.isVisible = folder.videos.isEmpty()
            videoAdapter.submitList(folder.videos)
        }
    }

    private fun exitFolderView() {
        currentFolder = null
        binding?.apply {
            btnBack.isVisible = false
            videoTabLayout.isVisible = true
            titleText.setText(R.string.title_videos)
            updateView()
        }
    }

    private fun updateView() {
        val b = binding ?: return
        if (currentFolder != null) {
            b.btnBack.isVisible = true
            b.videoTabLayout.isVisible = false
            b.titleText.text = currentFolder?.name ?: getString(R.string.title_videos)
            b.downloadList.isVisible = true
            b.folderList.isVisible = false
            b.emptyState.isVisible = currentFolder?.videos?.isEmpty() == true
            videoAdapter.submitList(currentFolder?.videos ?: emptyList())
            return
        }

        b.btnBack.isVisible = false
        b.videoTabLayout.isVisible = true
        b.titleText.setText(R.string.title_videos)

        if (currentTab == TAB_ALL_VIDEOS) {
            b.downloadList.isVisible = true
            b.folderList.isVisible = false
            b.emptyState.isVisible = allVideos.isEmpty()
            videoAdapter.submitList(allVideos)
        } else {
            b.downloadList.isVisible = false
            b.folderList.isVisible = true
            b.emptyState.isVisible = allFolders.isEmpty()
            folderAdapter.submitList(allFolders)
        }
    }

    private fun loadVideos(showLoading: Boolean = true) {
        val ctx = context ?: return
        val b = binding ?: return

        if (!MainActivity.hasStoragePermission(ctx)) {
            b.permissionState.isVisible = true
            b.downloadLoading.isVisible = false
            b.downloadList.isVisible = false
            b.folderList.isVisible = false
            b.emptyState.isVisible = false
            return
        }

        b.permissionState.isVisible = false
        if (showLoading) {
            b.downloadLoading.isVisible = true
        }

        ioSafe {
            val videos = VideoRepository.getAllVideos(ctx)
            val folders = VideoRepository.getFolders(ctx)

            main {
                val currentBinding = binding ?: return@main
                currentBinding.downloadLoading.isVisible = false
                allVideos = videos
                allFolders = folders

                if (currentFolder != null) {
                    val updatedFolder = folders.firstOrNull { it.bucketId == currentFolder?.bucketId }
                    if (updatedFolder != null) {
                        currentFolder = updatedFolder
                    } else {
                        currentFolder = null
                    }
                }
                updateView()
            }
        }
    }
}