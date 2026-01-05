package com.example.cleanflow.domain.model

data class MediaCollection(
    val id: String,
    val displayName: String,
    val fileCount: Int,
    val totalSize: Long,
    val previewUri: String
)
