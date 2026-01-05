package com.example.cleanflow.domain.model

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaFile(
    val id: Long,
    val uri: String,
    val path: String,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val type: MediaType
)
