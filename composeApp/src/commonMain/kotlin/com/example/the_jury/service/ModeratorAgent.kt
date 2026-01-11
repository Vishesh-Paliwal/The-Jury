package com.example.the_jury.service

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.example.the_jury.model.AgentPersona
import com.example.the_jury.model.AgentResult
import com.example.the_jury.model.FollowUpQuestion
import com.example.the_jury.model.TrialInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class ModeratorAgent(
    private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private fun createModeratorAgent(systemPrompt: String): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            llmModel = GoogleModels.Gemini2_5Flash,
            systemPrompt = systemPrompt
        )
    }

    /**
     * Helper to get persona name from ID
     */
    private fun getPersonaName(personaId: String, personas: List<AgentPersona>): String {
        return personas.find { it.id == personaId }?.name ?: personaId
    }

    /**
     * Helper to get persona ID from name (for parsing AI responses)
     */
    private fun getPersonaId(nameOrId: String, personas: List<AgentPersona>): String {
        // First check if it's already an ID
        personas.find { it.id == nameOrId }?.let { return it.id }
        // Then check by name
        personas.find { it.name.equals(nameOrId, ignoreCase = true) }?.let { return it.id }
        // Return as-is if not found
        return nameOrId
    }

    /**
     * Generates follow-up questions based on initial responses from personas
     * Requirements: 2.2, 6.4
     */
    suspend fun generateFollowUpQuestions(
        originalQuestion: String,
        responses: List<AgentResult>,
        personas: List<AgentPersona> = emptyList()
    ): List<FollowUpQuestion> = withContext(Dispatchers.IO) {
        // Build persona name list for the prompt
        val personaNames = personas.map { it.name }
        
        val systemPrompt = """
            You are an intelligent moderator facilitating a jury deliberation. Your role is to analyze responses from different personas and generate targeted follow-up questions to clarify, resolve conflicts, or gather missing information.

            Guidelines:
            1. Analyze responses for conflicts, gaps, or areas needing clarification
            2. Generate specific, targeted questions rather than general ones
            3. Focus on areas where personas disagree or provide incomplete information
            4. Avoid redundant questions already answered
            5. Ask for specific examples or clarification when responses are vague
            6. Identify contradictions with previous context and request explanations
            7. IMPORTANT: Use the exact persona NAMES provided, not IDs

            Available personas: ${personaNames.joinToString(", ")}

            Your response should be a JSON array of follow-up questions in this format:
            [
                {
                    "question": "Specific question text",
                    "targetPersonaName": "exact persona name from the list above",
                    "reasoning": "Why this question is needed"
                }
            ]

            If no follow-up questions are needed, return an empty array: []
        """.trimIndent()

        val agent = createModeratorAgent(systemPrompt)
        
        val prompt = buildString {
            appendLine("Original Question: $originalQuestion")
            appendLine("\nPersona Responses:")
            responses.forEach { result ->
                if (result.error == null && result.response.isNotBlank()) {
                    val personaName = getPersonaName(result.personaId, personas)
                    appendLine("- $personaName: ${result.response}")
                }
            }
            appendLine("\nAnalyze these responses and generate follow-up questions if needed. Use the exact persona names in your response.")
        }

        try {
            val response = agent.run(prompt)
            // Parse the JSON response to extract follow-up questions
            parseFollowUpQuestions(response, personas)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Determines if deliberation should continue based on current interactions and round count
     * Requirements: 2.6
     */
    suspend fun shouldContinueDeliberation(
        interactions: List<TrialInteraction>,
        roundCount: Int,
        personas: List<AgentPersona> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        // Hard limit of 5 rounds as per requirements
        if (roundCount >= 5) return@withContext false
        
        val systemPrompt = """
            You are an intelligent moderator determining whether a jury deliberation should continue or if sufficient information has been gathered for a final verdict.

            Guidelines:
            1. Consider if there are unresolved conflicts between personas
            2. Check if important questions remain unanswered
            3. Evaluate if responses are sufficiently detailed and specific
            4. Determine if additional rounds would provide valuable insights
            5. Remember that deliberation is limited to 5 rounds maximum

            Respond with either "CONTINUE" or "STOP" followed by a brief reasoning.
        """.trimIndent()

        val agent = createModeratorAgent(systemPrompt)
        
        val prompt = buildString {
            appendLine("Current Round: $roundCount/5")
            appendLine("\nTrial Interactions:")
            interactions.forEach { interaction ->
                val speakerName = if (interaction.speaker == "moderator") "Moderator" 
                                  else getPersonaName(interaction.speaker, personas)
                appendLine("- $speakerName (Round ${interaction.roundNumber}): ${interaction.content}")
            }
            appendLine("\nShould deliberation continue?")
        }

        try {
            val response = agent.run(prompt)
            response.trim().startsWith("CONTINUE", ignoreCase = true)
        } catch (e: Exception) {
            e.printStackTrace()
            false // Default to stopping on error
        }
    }

    /**
     * Synthesizes all interactions into a comprehensive final verdict
     * Requirements: 4.1
     */
    suspend fun synthesizeVerdict(
        originalQuestion: String,
        allInteractions: List<TrialInteraction>,
        personas: List<AgentPersona> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an intelligent moderator synthesizing a final verdict from a jury deliberation. Your role is to create a comprehensive answer that addresses the original question by incorporating insights from all personas.

            Guidelines:
            1. Address the original question directly and comprehensively
            2. Reference key points from different personas BY NAME
            3. Present different viewpoints with reasoning when personas disagree
            4. Consider how the answer relates to any initial context provided
            5. Provide a clear, well-reasoned conclusion
            6. Structure the verdict logically with clear reasoning

            Format your verdict as a comprehensive response that synthesizes all perspectives while directly answering the original question.
        """.trimIndent()

        val agent = createModeratorAgent(systemPrompt)
        
        val prompt = buildString {
            appendLine("Original Question: $originalQuestion")
            appendLine("\nComplete Trial Transcript:")
            allInteractions.forEach { interaction ->
                val speakerName = if (interaction.speaker == "moderator") "Moderator" 
                                  else getPersonaName(interaction.speaker, personas)
                appendLine("$speakerName (Round ${interaction.roundNumber}, ${interaction.type}): ${interaction.content}")
            }
            appendLine("\nSynthesize all perspectives into a comprehensive final verdict that addresses the original question. Reference personas by their names.")
        }

        try {
            agent.run(prompt)
        } catch (e: Exception) {
            e.printStackTrace()
            "Unable to generate verdict due to an error: ${e.message}"
        }
    }

    private fun parseFollowUpQuestions(response: String, personas: List<AgentPersona>): List<FollowUpQuestion> {
        return try {
            // Try to extract JSON from the response
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = response.substring(jsonStart, jsonEnd)
                val questions = json.decodeFromString<List<Map<String, String>>>(jsonString)
                
                questions.mapNotNull { questionMap ->
                    val question = questionMap["question"]
                    // Support both old "targetPersonaId" and new "targetPersonaName" fields
                    val targetPersonaNameOrId = questionMap["targetPersonaName"] 
                        ?: questionMap["targetPersonaId"]
                    val reasoning = questionMap["reasoning"]
                    
                    if (question != null && targetPersonaNameOrId != null && reasoning != null) {
                        // Convert name to ID for internal use
                        val targetPersonaId = getPersonaId(targetPersonaNameOrId, personas)
                        FollowUpQuestion(
                            question = question,
                            targetPersonaId = targetPersonaId,
                            reasoning = reasoning
                        )
                    } else null
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}