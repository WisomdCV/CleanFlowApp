package com.example.cleanflow.data.repository

import com.example.cleanflow.data.local.TrashDao
import com.example.cleanflow.data.local.TrashedFileEntity
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.util.AppException
import com.example.cleanflow.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing trashed files.
 */
class TrashRepository(private val trashDao: TrashDao) {
    
    val trashedFiles: Flow<List<TrashedFileEntity>> = trashDao.getAllTrashed()
    
    val trashedIds: Flow<List<Long>> = trashDao.getTrashedIds()
    
    val trashCount: Flow<Int> = trashDao.getTrashCount()
    
    suspend fun addToTrash(file: MediaFile, collectionId: String): Result<Unit> {
        return try {
            val entity = TrashedFileEntity(
                mediaId = file.id,
                uri = file.uri,
                displayName = file.displayName,
                size = file.size,
                mimeType = file.mimeType,
                collectionId = collectionId
            )
            trashDao.addToTrash(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseError(e.message ?: "Error al agregar a papelera"))
        }
    }
    
    suspend fun removeFromTrash(id: Long): Result<Unit> {
        return try {
            trashDao.removeFromTrash(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseError(e.message ?: "Error al remover de papelera"))
        }
    }
    
    suspend fun clearTrash(): Result<Unit> {
        return try {
            trashDao.clearTrash()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.DatabaseError(e.message ?: "Error al vaciar papelera"))
        }
    }
    
    suspend fun getTrashedFile(id: Long): TrashedFileEntity? {
        return trashDao.getTrashedFile(id)
    }
}
