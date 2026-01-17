package com.example.cleanflow.domain.usecase

import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.domain.model.SmartDashboardStats
import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSmartStatsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    // Thresholds
    private val LARGE_VIDEO_THRESHOLD = 100 * 1024 * 1024L // 100 MB
    private val LARGE_IMAGE_THRESHOLD = 10 * 1024 * 1024L  // 10 MB
    private val OLD_FILE_DAYS = 30

    operator fun invoke(): Flow<SmartDashboardStats> {
        return mediaRepository.getAllFiles().map { files ->
            calculateStats(files)
        }
    }

    private fun calculateStats(files: List<MediaFile>): SmartDashboardStats {
        val nowSec = System.currentTimeMillis() / 1000
        val oldThresholdSec = nowSec - (OLD_FILE_DAYS * 24 * 60 * 60)

        var largeCount = 0
        var largeSize = 0L
        var oldCount = 0
        var oldSize = 0L
        var videoSize = 0L
        var imageSize = 0L
        var totalSize = 0L

        files.forEach { file ->
            totalSize += file.size
            
            if (file.type == MediaType.VIDEO) {
                videoSize += file.size
                if (file.size > LARGE_VIDEO_THRESHOLD) {
                    largeCount++
                    largeSize += file.size
                }
            } else {
                imageSize += file.size
                if (file.size > LARGE_IMAGE_THRESHOLD) {
                    largeCount++
                    largeSize += file.size
                }
            }

            if (file.dateAdded < oldThresholdSec) {
                oldCount++
                oldSize += file.size
            }
        }

        // Duplicate detection logic (Size + Name)
        val duplicateGroups = files.groupBy { Pair(it.size, it.displayName) }
            .filter { it.value.size > 1 }
        
        var dupGroupsCount = 0
        var dupWastedSize = 0L
        
        duplicateGroups.forEach { (_, groupFiles) ->
            dupGroupsCount++
            // Wasted space is size * (count - 1) - keep one original
            val file = groupFiles.first()
            dupWastedSize += file.size * (groupFiles.size - 1)
        }

        return SmartDashboardStats(
            largeFilesCount = largeCount,
            largeFilesSize = largeSize,
            duplicateGroupsCount = dupGroupsCount,
            duplicateWastedSize = dupWastedSize,
            oldFilesCount = oldCount,
            oldFilesSize = oldSize,
            videoSize = videoSize,
            imageSize = imageSize,
            totalUsedSize = totalSize
        )
    }
}
