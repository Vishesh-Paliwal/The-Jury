package com.example.the_jury.service

import com.example.the_jury.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TrialService(
    private val chatPersistenceService: ChatPersistenceService
) {
    // In-memory cache for trials - backed by persistent storage
    private val _trials = MutableStateFlow<Map<String, Trial>>(emptyMap())
    private val trials: StateFlow<Map<String, Trial>> = _trials.asStateFlow()
    
    // Loading state for chat restoration
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state for persistence operations
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Initializes the service by loading existing trials from persistence
     * Requirements: 3.2 - restore chat history on app startup
     */
    suspend fun initialize() {
        _isLoading.value = true
        _error.value = null
        
        try {
            val result = chatPersistenceService.loadTrials()
            result.fold(
                onSuccess = { loadedTrials ->
                    val trialsMap = loadedTrials.associateBy { it.id }
                    _trials.value = trialsMap
                },
                onFailure = { exception ->
                    _error.value = "Failed to load trials: ${exception.message}"
                }
            )
        } catch (e: Exception) {
            _error.value = "Failed to initialize trials: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Creates a new trial session with the given question and personas
     * Requirements: 1.1, 1.4, 3.1 - immediate storage
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
        
        // Save to persistence layer immediately
        val saveResult = chatPersistenceService.saveTrial(trial)
        saveResult.fold(
            onSuccess = {
                // Update in-memory cache
                _trials.update { currentTrials ->
                    currentTrials + (trial.id to trial)
                }
            },
            onFailure = { exception ->
                _error.value = "Failed to save trial: ${exception.message}"
                throw exception
            }
        )
        
        return trial
    }
    
    /**
     * Adds an interaction to an existing trial
     * Requirements: 3.1, 3.2, 3.5 - immediate storage and persistence
     */
    suspend fun addInteraction(trialId: String, interaction: TrialInteraction): Trial? {
        var updatedTrial: Trial? = null
        
        // Save interaction to persistence layer first
        val saveResult = chatPersistenceService.saveTrialInteraction(interaction)
        saveResult.fold(
            onSuccess = {
                // Update in-memory cache
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
            },
            onFailure = { exception ->
                _error.value = "Failed to save interaction: ${exception.message}"
                return null
            }
        )
        
        return updatedTrial
    }
    
    /**
     * Updates the status of a trial
     * Requirements: 1.4, 3.1 - immediate storage
     */
    suspend fun updateTrialStatus(trialId: String, status: TrialStatus): Trial? {
        var updatedTrial: Trial? = null
        
        // Update in-memory cache first
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                updatedTrial = trial.copy(status = status)
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        // Save updated status to persistence layer using efficient update
        if (updatedTrial != null) {
            val persistenceService = chatPersistenceService as? ChatPersistenceServiceImpl
            val saveResult = if (persistenceService != null) {
                // Use efficient status update if available
                persistenceService.updateTrialStatus(trialId, status)
            } else {
                // Fallback to full trial save
                chatPersistenceService.saveTrial(updatedTrial!!)
            }
            
            saveResult.fold(
                onSuccess = { /* Success - trial status saved */ },
                onFailure = { exception ->
                    _error.value = "Failed to save trial status: ${exception.message}"
                }
            )
        }
        
        return updatedTrial
    }
    
    /**
     * Completes a trial with a final verdict
     * Requirements: 1.4, 3.1 - immediate storage
     */
    suspend fun completeTrial(trialId: String, verdict: String): Trial? {
        val completedAt = System.currentTimeMillis()
        var updatedTrial: Trial? = null
        
        // Update in-memory cache first
        _trials.update { currentTrials ->
            val trial = currentTrials[trialId]
            if (trial != null) {
                updatedTrial = trial.copy(
                    status = TrialStatus.COMPLETED,
                    verdict = verdict,
                    completedAt = completedAt
                )
                currentTrials + (trialId to updatedTrial!!)
            } else {
                currentTrials
            }
        }
        
        // Save completed trial to persistence layer using efficient update
        if (updatedTrial != null) {
            val persistenceService = chatPersistenceService as? ChatPersistenceServiceImpl
            val saveResult = if (persistenceService != null) {
                // Use efficient verdict update if available
                persistenceService.updateTrialVerdict(trialId, verdict, completedAt)
            } else {
                // Fallback to full trial save
                chatPersistenceService.saveTrial(updatedTrial!!)
            }
            
            saveResult.fold(
                onSuccess = { /* Success - trial completed */ },
                onFailure = { exception ->
                    _error.value = "Failed to save completed trial: ${exception.message}"
                }
            )
        }
        
        return updatedTrial
    }
    
    /**
     * Gets a trial by ID
     * Requirements: 3.2 - data retrieval from persistence
     */
    suspend fun getTrial(trialId: String): Trial? {
        // First check in-memory cache
        val cachedTrial = _trials.value[trialId]
        if (cachedTrial != null) {
            return cachedTrial
        }
        
        // If not in cache, try to load from persistence
        val result = chatPersistenceService.getTrial(trialId)
        return result.fold(
            onSuccess = { trial ->
                // Update cache if found
                trial?.let { 
                    _trials.update { currentTrials ->
                        currentTrials + (trialId to it)
                    }
                }
                trial
            },
            onFailure = { exception ->
                _error.value = "Failed to load trial: ${exception.message}"
                null
            }
        )
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
     * Requirements: 3.2 - data retrieval from persistence
     */
    suspend fun getAllTrials(): List<Trial> {
        // Return from in-memory cache if available
        val cachedTrials = _trials.value.values.toList()
        if (cachedTrials.isNotEmpty()) {
            return cachedTrials
        }
        
        // Otherwise load from persistence
        val result = chatPersistenceService.loadTrials()
        return result.fold(
            onSuccess = { trials ->
                // Update cache
                val trialsMap = trials.associateBy { it.id }
                _trials.value = trialsMap
                trials
            },
            onFailure = { exception ->
                _error.value = "Failed to load all trials: ${exception.message}"
                emptyList()
            }
        )
    }
    
    /**
     * Marks a trial as failed with error information
     * Requirements: 7.5 (error handling), 3.1 - immediate storage
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
        
        // Save failed trial to persistence layer
        updatedTrial?.let { trial ->
            // Save the error interaction first
            val errorInteraction = trial.interactions.last()
            chatPersistenceService.saveTrialInteraction(errorInteraction)
            
            // Then save the updated trial
            val saveResult = chatPersistenceService.saveTrial(trial)
            saveResult.fold(
                onSuccess = { /* Success - trial saved */ },
                onFailure = { exception ->
                    _error.value = "Failed to save failed trial: ${exception.message}"
                }
            )
        }
        
        return updatedTrial
    }
    
    /**
     * Clears error state
     */
    fun clearError() {
        _error.value = null
    }
}