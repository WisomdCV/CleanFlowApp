package com.example.cleanflow.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.cleanflow.data.model.MediaFileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DataSource that queries MediaStore and caches results in a StateFlow.
 * Call refreshMediaFiles() to reload from disk.
 */
class MediaStoreDataSource(private val context: Context) {

    private val _mediaFiles = MutableStateFlow<List<MediaFileEntity>>(emptyList())
    val mediaFiles: StateFlow<List<MediaFileEntity>> = _mediaFiles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Load media files on initialization
        refreshMediaFiles()
    }

    /**
     * Refresh the cached media files from MediaStore.
     * Call this after deleting files or when you want to sync with disk.
     */
    fun refreshMediaFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.value = true
            Log.d("CleanFlow", "Refreshing media files cache...")
            val files = queryMediaStore()
            _mediaFiles.value = files
            _isLoading.value = false
            Log.d("CleanFlow", "Media cache loaded: ${files.size} files")
        }
    }

    private fun queryMediaStore(): List<MediaFileEntity> {
        val mediaList = mutableListOf<MediaFileEntity>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val query = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "Unknown"
                val dateAdded = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: ""
                val path = cursor.getString(pathColumn) ?: ""
                val bucketName = cursor.getString(bucketColumn) ?: "Internal Storage"
                val duration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    id
                ).toString()

                mediaList.add(
                    MediaFileEntity(
                        id = id,
                        uri = contentUri,
                        path = path,
                        displayName = displayName,
                        size = size,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        bucketName = bucketName,
                        duration = duration
                    )
                )
            }
        }
        return mediaList
    }
    
    fun deleteFile(uri: String): Boolean {
        Log.d("CleanFlow", "Attempting to delete file: $uri")
        return try {
            val contentUri = android.net.Uri.parse(uri)
            val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
            Log.d("CleanFlow", "Delete result: $rowsDeleted rows deleted")
            if (rowsDeleted > 0) {
                // Refresh cache after successful delete
                refreshMediaFiles()
            }
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e("CleanFlow", "Error deleting file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Delete file without refreshing cache. Use for bulk deletes.
     * Call refreshMediaFiles() manually after all deletes are complete.
     */
    fun deleteFileWithoutRefresh(uri: String): Boolean {
        return try {
            val contentUri = android.net.Uri.parse(uri)
            val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e("CleanFlow", "Error deleting file: ${e.message}", e)
            false
        }
    }
}

