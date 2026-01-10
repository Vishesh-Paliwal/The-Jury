package com.example.the_jury.service

import com.example.the_jury.model.AgentPersona
import kotlinx.coroutines.flow.Flow

/**
 * Service interface for handling streaming responses from AI agents.
 * Provides real-time streaming of agent responses using Kotlin Flow.
 */
interface StreamingService {
    /**
     * Stream a response from an AI agent for the given prompt and persona.
     * 
     * @param prompt The input prompt to send to the agent
     * @param persona The agent persona configuration
     * @return Flow of StreamingResponse objects containing incremental content
     */
    fun streamResponse(
        prompt: String,
        persona: AgentPersona
    ): Flow<StreamingResponse>
    
    /**
     * Cancel an active stream by its ID.
     * 
     * @param streamId The unique identifier of the stream to cancel
     */
    suspend fun cancelStream(streamId: String)
    
    /**
     * Get the current status of all active streams.
     * 
     * @return Map of stream IDs to their current status
     */
    fun getActiveStreams(): Map<String, StreamStatus>
}

/**
 * Represents a streaming response chunk from an AI agent.
 */
data class StreamingResponse(
    val streamId: String,
    val content: String,
    val isComplete: Boolean,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the current status of a streaming operation.
 */
enum class StreamStatus {
    STARTING,
    STREAMING,
    COMPLETED,
    CANCELLED,
    ERROR
}