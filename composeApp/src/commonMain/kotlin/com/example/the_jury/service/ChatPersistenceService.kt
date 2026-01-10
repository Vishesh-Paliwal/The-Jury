package com.example.the_jury.service

import com.example.the_jury.model.ChatMessage
import com.example.the_jury.model.Trial
import com.example.the_jury.model.TrialInteraction

/**
 * Service interface for persisting chat messages and trials to local storage.
 * Implements requirements 3.1, 3.2, 3.3, 3.4 for persistent chat storage.
 */
interface ChatPersistenceService {
    
    /**
     * Saves a chat message immediately to persistent storage.
     * Requirements: 3.1 - immediate storage of messages
     */
    suspend fun saveMessage(message: ChatMessage): Result<Unit>
    
    /**
     * Loads all chat history for a specific trial in chronological order.
     * Requirements: 3.2 - restore chat history on app restart
     * Requirements: 3.3 - maintain message order and timestamps
     */
    suspend fun loadChatHistory(trialId: String): Result<List<ChatMessage>>
    
    /**
     * Saves a trial with all its data to persistent storage.
     * Requirements: 3.1 - immediate storage
     * Requirements: 3.4 - handle data serialization
     */
    suspend fun saveTrial(trial: Trial): Result<Unit>
    
    /**
     * Loads all trials from persistent storage.
     * Requirements: 3.2 - restore data on app restart
     */
    suspend fun loadTrials(): Result<List<Trial>>
    
    /**
     * Saves a trial interaction to persistent storage.
     * Requirements: 3.1 - immediate storage
     * Requirements: 3.3 - maintain order and timestamps
     */
    suspend fun saveTrialInteraction(interaction: TrialInteraction): Result<Unit>
    
    /**
     * Loads all interactions for a specific trial in chronological order.
     * Requirements: 3.2 - restore data on app restart
     * Requirements: 3.3 - maintain message order and timestamps
     */
    suspend fun loadTrialInteractions(trialId: String): Result<List<TrialInteraction>>
    
    /**
     * Clears all chat history for a specific trial.
     * Utility method for cleanup operations.
     */
    suspend fun clearChatHistory(trialId: String): Result<Unit>
    
    /**
     * Clears all trial interactions for a specific trial.
     * Utility method for cleanup operations.
     */
    suspend fun clearTrialInteractions(trialId: String): Result<Unit>
    
    /**
     * Updates an existing chat message (useful for streaming updates).
     * Requirements: 3.1 - immediate storage updates
     */
    suspend fun updateMessage(message: ChatMessage): Result<Unit>
    
    /**
     * Gets a specific trial by ID.
     * Requirements: 3.2 - data retrieval
     */
    suspend fun getTrial(trialId: String): Result<Trial?>
}