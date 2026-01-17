package com.example.cleanflow.domain.model

data class SmartDashboardStats(
    val largeFilesCount: Int = 0,
    val largeFilesSize: Long = 0L,
    val duplicateGroupsCount: Int = 0,
    val duplicateWastedSize: Long = 0L,
    val oldFilesCount: Int = 0,
    val oldFilesSize: Long = 0L,
    val videoSize: Long = 0L,
    val imageSize: Long = 0L,
    val totalUsedSize: Long = 0L
)
