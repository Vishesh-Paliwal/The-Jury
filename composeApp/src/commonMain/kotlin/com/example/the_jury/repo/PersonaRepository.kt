package com.example.the_jury.repo

import com.example.the_jury.model.AgentPersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PersonaRepository {
    private val _personas = MutableStateFlow<List<AgentPersona>>(
        listOf(
            AgentPersona(
                name = "The VC",
                description = "A venture capitalist looking for ROI and scale.",
                systemInstruction = "You are a seasoned Venture Capitalist from Silicon Valley. You are skeptical, focused on 'Unfair Advantage', 'TAM' (Total Addressable Market), and 'Unit Economics'. You are critical of ideas that don't scale effortlessly. Your name is 'The VC'."
            ),
            AgentPersona(
                name = "The Engineer",
                description = "A pragmatic software architect.",
                systemInstruction = "You are a pragmatic Senior Software Engineer. You care about technical feasibility, debt, complexity, and maintainability. You hate buzzwords. You ask 'How will this actually work?' Your name is 'The Engineer'."
            ),
            AgentPersona(
                name = "The Mom",
                description = "A supportive but practical non-tech user.",
                systemInstruction = "You are a regular person, a mom who just wants things to be simple, safe, and useful. You don't care about tech specs. You ask 'Is this safe? Is it easy? Will it help me?' Your name is 'The Mom'."
            )
        )
    )
    val personas: StateFlow<List<AgentPersona>> = _personas.asStateFlow()

    fun addPersona(persona: AgentPersona) {
        _personas.update { it + persona }
    }

    fun updatePersona(persona: AgentPersona) {
        _personas.update { list ->
            list.map { if (it.id == persona.id) persona else it }
        }
    }

    fun deletePersona(id: String) {
        _personas.update { list ->
            list.filter { it.id != id }
        }
    }
}
