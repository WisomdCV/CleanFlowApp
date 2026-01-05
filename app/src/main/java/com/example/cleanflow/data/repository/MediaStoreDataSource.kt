package com.example.cleanflow.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.cleanflow.data.model.MediaFileEntity

class MediaStoreDataSource(private val context: Context) {

    fun getMediaFiles(): List<MediaFileEntity> {
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
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "Unknown"
                val dateAdded = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: ""
                val path = cursor.getString(pathColumn) ?: ""
                val bucketName = cursor.getString(bucketColumn) ?: "Internal Storage"

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
                        bucketName = bucketName
                    )
                )
            }
        }
        return mediaList
    }
}
