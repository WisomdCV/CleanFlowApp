package com.example.cleanflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a file that has been moved to the trash.
 * The file is not physically deleted, just marked as trashed.
 */
@Entity(tableName = "trashed_files")
data class TrashedFileEntity(
    @PrimaryKey val mediaId: Long,
    val uri: String,
    val displayName: String,
    val size: Long,
    val mimeType: String,
    val collectionId: String,
    val trashedAt: Long = System.currentTimeMillis()
)
