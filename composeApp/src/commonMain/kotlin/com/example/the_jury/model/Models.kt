package com.example.the_jury.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class AgentPersona(
    val id: String = Uuid.random().toString(),
    val name: String,
    val description: String,
    val systemInstruction: String
)

@Serializable
data class AgentResult(
    val personaId: String,
    val response: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Jury System Data Models

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Trial(
    val id: String = Uuid.random().toString(),
    val originalQuestion: String,
    val personas: List<AgentPersona>,
    val interactions: List<TrialInteraction> = emptyList(),
    val status: TrialStatus = TrialStatus.INITIALIZING,
    val verdict: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class TrialInteraction(
    val id: String = Uuid.random().toString(),
    val trialId: String,
    val type: InteractionType,
    val speaker: String, // "moderator" or persona name
    val content: String,
    val targetPersona: String? = null, // for directed questions
    val timestamp: Long = System.currentTimeMillis(),
    val roundNumber: Int
)

@Serializable
enum class TrialStatus {
    INITIALIZING,
    GATHERING_INITIAL_RESPONSES,
    DELIBERATING,
    GENERATING_VERDICT,
    COMPLETED,
    FAILED
}

@Serializable
enum class InteractionType {
    INITIAL_QUESTION,
    INITIAL_RESPONSE,
    FOLLOW_UP_QUESTION,
    FOLLOW_UP_RESPONSE,
    VERDICT
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class FollowUpQuestion(
    val id: String = Uuid.random().toString(),
    val question: String,
    val targetPersonaId: String,
    val reasoning: String
)

@Serializable
data class TrialState(
    val trial: Trial,
    val currentlyThinking: List<String> = emptyList(), // persona IDs
    val isComplete: Boolean = false
)

@Serializable
enum class ExecutionMode {
    PARALLEL,
    JURY
}
