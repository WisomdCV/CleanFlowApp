package com.example.cleanflow.data.repository

import com.example.cleanflow.data.model.MediaFileEntity
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaRepositoryImpl(
    private val dataSource: MediaStoreDataSource
) : MediaRepository {

    override fun getAllCollections(): Flow<List<MediaCollection>> = flow {
        val files = dataSource.getMediaFiles()
        
        // Group by bucketName
        val grouped = files.groupBy { it.bucketName }
        
        val collections = grouped.map { (bucketName, bucketFiles) ->
            val totalSize = bucketFiles.sumOf { it.size }
            val count = bucketFiles.size
            // Use the first file as preview, or maybe the most recent one (since they are sorted DESC)
            val previewUri = bucketFiles.firstOrNull()?.uri ?: ""
            
            // Generate a stable ID for the collection. 
            // Often bucketId is available in MediaStore, but using name or hashing name is a simple fallback for now.
            // Using bucketName as ID for simplicity as requested "agruparlos basándote en el nombre".
            MediaCollection(
                id = bucketName, 
                displayName = bucketName,
                fileCount = count,
                totalSize = totalSize,
                previewUri = previewUri
            )
        }
        
        emit(collections)
    }.flowOn(Dispatchers.IO)

    override fun getFilesByCollection(collectionId: String): Flow<List<MediaFile>> = flow {
        val allFiles = dataSource.getMediaFiles()
        // Filter by bucketName
        val filtered = allFiles.filter { it.bucketName == collectionId }
        
        val domainFiles = filtered.map { entity ->
            mapToDomain(entity)
        }
        
        emit(domainFiles)
    }.flowOn(Dispatchers.IO)

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
        // We run this on IO dispatcher implicitly by caller or enforce it?
        // DataSource access should be safe, but let's be explicit if complex.
        // Simple call for now.
        return dataSource.deleteFile(uri)
    }
}
