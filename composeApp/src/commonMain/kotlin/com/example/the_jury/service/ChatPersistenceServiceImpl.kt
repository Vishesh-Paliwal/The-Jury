package com.example.the_jury.service

import com.example.the_jury.database.JuryDatabase
import com.example.the_jury.database.DatabaseDriverFactory
import com.example.the_jury.model.*
import com.example.the_jury.util.SerializationUtils
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * SQLite implementation of ChatPersistenceService.
 * Handles persistent storage of chat messages and trials using SQLDelight.
 * Implements requirements 3.1, 3.2, 3.3, 3.4.
 */
class ChatPersistenceServiceImpl(
    databaseDriverFactory: DatabaseDriverFactory
) : ChatPersistenceService {
    
    private val database = JuryDatabase(databaseDriverFactory.createDriver())
    private val trialQueries = database.trialQueries
    private val chatMessageQueries = database.chatMessageQueries
    private val trialInteractionQueries = database.trialInteractionQueries
    
    override suspend fun saveMessage(message: ChatMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatMessageQueries.insertOrReplace(
                id = message.id,
                trialId = message.trialId,
                sender = message.sender,
                content = message.content,
                timestamp = message.timestamp,
                messageType = SerializationUtils.serializeMessageType(message.messageType),
                streamingState = SerializationUtils.serializeStreamingState(message.streamingState)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadChatHistory(trialId: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val messages = chatMessageQueries.selectByTrialId(trialId).executeAsList()
            val chatMessages = messages.map { row ->
                ChatMessage(
                    id = row.id,
                    trialId = row.trialId,
                    sender = row.sender,
                    content = row.content,
                    timestamp = row.timestamp,
                    messageType = SerializationUtils.deserializeMessageType(row.messageType),
                    streamingState = SerializationUtils.deserializeStreamingState(row.streamingState)
                )
            }
            Result.success(chatMessages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveTrial(trial: Trial): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val personasJson = SerializationUtils.serializePersonas(trial.personas)
            val statusString = SerializationUtils.serializeTrialStatus(trial.status)
            
            // Use INSERT OR REPLACE to handle both new and existing trials
            trialQueries.insertOrReplace(
                id = trial.id,
                originalQuestion = trial.originalQuestion,
                personas = personasJson,
                status = statusString,
                verdict = trial.verdict,
                createdAt = trial.createdAt,
                completedAt = trial.completedAt
            )
            
            // Save all interactions (only new ones that don't exist yet)
            trial.interactions.forEach { interaction ->
                saveTrialInteraction(interaction).getOrThrow()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadTrials(): Result<List<Trial>> = withContext(Dispatchers.IO) {
        try {
            val trialRows = trialQueries.selectAll().executeAsList()
            val trials = trialRows.map { row ->
                val personas = SerializationUtils.deserializePersonas(row.personas)
                val status = SerializationUtils.deserializeTrialStatus(row.status)
                val interactions = loadTrialInteractions(row.id).getOrElse { emptyList() }
                
                Trial(
                    id = row.id,
                    originalQuestion = row.originalQuestion,
                    personas = personas,
                    interactions = interactions,
                    status = status,
                    verdict = row.verdict,
                    createdAt = row.createdAt,
                    completedAt = row.completedAt
                )
            }
            Result.success(trials)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveTrialInteraction(interaction: TrialInteraction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use INSERT OR REPLACE to handle duplicates gracefully
            trialInteractionQueries.insertOrReplace(
                id = interaction.id,
                trialId = interaction.trialId,
                type = SerializationUtils.serializeInteractionType(interaction.type),
                speaker = interaction.speaker,
                content = interaction.content,
                targetPersona = interaction.targetPersona,
                timestamp = interaction.timestamp,
                roundNumber = interaction.roundNumber.toLong()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadTrialInteractions(trialId: String): Result<List<TrialInteraction>> = withContext(Dispatchers.IO) {
        try {
            val interactionRows = trialInteractionQueries.selectByTrialId(trialId).executeAsList()
            val interactions = interactionRows.map { row ->
                TrialInteraction(
                    id = row.id,
                    trialId = row.trialId,
                    type = SerializationUtils.deserializeInteractionType(row.type),
                    speaker = row.speaker,
                    content = row.content,
                    targetPersona = row.targetPersona,
                    timestamp = row.timestamp,
                    roundNumber = row.roundNumber.toInt()
                )
            }
            Result.success(interactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearChatHistory(trialId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatMessageQueries.deleteByTrialId(trialId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearTrialInteractions(trialId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            trialInteractionQueries.deleteByTrialId(trialId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateMessage(message: ChatMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatMessageQueries.updateContent(
                content = message.content,
                streamingState = SerializationUtils.serializeStreamingState(message.streamingState),
                id = message.id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates only the status of a trial (more efficient than full trial update)
     * Requirements: 3.1 - immediate storage
     */
    suspend fun updateTrialStatus(trialId: String, status: TrialStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val statusString = SerializationUtils.serializeTrialStatus(status)
            trialQueries.updateStatus(status = statusString, id = trialId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates the verdict and completes a trial (more efficient than full trial update)
     * Requirements: 3.1 - immediate storage
     */
    suspend fun updateTrialVerdict(trialId: String, verdict: String, completedAt: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val statusString = SerializationUtils.serializeTrialStatus(TrialStatus.COMPLETED)
            trialQueries.updateVerdict(
                verdict = verdict,
                status = statusString,
                completedAt = completedAt,
                id = trialId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTrial(trialId: String): Result<Trial?> = withContext(Dispatchers.IO) {
        try {
            val trialRow = trialQueries.selectById(trialId).executeAsOneOrNull()
            if (trialRow == null) {
                return@withContext Result.success(null)
            }
            
            val personas = SerializationUtils.deserializePersonas(trialRow.personas)
            val status = SerializationUtils.deserializeTrialStatus(trialRow.status)
            val interactions = loadTrialInteractions(trialRow.id).getOrElse { emptyList() }
            
            val trial = Trial(
                id = trialRow.id,
                originalQuestion = trialRow.originalQuestion,
                personas = personas,
                interactions = interactions,
                status = status,
                verdict = trialRow.verdict,
                createdAt = trialRow.createdAt,
                completedAt = trialRow.completedAt
            )
            
            Result.success(trial)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}