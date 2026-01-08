package com.example.cleanflow.util

/**
 * Formats video duration in milliseconds to a human-readable string.
 * Format: "M:SS" for durations under 1 hour, "H:MM:SS" for longer.
 */
object DurationFormatter {
    
    fun format(durationMs: Long): String {
        if (durationMs <= 0) return "0:00"
        
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
