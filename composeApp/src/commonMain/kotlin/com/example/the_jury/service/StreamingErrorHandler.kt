package com.example.the_jury.service

import kotlinx.coroutines.delay

/**
 * Handles errors and recovery strategies for streaming operations.
 */
class StreamingErrorHandler {
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
    }
    
    /**
     * Determines if an error is recoverable and should be retried.
     */
    fun isRecoverableError(error: Throwable): Boolean {
        return when {
            error is java.net.SocketTimeoutException -> true
            error is java.net.ConnectException -> true
            error is java.io.IOException -> true
            error.message?.contains("timeout", ignoreCase = true) == true -> true
            error.message?.contains("connection", ignoreCase = true) == true -> true
            error.message?.contains("network", ignoreCase = true) == true -> true
            else -> false
        }
    }
    
    /**
     * Calculates the delay for retry attempts using exponential backoff.
     */
    fun calculateRetryDelay(attemptNumber: Int): Long {
        val delay = BASE_DELAY_MS * (1L shl (attemptNumber - 1))
        return minOf(delay, MAX_DELAY_MS)
    }
    
    /**
     * Executes a retry strategy with exponential backoff.
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        operation: suspend (attempt: Int) -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Exception) {
                lastException = e
                
                if (attempt < maxAttempts - 1 && isRecoverableError(e)) {
                    val delayMs = calculateRetryDelay(attempt + 1)
                    delay(delayMs)
                } else {
                    throw e
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error during retry operation")
    }
    
    /**
     * Creates a user-friendly error message from an exception.
     */
    fun createUserFriendlyErrorMessage(error: Throwable): String {
        return when {
            error is java.net.SocketTimeoutException -> 
                "Connection timed out. Please check your internet connection and try again."
            error is java.net.ConnectException -> 
                "Unable to connect to the service. Please check your internet connection."
            error is java.io.IOException -> 
                "Network error occurred. Please try again."
            error.message?.contains("API", ignoreCase = true) == true -> 
                "API service error. Please try again later."
            error.message?.contains("rate limit", ignoreCase = true) == true -> 
                "Too many requests. Please wait a moment and try again."
            error.message?.contains("unauthorized", ignoreCase = true) == true -> 
                "Authentication failed. Please check your API key."
            else -> 
                "An unexpected error occurred: ${error.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Determines if an error should trigger a stream cancellation.
     */
    fun shouldCancelStream(error: Throwable): Boolean {
        return when {
            error.message?.contains("unauthorized", ignoreCase = true) == true -> true
            error.message?.contains("forbidden", ignoreCase = true) == true -> true
            error.message?.contains("invalid", ignoreCase = true) == true -> true
            error is IllegalArgumentException -> true
            else -> false
        }
    }
}