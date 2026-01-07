package com.example.the_jury.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.the_jury.model.AgentPersona
import com.example.the_jury.repo.PersonaRepository
import org.koin.compose.koinInject

data class AgentEditScreen(val personaId: String?) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PersonaRepository = koinInject()
        
        // Load existing data if editing
        val personas by repository.personas.collectAsState()
        val existingPersona = remember(personaId, personas) { 
            personas.find { it.id == personaId } 
        }

        var name by remember { mutableStateOf(existingPersona?.name ?: "") }
        var description by remember { mutableStateOf(existingPersona?.description ?: "") }
        var instruction by remember { mutableStateOf(existingPersona?.systemInstruction ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (personaId == null) "New Agent" else "Edit Agent") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("System Instruction (Prompt)") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("You are a helpful assistant...") }
                )
                
                Button(
                    onClick = {
                        if (personaId == null) {
                            // Create new
                             val newPersona = AgentPersona(
                                name = name,
                                description = description,
                                systemInstruction = instruction
                            )
                            repository.addPersona(newPersona)
                        } else {
                            // Update existing
                            val updatedPersona = AgentPersona(
                                id = personaId,
                                name = name,
                                description = description,
                                systemInstruction = instruction
                            )
                            repository.updatePersona(updatedPersona)
                        }
                        navigator.pop()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && instruction.isNotBlank()
                ) {
                    Text("Save Agent")
                }
                
                if (personaId != null) {
                    OutlinedButton(
                        onClick = {
                            repository.deletePersona(personaId)
                            navigator.pop()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Agent")
                    }
                }
            }
        }
    }
}
