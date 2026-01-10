package com.example.the_jury.service

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the state and lifecycle of streaming operations.
 * Provides thread-safe operations for tracking active streams and their cancellation.
 */
class StreamStateManager {
    
    // Thread-safe storage for active streams
    private val activeStreams = mutableMapOf<String, StreamStatus>()
    private val streamJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()
    
    /**
     * Register a new stream with the given ID and initial status.
     */
    suspend fun registerStream(streamId: String, status: StreamStatus = StreamStatus.STARTING) {
        mutex.withLock {
            activeStreams[streamId] = status
        }
    }
    
    /**
     * Update the status of an existing stream.
     */
    suspend fun updateStreamStatus(streamId: String, status: StreamStatus) {
        mutex.withLock {
            activeStreams[streamId] = status
        }
    }
    
    /**
     * Get the current status of a stream.
     */
    suspend fun getStreamStatus(streamId: String): StreamStatus? {
        return mutex.withLock {
            activeStreams[streamId]
        }
    }
    
    /**
     * Get all active streams and their statuses.
     */
    suspend fun getAllActiveStreams(): Map<String, StreamStatus> {
        return mutex.withLock {
            activeStreams.toMap()
        }
    }
    
    /**
     * Register a job for a stream to enable cancellation.
     */
    suspend fun registerStreamJob(streamId: String, job: Job) {
        mutex.withLock {
            streamJobs[streamId] = job
        }
    }
    
    /**
     * Cancel a stream by its ID.
     */
    suspend fun cancelStream(streamId: String): Boolean {
        return mutex.withLock {
            val job = streamJobs[streamId]
            val wasCancelled = job?.let {
                it.cancel()
                true
            } ?: false
            
            if (wasCancelled) {
                activeStreams[streamId] = StreamStatus.CANCELLED
            }
            
            wasCancelled
        }
    }
    
    /**
     * Check if a stream is cancelled.
     */
    suspend fun isStreamCancelled(streamId: String): Boolean {
        return mutex.withLock {
            activeStreams[streamId] == StreamStatus.CANCELLED
        }
    }
    
    /**
     * Clean up a stream from tracking.
     */
    suspend fun cleanupStream(streamId: String) {
        mutex.withLock {
            activeStreams.remove(streamId)
            streamJobs.remove(streamId)
        }
    }
    
    /**
     * Get the count of active streams.
     */
    suspend fun getActiveStreamCount(): Int {
        return mutex.withLock {
            activeStreams.size
        }
    }
    
    /**
     * Check if there are any active streams.
     */
    suspend fun hasActiveStreams(): Boolean {
        return mutex.withLock {
            activeStreams.isNotEmpty()
        }
    }
}