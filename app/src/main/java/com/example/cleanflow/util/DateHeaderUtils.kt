package com.example.cleanflow.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for grouping timestamps into human-readable date categories.
 */
object DateHeaderUtils {
    
    private val spanishLocale = Locale.forLanguageTag("es-ES")
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", spanishLocale)
    
    /**
     * Converts a timestamp to a human-readable category.
     * @param timestamp Unix timestamp in seconds (as stored in MediaStore)
     * @return Category string: "Hoy", "Ayer", "Esta semana", "Este mes", or "Mes Año"
     */
    fun getDateCategory(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        
        // Reset to start of day for comparison
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        // Convert timestamp (seconds) to Date
        val fileDate = Calendar.getInstance().apply {
            timeInMillis = timestamp * 1000 // Convert seconds to millis
        }
        
        val fileDay = Calendar.getInstance().apply {
            timeInMillis = timestamp * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val yesterday = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val weekAgo = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }
        
        val monthStart = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        return when {
            fileDay.timeInMillis >= today.timeInMillis -> "Hoy"
            fileDay.timeInMillis >= yesterday.timeInMillis -> "Ayer"
            fileDay.timeInMillis >= weekAgo.timeInMillis -> "Esta semana"
            fileDay.timeInMillis >= monthStart.timeInMillis -> "Este mes"
            else -> monthYearFormat.format(Date(timestamp * 1000)).replaceFirstChar { 
                it.titlecase(spanishLocale) 
            }
        }
    }
}
