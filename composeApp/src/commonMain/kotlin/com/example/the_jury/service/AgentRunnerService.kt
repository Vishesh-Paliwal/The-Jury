package com.example.the_jury.service

//import ai.koog.agents.AIAgent
import ai.koog.agents.core.agent.AIAgent
//import ai.koog.agents.google.GoogleModels
//import ai.koog.agents.google.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.example.the_jury.model.AgentPersona
import com.example.the_jury.model.AgentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

class AgentRunnerService(
    private val apiKey: String
) {
    // In a real app, we might cache these agents or recreate them per run.
    private fun createAgent(persona: AgentPersona): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            llmModel = GoogleModels.Gemini2_5Flash,
            systemPrompt = persona.systemInstruction
        )
    }

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
