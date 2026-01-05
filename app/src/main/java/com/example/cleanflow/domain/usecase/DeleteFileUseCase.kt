package com.example.cleanflow.domain.usecase

import com.example.cleanflow.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteFileUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            repository.deleteFile(uri)
        }
    }
}
