package com.example.cleanflow.domain.model

/**
 * Sealed class for Gallery grid items.
 * Allows mixing headers and media items in a single list.
 */
sealed class GalleryItem {
    /**
     * Date header (e.g., "Hoy", "Ayer", "Agosto 2024")
     */
    data class Header(val title: String) : GalleryItem()
    
    /**
     * Media item with its original index for navigation
     */
    data class Media(
        val file: MediaFile,
        val originalIndex: Int
    ) : GalleryItem()
}
