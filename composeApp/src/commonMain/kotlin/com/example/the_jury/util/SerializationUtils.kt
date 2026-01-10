package com.example.the_jury.util

import com.example.the_jury.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Utility object for handling data serialization and deserialization.
 * Implements requirement 3.4 - handle data serialization/deserialization correctly.
 */
object SerializationUtils {
    
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // AgentPersona serialization
    fun serializePersonas(personas: List<AgentPersona>): String {
        return json.encodeToString(personas)
    }
    
    fun deserializePersonas(jsonString: String): List<AgentPersona> {
        return json.decodeFromString(jsonString)
    }
    
    // TrialStatus serialization
    fun serializeTrialStatus(status: TrialStatus): String {
        return status.name
    }
    
    fun deserializeTrialStatus(statusString: String): TrialStatus {
        return TrialStatus.valueOf(statusString)
    }
    
    // InteractionType serialization
    fun serializeInteractionType(type: InteractionType): String {
        return type.name
    }
    
    fun deserializeInteractionType(typeString: String): InteractionType {
        return InteractionType.valueOf(typeString)
    }
    
    // MessageType serialization
    fun serializeMessageType(type: MessageType): String {
        return type.name
    }
    
    fun deserializeMessageType(typeString: String): MessageType {
        return MessageType.valueOf(typeString)
    }
    
    // StreamingState serialization
    fun serializeStreamingState(state: StreamingState): String {
        return state.name
    }
    
    fun deserializeStreamingState(stateString: String): StreamingState {
        return StreamingState.valueOf(stateString)
    }
    
    // Generic JSON serialization for complex objects
    inline fun <reified T> serialize(obj: T): String {
        return json.encodeToString(obj)
    }
    
    inline fun <reified T> deserialize(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }
    
    // Safe deserialization with error handling
    inline fun <reified T> safeDeserialize(jsonString: String): Result<T> {
        return try {
            Result.success(json.decodeFromString<T>(jsonString))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Safe serialization with error handling
    inline fun <reified T> safeSerialization(obj: T): Result<String> {
        return try {
            Result.success(json.encodeToString(obj))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}