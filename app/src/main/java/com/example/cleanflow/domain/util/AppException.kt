package com.example.cleanflow.domain.util

/**
 * Sealed class representing different types of application errors.
 * Provides type-safe error handling with user-friendly messages.
 */
sealed class AppException(
    override val message: String,
    val userMessage: String = message
) : Exception(message) {
    
    /**
     * User doesn't have required permissions.
     */
    data object PermissionDenied : AppException(
        message = "Permission denied",
        userMessage = "Permiso denegado. Ve a Configuración para otorgar acceso."
    )
    
    /**
     * Requested file was not found.
     */
    data object FileNotFound : AppException(
        message = "File not found",
        userMessage = "El archivo no existe o fue movido."
    )
    
    /**
     * Device storage is full.
     */
    data object StorageFull : AppException(
        message = "Storage full",
        userMessage = "Almacenamiento lleno. Libera espacio e intenta de nuevo."
    )
    
    /**
     * Operation failed for unknown reasons.
     */
    data class Unknown(override val message: String) : AppException(
        message = message,
        userMessage = "Ocurrió un error: $message"
    )
    
    /**
     * Operation was cancelled by user.
     */
    data object Cancelled : AppException(
        message = "Operation cancelled",
        userMessage = "Operación cancelada."
    )
    
    /**
     * Database operation failed.
     */
    data class DatabaseError(override val message: String) : AppException(
        message = message,
        userMessage = "Error en base de datos."
    )
}
