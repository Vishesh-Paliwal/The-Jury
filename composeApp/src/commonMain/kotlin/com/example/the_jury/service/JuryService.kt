package com.example.the_jury.service

import com.example.the_jury.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Main orchestrator for the jury deliberation process
 * Requirements: 1.1, 2.1, 2.4, 2.6
 */
class JuryService(
    private val agentRunnerService: AgentRunnerService,
    private val trialService: TrialService,
    private val moderatorAgent: ModeratorAgent
) {
    companion object {
        private const val MAX_DELIBERATION_ROUNDS = 5
        private const val AGENT_TIMEOUT_MS = 30_000L // 30 seconds
        private const val TOTAL_TRIAL_TIMEOUT_MS = 300_000L // 5 minutes
    }
    
    /**
     * Exposes TrialService loading state for UI
     * Requirements: 3.5 - loading states during data retrieval
     */
    val isLoading: StateFlow<Boolean> get() = trialService.isLoading
    
    /**
     * Exposes TrialService error state for UI
     * Requirements: 3.5 - handle empty state and error conditions
     */
    val error: StateFlow<String?> get() = trialService.error
    
    /**
     * Initializes the jury service by loading existing trials from persistence
     * Requirements: 3.2 - restore chat history on app startup
     */
    suspend fun initialize() {
        trialService.initialize()
    }
    
    /**
     * Clears error state
     */
    fun clearError() {
        trialService.clearError()
    }

    /**
     * Conducts a complete jury trial with deliberation flow
     * Requirements: 1.1, 2.1, 2.4, 2.6
     */
    suspend fun conductTrial(
        question: String,
        personas: List<AgentPersona>
    ): Flow<TrialState> = channelFlow {
        try {
            // Create initial trial
            val trial = trialService.createTrial(question, personas)
            send(TrialState(trial = trial, currentlyThinking = emptyList(), isComplete = false))

            // Add initial question interaction
            val initialQuestionInteraction = TrialInteraction(
                trialId = trial.id,
                type = InteractionType.INITIAL_QUESTION,
                speaker = "moderator",
                content = question,
                roundNumber = 1
            )
            trialService.addInteraction(trial.id, initialQuestionInteraction)
            
            // Emit with the question added
            val trialWithQuestion = trialService.getTrial(trial.id) ?: return@channelFlow
            send(TrialState(trial = trialWithQuestion, currentlyThinking = emptyList(), isComplete = false))

            // Update status to gathering initial responses
            trialService.updateTrialStatus(trial.id, TrialStatus.GATHERING_INITIAL_RESPONSES)
            val updatedTrial = trialService.getTrial(trial.id) ?: return@channelFlow
            send(TrialState(
                trial = updatedTrial,
                currentlyThinking = personas.map { it.id },
                isComplete = false
            ))

            // Gather initial responses from all personas - streaming each as it completes
            val initialResponses = gatherInitialResponsesStreaming(trial.id, question, personas) { personaId, response ->
                // Callback when each persona completes - add interaction immediately
                val responseInteraction = TrialInteraction(
                    trialId = trial.id,
                    type = InteractionType.INITIAL_RESPONSE,
                    speaker = personaId,
                    content = response,
                    roundNumber = 1
                )
                trialService.addInteraction(trial.id, responseInteraction)
                
                // Get updated trial and emit
                val currentTrial = trialService.getTrial(trial.id)
                if (currentTrial != null) {
                    // Remove this persona from thinking list
                    val stillThinking = personas.filter { p -> 
                        p.id != personaId && 
                        currentTrial.interactions.none { it.speaker == p.id && it.roundNumber == 1 }
                    }.map { it.id }
                    trySend(TrialState(
                        trial = currentTrial,
                        currentlyThinking = stillThinking,
                        isComplete = false
                    ))
                }
            }
            
            if (initialResponses.isEmpty()) {
                trialService.failTrial(trial.id, "Failed to gather initial responses from personas")
                val failedTrial = trialService.getTrial(trial.id) ?: return@channelFlow
                send(TrialState(trial = failedTrial, currentlyThinking = emptyList(), isComplete = true))
                return@channelFlow
            }

            // Update status to deliberating
            trialService.updateTrialStatus(trial.id, TrialStatus.DELIBERATING)
            val deliberatingTrial = trialService.getTrial(trial.id) ?: return@channelFlow
            send(TrialState(
                trial = deliberatingTrial,
                currentlyThinking = emptyList(),
                isComplete = false
            ))

            // Conduct deliberation rounds
            var currentRound = 2
            var allInteractions = deliberatingTrial.interactions

            while (currentRound <= MAX_DELIBERATION_ROUNDS) {
                // Show moderator thinking while deciding whether to continue
                send(TrialState(
                    trial = trialService.getTrial(trial.id) ?: return@channelFlow,
                    currentlyThinking = listOf("moderator"),
                    isComplete = false
                ))
                
                // Check if we should continue deliberation
                val shouldContinue = moderatorAgent.shouldContinueDeliberation(allInteractions, currentRound - 1, personas)
                if (!shouldContinue) {
                    break
                }

                // Generate follow-up questions (moderator still thinking)
                val followUpQuestions = moderatorAgent.generateFollowUpQuestions(question, initialResponses, personas)
                if (followUpQuestions.isEmpty()) {
                    break
                }

                // Add follow-up questions to trial first (moderator asks)
                for (followUp in followUpQuestions) {
                    val questionInteraction = TrialInteraction(
                        trialId = trial.id,
                        type = InteractionType.FOLLOW_UP_QUESTION,
                        speaker = "moderator",
                        content = followUp.question,
                        targetPersona = followUp.targetPersonaId,
                        roundNumber = currentRound
                    )
                    trialService.addInteraction(trial.id, questionInteraction)
                    
                    // Emit after each question is added
                    val trialAfterQuestion = trialService.getTrial(trial.id) ?: continue
                    send(TrialState(
                        trial = trialAfterQuestion,
                        currentlyThinking = followUpQuestions.map { it.targetPersonaId },
                        isComplete = false
                    ))
                }

                // Gather follow-up responses - streaming each as it completes
                val followUpResponses = gatherFollowUpResponsesStreaming(
                    trial.id,
                    followUpQuestions,
                    personas,
                    currentRound
                ) { personaId, response ->
                    // Callback when each persona responds
                    val responseInteraction = TrialInteraction(
                        trialId = trial.id,
                        type = InteractionType.FOLLOW_UP_RESPONSE,
                        speaker = personaId,
                        content = response,
                        roundNumber = currentRound
                    )
                    trialService.addInteraction(trial.id, responseInteraction)
                    
                    // Get updated trial and emit
                    val currentTrial = trialService.getTrial(trial.id)
                    if (currentTrial != null) {
                        val stillThinking = followUpQuestions.filter { fq ->
                            fq.targetPersonaId != personaId &&
                            currentTrial.interactions.none { 
                                it.speaker == fq.targetPersonaId && 
                                it.roundNumber == currentRound && 
                                it.type == InteractionType.FOLLOW_UP_RESPONSE 
                            }
                        }.map { it.targetPersonaId }
                        trySend(TrialState(
                            trial = currentTrial,
                            currentlyThinking = stillThinking,
                            isComplete = false
                        ))
                    }
                }

                // Update interactions for next iteration
                val updatedTrialAfterRound = trialService.getTrial(trial.id) ?: return@channelFlow
                allInteractions = updatedTrialAfterRound.interactions
                
                send(TrialState(
                    trial = updatedTrialAfterRound,
                    currentlyThinking = emptyList(),
                    isComplete = false
                ))

                currentRound++
            }

            // Generate final verdict
            trialService.updateTrialStatus(trial.id, TrialStatus.GENERATING_VERDICT)
            val verdictGeneratingTrial = trialService.getTrial(trial.id) ?: return@channelFlow
            send(TrialState(
                trial = verdictGeneratingTrial,
                currentlyThinking = listOf("moderator"),
                isComplete = false
            ))

            val finalVerdict = moderatorAgent.synthesizeVerdict(question, allInteractions, personas)
            
            // Add verdict interaction
            val verdictInteraction = TrialInteraction(
                trialId = trial.id,
                type = InteractionType.VERDICT,
                speaker = "moderator",
                content = finalVerdict,
                roundNumber = currentRound
            )
            trialService.addInteraction(trial.id, verdictInteraction)

            // Complete the trial
            val completedTrial = trialService.completeTrial(trial.id, finalVerdict)
            if (completedTrial != null) {
                send(TrialState(
                    trial = completedTrial,
                    currentlyThinking = emptyList(),
                    isComplete = true
                ))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Try to fail the trial gracefully
            try {
                val currentTrial = trialService.getTrial(question) // This might not work, but we try
                currentTrial?.let { trial ->
                    val failedTrial = trialService.failTrial(trial.id, e.message ?: "Unknown error")
                    if (failedTrial != null) {
                        send(TrialState(
                            trial = failedTrial,
                            currentlyThinking = emptyList(),
                            isComplete = true
                        ))
                    }
                }
            } catch (failException: Exception) {
                // If we can't even fail gracefully, just let the flow end
                failException.printStackTrace()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stops an ongoing trial
     * Requirements: 7.5 (error handling)
     */
    suspend fun stopTrial(trialId: String) {
        trialService.failTrial(trialId, "Trial stopped by user")
    }

    /**
     * Gathers initial responses from all personas with timeout handling
     * Requirements: 2.1, 7.5
     */
    private suspend fun gatherInitialResponses(
        trialId: String,
        question: String,
        personas: List<AgentPersona>
    ): List<AgentResult> = withContext(Dispatchers.IO) {
        return@withContext withTimeoutOrNull(TOTAL_TRIAL_TIMEOUT_MS) {
            try {
                val responses = mutableListOf<AgentResult>()
                
                // Use the existing AgentRunnerService to run agents in parallel
                agentRunnerService.runAgents(question, personas).collect { results ->
                    // Take the final results (when all agents are done loading)
                    if (results.none { it.isLoading }) {
                        responses.addAll(results.filter { it.error == null && it.response.isNotBlank() })
                        return@collect
                    }
                }
                
                responses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * Gathers initial responses with streaming - calls back as each agent completes
     * Requirements: 2.1, 4.1 - incremental display
     */
    private suspend fun gatherInitialResponsesStreaming(
        trialId: String,
        question: String,
        personas: List<AgentPersona>,
        onResponseReceived: suspend (personaId: String, response: String) -> Unit
    ): List<AgentResult> = withContext(Dispatchers.IO) {
        return@withContext withTimeoutOrNull(TOTAL_TRIAL_TIMEOUT_MS) {
            try {
                val completedResponses = mutableListOf<AgentResult>()
                val completedPersonas = mutableSetOf<String>()
                
                // Use streaming service to get responses as they complete
                agentRunnerService.runAgentsWithStreaming(question, personas).collect { results ->
                    // Check for newly completed responses
                    results.forEach { result ->
                        if (!result.isLoading && 
                            result.error == null && 
                            result.response.isNotBlank() &&
                            !completedPersonas.contains(result.personaId)) {
                            
                            // Mark as completed and callback
                            completedPersonas.add(result.personaId)
                            completedResponses.add(result)
                            onResponseReceived(result.personaId, result.response)
                        }
                    }
                    
                    // Stop collecting when all personas have responded
                    if (completedPersonas.size >= personas.size || results.none { it.isLoading }) {
                        return@collect
                    }
                }
                
                completedResponses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * Gathers follow-up responses from targeted personas
     * Requirements: 2.4, 6.4, 7.5
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun gatherFollowUpResponses(
        trialId: String,
        followUpQuestions: List<FollowUpQuestion>,
        personas: List<AgentPersona>,
        roundNumber: Int
    ): List<AgentResult> = withContext(Dispatchers.IO) {
        return@withContext withTimeoutOrNull(AGENT_TIMEOUT_MS * followUpQuestions.size) {
            try {
                val responses = mutableListOf<AgentResult>()
                
                // Process each follow-up question
                followUpQuestions.forEach { followUp ->
                    val targetPersona = personas.find { it.id == followUp.targetPersonaId }
                    if (targetPersona != null) {
                        try {
                            // Run the specific persona with the follow-up question
                            agentRunnerService.runAgents(followUp.question, listOf(targetPersona)).collect { results ->
                                // Take the final result when done loading
                                if (results.none { it.isLoading }) {
                                    val result = results.firstOrNull()
                                    if (result != null && result.error == null && result.response.isNotBlank()) {
                                        responses.add(result)
                                    }
                                    return@collect
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Continue with other questions even if one fails
                        }
                    }
                }
                
                responses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * Gathers follow-up responses with streaming - calls back as each agent completes
     * Requirements: 2.4, 4.1 - incremental display
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun gatherFollowUpResponsesStreaming(
        trialId: String,
        followUpQuestions: List<FollowUpQuestion>,
        personas: List<AgentPersona>,
        roundNumber: Int,
        onResponseReceived: suspend (personaId: String, response: String) -> Unit
    ): List<AgentResult> = withContext(Dispatchers.IO) {
        return@withContext withTimeoutOrNull(AGENT_TIMEOUT_MS * followUpQuestions.size) {
            try {
                val responses = mutableListOf<AgentResult>()
                val targetPersonas = followUpQuestions.mapNotNull { followUp ->
                    personas.find { it.id == followUp.targetPersonaId }
                }
                
                if (targetPersonas.isEmpty()) return@withTimeoutOrNull emptyList()
                
                // Build combined prompt for each persona
                val personaQuestions = followUpQuestions.associate { followUp ->
                    followUp.targetPersonaId to followUp.question
                }
                
                val completedPersonas = mutableSetOf<String>()
                
                // Run all target personas with streaming
                targetPersonas.forEach { persona ->
                    val question = personaQuestions[persona.id] ?: return@forEach
                    
                    agentRunnerService.runAgentsWithStreaming(question, listOf(persona)).collect { results ->
                        results.forEach { result ->
                            if (!result.isLoading && 
                                result.error == null && 
                                result.response.isNotBlank() &&
                                !completedPersonas.contains(result.personaId)) {
                                
                                completedPersonas.add(result.personaId)
                                responses.add(result)
                                onResponseReceived(result.personaId, result.response)
                            }
                        }
                        
                        if (results.none { it.isLoading }) {
                            return@collect
                        }
                    }
                }
                
                responses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } ?: emptyList()
    }
}