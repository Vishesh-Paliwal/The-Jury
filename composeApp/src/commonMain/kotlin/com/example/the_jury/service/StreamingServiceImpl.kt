package com.example.the_jury.service

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.example.the_jury.model.AgentPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of StreamingService that provides real-time streaming of AI agent responses.
 * Uses Kotlin Flow for reactive streaming and manages stream state and cancellation.
 */
class StreamingServiceImpl(
    private val apiKey: String
) : StreamingService {
    
    // Stream state manager for thread-safe stream lifecycle management
    private val streamStateManager = StreamStateManager()
    
    // Error handler for streaming operations
    private val errorHandler = StreamingErrorHandler()
    
    @OptIn(ExperimentalUuidApi::class)
    override fun streamResponse(
        prompt: String,
        persona: AgentPersona
    ): Flow<StreamingResponse> = flow {
        val streamId = Uuid.random().toString()
        
        try {
            // Register and mark stream as starting
            streamStateManager.registerStream(streamId, StreamStatus.STARTING)
            
            // Create AI agent for this persona
            val agent = createAgent(persona)
            
            // Emit starting response
            emit(StreamingResponse(
                streamId = streamId,
                content = "",
                isComplete = false
            ))
            
            // Mark stream as active
            streamStateManager.updateStreamStatus(streamId, StreamStatus.STREAMING)
            
            // Get the full response from the agent with error handling
            val fullResponse = errorHandler.executeWithRetry { attempt ->
                agent.run(prompt)
            }
            
            // Simulate streaming by breaking the response into chunks
            val chunks = simulateStreamingChunks(fullResponse)
            
            for ((index, chunk) in chunks.withIndex()) {
                // Check if stream was cancelled
                if (!coroutineContext.isActive || streamStateManager.isStreamCancelled(streamId)) {
                    streamStateManager.updateStreamStatus(streamId, StreamStatus.CANCELLED)
                    emit(StreamingResponse(
                        streamId = streamId,
                        content = "",
                        isComplete = true,
                        error = "Stream was cancelled"
                    ))
                    return@flow
                }
                
                // Emit the chunk
                emit(StreamingResponse(
                    streamId = streamId,
                    content = chunk,
                    isComplete = false
                ))
                
                // Add a delay to simulate real streaming (more visible)
                delay(200)
            }
            
            // Emit completion
            streamStateManager.updateStreamStatus(streamId, StreamStatus.COMPLETED)
            emit(StreamingResponse(
                streamId = streamId,
                content = "",
                isComplete = true
            ))
            
        } catch (e: Exception) {
            streamStateManager.updateStreamStatus(streamId, StreamStatus.ERROR)
            
            val userFriendlyMessage = errorHandler.createUserFriendlyErrorMessage(e)
            
            emit(StreamingResponse(
                streamId = streamId,
                content = "",
                isComplete = true,
                error = userFriendlyMessage
            ))
            
            // Cancel stream if it's a non-recoverable error
            if (errorHandler.shouldCancelStream(e)) {
                streamStateManager.cancelStream(streamId)
            }
        } finally {
            // Clean up stream tracking
            streamStateManager.cleanupStream(streamId)
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun cancelStream(streamId: String) {
        streamStateManager.cancelStream(streamId)
    }
    
    override fun getActiveStreams(): Map<String, StreamStatus> {
        // Note: This is a blocking call that should ideally be suspend, but the interface doesn't allow it
        // In a real implementation, this might need to be redesigned to be suspend
        return runBlocking {
            streamStateManager.getAllActiveStreams()
        }
    }
    
    private fun createAgent(persona: AgentPersona): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            llmModel = GoogleModels.Gemini2_5Flash,
            systemPrompt = persona.systemInstruction
        )
    }
    
    /**
     * Simulates streaming by breaking a full response into chunks.
     * In a real streaming implementation, this would be replaced by actual
     * streaming API calls that return partial responses.
     */
    private fun simulateStreamingChunks(fullResponse: String): List<String> {
        val words = fullResponse.split(" ")
        val chunks = mutableListOf<String>()
        
        // Group words into chunks of 2-4 words each for better streaming effect
        var currentChunk = ""
        var wordCount = 0
        
        for (word in words) {
            currentChunk += if (currentChunk.isEmpty()) word else " $word"
            wordCount++
            
            // Vary chunk size between 2-4 words for more natural streaming
            val chunkSize = (2..4).random()
            if (wordCount >= chunkSize) {
                chunks.add(currentChunk)
                currentChunk = ""
                wordCount = 0
            }
        }
        
        // Add any remaining words as the final chunk
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk)
        }
        
        return chunks
    }
}