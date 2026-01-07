package com.example.the_jury.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.example.the_jury.model.AgentResult
import com.example.the_jury.repo.PersonaRepository
import com.example.the_jury.service.AgentRunnerService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class ExecutionScreen : Screen {
    @Composable
    override fun Content() {
        val repository: PersonaRepository = koinInject()
        val runnerService: AgentRunnerService = koinInject()
        val personas by repository.personas.collectAsState()
        val scope = rememberCoroutineScope()

        var prompt by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<AgentResult>>(emptyList()) }
        var isRunning by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Area
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Enter your idea or question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        isRunning = true
                        scope.launch {
                            runnerService.runAgents(prompt, personas)
                                .collect { newResults ->
                                    results = newResults
                                    if (newResults.none { it.isLoading }) {
                                        isRunning = false
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isRunning && prompt.isNotBlank()
            ) {
                Text(if (isRunning) "The Jury is Thinking..." else "Summon The Jury")
            }

            // Results Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = results, key = { it.personaId }) { result ->
                    val persona = personas.find { it.id == result.personaId }
                    AgentResultCard(
                        agentName = persona?.name ?: "Unknown Agent",
                        result = result
                    )
                }
            }
        }
    }
}

@Composable
fun AgentResultCard(agentName: String, result: AgentResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = agentName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (result.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Thinking...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=4.dp))
            } else if (result.error != null) {
                 Text(
                    text = "Error: ${result.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = result.response,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
