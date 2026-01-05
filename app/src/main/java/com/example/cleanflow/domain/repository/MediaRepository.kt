package com.example.cleanflow.domain.repository

import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.model.MediaFile
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllCollections(): Flow<List<MediaCollection>>
    fun getFilesByCollection(collectionId: String): Flow<List<MediaFile>>
}
