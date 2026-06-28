package com.example.core

import com.example.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data class ProgressOverlayState(
    val isVisible: Boolean = false,
    val mainMessage: String = "",
    val subMessage: String = "",
    val progress: Float = 0f,
    val activeTaskCount: Int = 0
)

class ProgressManager(
    private val libraryScanner: LibraryScanner,
    private val downloadManager: DownloadManager,
    private val scope: CoroutineScope
) {
    private val _overlayState = MutableStateFlow(ProgressOverlayState())
    val overlayState: StateFlow<ProgressOverlayState> = _overlayState.asStateFlow()

    init {
        // Observe and combine flows from both libraryScanner and downloadManager
        combine(
            libraryScanner.isScanning,
            libraryScanner.scanProgress,
            libraryScanner.scannedFileCount,
            downloadManager.jobs
        ) { isScanning, scanProgress, scannedCount, downloadJobs ->
            val downloadingJobs = downloadJobs.filter { 
                it.status == DownloadStatus.DOWNLOADING || 
                it.status == DownloadStatus.CONVERTING 
            }
            val pendingJobsCount = downloadJobs.count { it.status == DownloadStatus.PENDING }
            val activeDownloadsCount = downloadingJobs.size
            
            val isVisible = isScanning || activeDownloadsCount > 0 || pendingJobsCount > 0
            
            var mainMessage = ""
            var subMessage = ""
            var progress = 0f
            var activeTaskCount = 0

            if (isScanning) {
                activeTaskCount++
                mainMessage = "SCANNING SYSTEM SOUNDWAVES"
                subMessage = "Indexing track $scannedCount | ${(scanProgress * 100).toInt()}%"
                progress = scanProgress
            }

            if (activeDownloadsCount > 0 || pendingJobsCount > 0) {
                activeTaskCount += (activeDownloadsCount + pendingJobsCount)
                if (mainMessage.isEmpty()) {
                    mainMessage = "GRID NETWORK DOWNLOAD ACTIVE"
                    val activeJob = downloadingJobs.firstOrNull()
                    if (activeJob != null) {
                        subMessage = "Muxing '${activeJob.title}' | ${(activeJob.progress * 100).toInt()}%"
                        progress = activeJob.progress
                    } else {
                        subMessage = "$pendingJobsCount tracks queued in FIFO buffer"
                        progress = 0f
                    }
                } else {
                    mainMessage = "CYBERNETIC CO-PROCESSING ACTIVE"
                    val downloadProgress = downloadingJobs.map { it.progress }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                    progress = (scanProgress + downloadProgress) / 2f
                    subMessage = "Scanner active & $activeDownloadsCount parallel downloads processing"
                }
            }

            ProgressOverlayState(
                isVisible = isVisible,
                mainMessage = mainMessage,
                subMessage = subMessage,
                progress = progress,
                activeTaskCount = activeTaskCount
            )
        }.onEach { state ->
            _overlayState.value = state
        }.launchIn(scope)
    }
}
