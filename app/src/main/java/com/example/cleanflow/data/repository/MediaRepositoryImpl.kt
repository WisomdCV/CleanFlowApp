package com.example.cleanflow.data.repository

import com.example.cleanflow.data.model.MediaFileEntity
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MediaRepositoryImpl(
    private val dataSource: MediaStoreDataSource,
    private val trashRepository: TrashRepository
) : MediaRepository {

    override fun getAllCollections(): Flow<List<MediaCollection>> = 
        combine(
            dataSource.mediaFiles,  // Use cached StateFlow
            trashRepository.trashedIds
        ) { files, trashedIds ->
            // Filter out trashed files
            val activeFiles = files.filter { it.id !in trashedIds }
            
            // Group by bucketName
            val grouped = activeFiles.groupBy { it.bucketName }
            
            grouped.map { (bucketName, bucketFiles) ->
                val totalSize = bucketFiles.sumOf { it.size }
                val count = bucketFiles.size
                val previewUri = bucketFiles.firstOrNull()?.uri ?: ""
                
                MediaCollection(
                    id = bucketName, 
                    displayName = bucketName,
                    fileCount = count,
                    totalSize = totalSize,
                    previewUri = previewUri
                )
            }
        }

    override fun getFilesByCollection(collectionId: String): Flow<List<MediaFile>> = 
        combine(
            dataSource.mediaFiles,  // Use cached StateFlow
            trashRepository.trashedIds
        ) { files, trashedIds ->
            files
                .filter { it.bucketName == collectionId }
                .filter { it.id !in trashedIds }
                .map { mapToDomain(it) }
        }

    private fun mapToDomain(entity: MediaFileEntity): MediaFile {
        val type = if (entity.mimeType.startsWith("video/")) MediaType.VIDEO else MediaType.IMAGE
        return MediaFile(
            id = entity.id,
            uri = entity.uri,
            path = entity.path,
            displayName = entity.displayName,
            size = entity.size,
            dateAdded = entity.dateAdded,
            mimeType = entity.mimeType,
            type = type
        )
    }
    
    override suspend fun deleteFile(uri: String): Boolean {
        return dataSource.deleteFile(uri)
    }
}

