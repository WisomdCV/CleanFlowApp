package com.example.cleanflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaFileEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val path: String,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val bucketName: String, // Added for grouping
    val duration: Long = 0 // Duration in milliseconds (for videos)
)
