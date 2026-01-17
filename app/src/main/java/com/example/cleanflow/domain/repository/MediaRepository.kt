package com.example.cleanflow.domain.repository

import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.model.MediaFile
import com.example.cleanflow.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllCollections(): Flow<List<MediaCollection>>
    fun getAllFiles(): Flow<List<MediaFile>>
    fun getFilesByCollection(collectionId: String): Flow<List<MediaFile>>
    suspend fun deleteFile(uri: String): Result<Boolean>
    suspend fun deleteFileWithoutRefresh(uri: String): Result<Boolean>
    fun refreshCache()
}
