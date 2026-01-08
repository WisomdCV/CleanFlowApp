package com.example.cleanflow.domain.util

/**
 * A sealed class that encapsulates the result of an operation.
 * Used throughout the app for consistent error handling.
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with an exception.
     */
    data class Error(val exception: AppException) : Result<Nothing>()
    
    /**
     * Represents an operation in progress.
     */
    data object Loading : Result<Nothing>()
    
    /**
     * Returns true if this is a Success.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if this is an Error.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Returns the data if Success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the exception if Error, or null otherwise.
     */
    fun exceptionOrNull(): AppException? = when (this) {
        is Error -> exception
        else -> null
    }
    
    /**
     * Maps the success value to another type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }
    
    companion object {
        /**
         * Wraps a suspending block in a Result, catching exceptions.
         */
        suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: AppException) {
                Error(e)
            } catch (e: SecurityException) {
                Error(AppException.PermissionDenied)
            } catch (e: java.io.FileNotFoundException) {
                Error(AppException.FileNotFound)
            } catch (e: Exception) {
                Error(AppException.Unknown(e.message ?: "Error desconocido"))
            }
        }
    }
}
