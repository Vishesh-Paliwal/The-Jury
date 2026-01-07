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
