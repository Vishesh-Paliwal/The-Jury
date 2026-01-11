package com.example.the_jury.service

//import ai.koog.agents.AIAgent
import ai.koog.agents.core.agent.AIAgent
//import ai.koog.agents.google.GoogleModels
//import ai.koog.agents.google.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.example.the_jury.model.AgentPersona
import com.example.the_jury.model.AgentResult
import com.example.the_jury.model.StreamingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class AgentRunnerService(
    private val apiKey: String,
    private val streamingService: StreamingService
) {
    // In a real app, we might cache these agents or recreate them per run.
    private fun createAgent(persona: AgentPersona): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            llmModel = GoogleModels.Gemini2_5Flash,
            systemPrompt = persona.systemInstruction
        )
    }

    /**
     * Run agents with streaming responses for incremental text display.
     * Requirements: 4.1 - display text incrementally as it arrives
     * Requirements: 4.2, 4.4 - error recovery and message integrity
     */
    suspend fun runAgentsWithStreaming(
        prompt: String,
        personas: List<AgentPersona>
    ): Flow<List<AgentResult>> = channelFlow {
        // Initialize results map with loading state for all agents
        val resultsMap = personas.associate { 
            it.id to AgentResult(personaId = it.id, isLoading = true) 
        }.toMutableMap()
        
        // Emit initial loading state
        send(resultsMap.values.toList())

        // Track accumulated content for each persona
        val accumulatedContent = mutableMapOf<String, String>()

        // Launch all streaming flows concurrently
        personas.forEach { persona ->
            launch {
                try {
                    streamingService.streamResponse(prompt, persona)
                        .collect { streamingResponse ->
                            val result = when {
                                streamingResponse.error != null -> {
                                    // Preserve partial content on error
                                    val partialContent = accumulatedContent[persona.id] ?: ""
                                    AgentResult(
                                        personaId = persona.id,
                                        response = partialContent,
                                        isLoading = false,
                                        error = streamingResponse.error
                                    )
                                }
                                streamingResponse.isComplete -> {
                                    // Use accumulated content for final result
                                    val finalContent = accumulatedContent[persona.id] ?: ""
                                    AgentResult(
                                        personaId = persona.id,
                                        response = finalContent,
                                        isLoading = false
                                    )
                                }
                                else -> {
                                    // Accumulate streaming content
                                    val currentContent = accumulatedContent[persona.id] ?: ""
                                    val newContent = currentContent + streamingResponse.content
                                    accumulatedContent[persona.id] = newContent
                                    
                                    AgentResult(
                                        personaId = persona.id,
                                        response = newContent,
                                        isLoading = true
                                    )
                                }
                            }
                            
                            // Update the results map and emit immediately
                            synchronized(resultsMap) {
                                resultsMap[persona.id] = result
                                // Send current state of all results
                                trySend(resultsMap.values.toList())
                            }
                        }
                } catch (e: Exception) {
                    // Handle any errors during streaming
                    val errorResult = AgentResult(
                        personaId = persona.id,
                        response = accumulatedContent[persona.id] ?: "",
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                    synchronized(resultsMap) {
                        resultsMap[persona.id] = errorResult
                        trySend(resultsMap.values.toList())
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cancel all active streams for the given personas.
     * Requirements: 4.2 - handle connection interruptions gracefully
     */
    suspend fun cancelStreaming(personas: List<AgentPersona>) {
        // Get active streams and cancel them
        val activeStreams = streamingService.getActiveStreams()
        activeStreams.keys.forEach { streamId ->
            try {
                streamingService.cancelStream(streamId)
            } catch (e: Exception) {
                // Log error but don't throw - cancellation should be best effort
                println("Error cancelling stream $streamId: ${e.message}")
            }
        }
    }

    /**
     * Retry streaming for a specific persona.
     * Requirements: 4.2 - error recovery mechanisms
     */
    suspend fun retryAgentStreaming(
        prompt: String,
        persona: AgentPersona
    ): Flow<AgentResult> = flow {
        // Emit loading state
        emit(AgentResult(personaId = persona.id, isLoading = true))
        
        // Stream response for this specific persona
        streamingService.streamResponse(prompt, persona)
            .collect { streamingResponse ->
                val result = when {
                    streamingResponse.error != null -> {
                        AgentResult(
                            personaId = persona.id,
                            response = "",
                            isLoading = false,
                            error = streamingResponse.error
                        )
                    }
                    streamingResponse.isComplete -> {
                        AgentResult(
                            personaId = persona.id,
                            response = streamingResponse.content,
                            isLoading = false
                        )
                    }
                    else -> {
                        AgentResult(
                            personaId = persona.id,
                            response = streamingResponse.content,
                            isLoading = true
                        )
                    }
                }
                emit(result)
            }
    }.flowOn(Dispatchers.IO)

    /**
     * Legacy method for non-streaming parallel execution.
     * Maintained for backward compatibility.
     */
    suspend fun runAgents(
        prompt: String,
        personas: List<AgentPersona>
    ): Flow<List<AgentResult>> = flow {
        // Initial Loading State
        val initialResults = personas.map {
            AgentResult(personaId = it.id, isLoading = true)
        }
        emit(initialResults)

        // Run agents in parallel
        val finalResults = coroutineScope {
            personas.map { persona ->
                async {
                    try {
                        val agent = createAgent(persona)
                        // Koog's run is blocking/suspending depending on implementation.
                        // Since we are in IO context, it should be fine.
                        val response = agent.run(prompt)
                        AgentResult(
                             personaId = persona.id,
                             response = response,
                             isLoading = false
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        AgentResult(
                            personaId = persona.id,
                            error = e.message ?: "Unknown error",
                            isLoading = false
                        )
                    }
                }
            }.awaitAll()
        }
        emit(finalResults)
    }.flowOn(Dispatchers.IO)
}
