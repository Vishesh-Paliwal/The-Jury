package com.example.the_jury.service

import com.example.the_jury.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TrialService {
    // In-memory storage for trials - in a real app this would be persisted
    private val _trials = MutableStateFlow<Map<String, Trial>>(emptyMap())
    private val trials: StateFlow<Map<String, Trial>> = _trials.asStateFlow()
    
    /**
     * Creates a new trial session with the given question and personas
     * Requirements: 1.1, 1.4, 3.1
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun createTrial(question: String, personas: List<AgentPersona>): Trial {
        val trial = Trial(
            id = Uuid.random().toString(),
            originalQuestion = question,
            personas = personas,
            interactions = emptyList(),
            status = TrialStatus.INITIALIZING,
            verdict = null,
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )
        
        _trials.update { currentTrials ->
            currentTrials + (trial.id to trial)
        }
        
        return trial
    }
    
    /**
     * Adds an interaction to an existing trial
     * Requirements: 3.1, 3.2, 3.5
     */
    suspend fun addInteraction(trialId: String, interaction: TrialInteraction): Trial? {
        var updatedTrial: Trial? = null
        
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                updatedTrial = trial.copy(
                    interactions = trial.interactions + interaction
                )
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        return updatedTrial
    }
    
    /**
     * Updates the status of a trial
     * Requirements: 1.4, 3.1
     */
    suspend fun updateTrialStatus(trialId: String, status: TrialStatus): Trial? {
        var updatedTrial: Trial? = null
        
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                updatedTrial = trial.copy(status = status)
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        return updatedTrial
    }
    
    /**
     * Completes a trial with a final verdict
     * Requirements: 1.4, 3.1
     */
    suspend fun completeTrial(trialId: String, verdict: String): Trial? {
        var updatedTrial: Trial? = null
        
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                updatedTrial = trial.copy(
                    status = TrialStatus.COMPLETED,
                    verdict = verdict,
                    completedAt = System.currentTimeMillis()
                )
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        return updatedTrial
    }
    
    /**
     * Gets a trial by ID
     */
    suspend fun getTrial(trialId: String): Trial? {
        return _trials.value[trialId]
    }
    
    /**
     * Gets a flow of trial state for real-time updates
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
     */
    fun getTrialFlow(trialId: String): StateFlow<Trial?> {
        val trialFlow = MutableStateFlow<Trial?>(null)
        
        // Update the flow whenever the trial changes
        _trials.value[trialId]?.let { trial ->
            trialFlow.value = trial
        }
        
        // Listen for updates to this specific trial
        // Note: In a more sophisticated implementation, we'd use a proper reactive approach
        // For now, this provides the basic flow interface
        return trialFlow.asStateFlow()
    }
    
    /**
     * Gets all trials (for debugging/admin purposes)
     */
    suspend fun getAllTrials(): List<Trial> {
        return _trials.value.values.toList()
    }
    
    /**
     * Marks a trial as failed with error information
     * Requirements: 7.5 (error handling)
     */
    suspend fun failTrial(trialId: String, errorMessage: String): Trial? {
        var updatedTrial: Trial? = null
        
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                // Add error interaction to the trial
                val errorInteraction = TrialInteraction(
                    trialId = trialId,
                    type = InteractionType.VERDICT, // Using VERDICT type for error messages
                    speaker = "system",
                    content = "Trial failed: $errorMessage",
                    timestamp = System.currentTimeMillis(),
                    roundNumber = trial.interactions.maxOfOrNull { it.roundNumber } ?: 0
                )
                
                updatedTrial = trial.copy(
                    status = TrialStatus.FAILED,
                    interactions = trial.interactions + errorInteraction,
                    completedAt = System.currentTimeMillis()
                )
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        return updatedTrial
    }
}