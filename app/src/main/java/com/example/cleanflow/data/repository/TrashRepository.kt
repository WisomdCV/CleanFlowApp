package com.example.cleanflow.data.repository

import com.example.cleanflow.data.local.TrashDao
import com.example.cleanflow.data.local.TrashedFileEntity
import com.example.cleanflow.domain.model.MediaFile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing trashed files.
 */
class TrashRepository(private val trashDao: TrashDao) {
    
    val trashedFiles: Flow<List<TrashedFileEntity>> = trashDao.getAllTrashed()
    
    val trashedIds: Flow<List<Long>> = trashDao.getTrashedIds()
    
    val trashCount: Flow<Int> = trashDao.getTrashCount()
    
    suspend fun addToTrash(file: MediaFile, collectionId: String) {
        val entity = TrashedFileEntity(
            mediaId = file.id,
            uri = file.uri,
            displayName = file.displayName,
            size = file.size,
            mimeType = file.mimeType,
            collectionId = collectionId
        )
        trashDao.addToTrash(entity)
    }
    
    suspend fun removeFromTrash(id: Long) {
        trashDao.removeFromTrash(id)
    }
    
    suspend fun clearTrash() {
        trashDao.clearTrash()
    }
    
    suspend fun getTrashedFile(id: Long): TrashedFileEntity? {
        return trashDao.getTrashedFile(id)
    }
}
