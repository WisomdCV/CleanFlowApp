package com.example.cleanflow.domain.model

enum class ContentScaleMode {
    FIT, CROP
}

data class UserPreferences(
    val autoPlayVideo: Boolean = true,
    val defaultContentScale: ContentScaleMode = ContentScaleMode.CROP
)
