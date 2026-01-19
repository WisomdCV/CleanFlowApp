package com.example.cleanflow.domain.usecase

import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.domain.model.SmartFilterType
import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * UseCase that returns files filtered by SmartFilterType.
 * Reuses the same thresholds as GetSmartStatsUseCase for consistency.
 */
class GetFilteredFilesUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    companion object {
        private const val LARGE_VIDEO_THRESHOLD = 100 * 1024 * 1024L // 100 MB
        private const val LARGE_IMAGE_THRESHOLD = 10 * 1024 * 1024L  // 10 MB
        private const val OLD_FILE_DAYS = 30
    }

    operator fun invoke(filterType: SmartFilterType): Flow<List<MediaFile>> {
        return repository.getAllFiles().map { files ->
            when (filterType) {
                SmartFilterType.ALL -> files
                SmartFilterType.LARGE_FILES -> filterLargeFiles(files)
                SmartFilterType.OLD_FILES -> filterOldFiles(files)
                SmartFilterType.DUPLICATES -> filterDuplicates(files)
            }
        }
    }

    private fun filterLargeFiles(files: List<MediaFile>): List<MediaFile> {
        return files.filter { file ->
            when (file.type) {
                MediaType.VIDEO -> file.size > LARGE_VIDEO_THRESHOLD
                MediaType.IMAGE -> file.size > LARGE_IMAGE_THRESHOLD
            }
        }.sortedByDescending { it.size }
    }

    private fun filterOldFiles(files: List<MediaFile>): List<MediaFile> {
        val thresholdSeconds = System.currentTimeMillis() / 1000 - (OLD_FILE_DAYS * 24 * 60 * 60)
        return files.filter { it.dateAdded < thresholdSeconds }
            .sortedBy { it.dateAdded }
    }

    private fun filterDuplicates(files: List<MediaFile>): List<MediaFile> {
        // Group by (size, displayName) as proxy for duplicates
        val grouped = files.groupBy { Pair(it.size, it.displayName) }
        
        // Return all files that have duplicates (groups with size > 1)
        return grouped.values
            .filter { it.size > 1 }
            .flatten()
            .sortedBy { it.displayName }
    }
}
