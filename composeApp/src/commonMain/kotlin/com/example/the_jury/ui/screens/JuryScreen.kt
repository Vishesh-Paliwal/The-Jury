package com.example.the_jury.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.example.the_jury.model.*
import com.example.the_jury.repo.PersonaRepository
import com.example.the_jury.service.JuryService
import com.example.the_jury.service.AgentRunnerService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

class JuryScreen : Screen {
    @Composable
    override fun Content() {
        val repository: PersonaRepository = koinInject()
        val juryService: JuryService = koinInject()
        val agentRunnerService: AgentRunnerService = koinInject()
        val personas by repository.personas.collectAsState()
        val scope = rememberCoroutineScope()

        var juryQuestion by rememberSaveable { mutableStateOf("") }
        var currentTrialState by remember { mutableStateOf<TrialState?>(null) }
        var parallelResults by remember { mutableStateOf<List<AgentResult>>(emptyList()) }
        var isRunning by remember { mutableStateOf(false) }
        var executionMode by rememberSaveable { mutableStateOf(ExecutionMode.PARALLEL) }
        val parallelGridState = rememberLazyGridState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selection
            ModeSelectionCard(
                currentMode = executionMode,
                onModeChange = { executionMode = it },
                enabled = !isRunning
            )

            // Input Area
            OutlinedTextField(
                value = juryQuestion,
                onValueChange = { juryQuestion = it },
                label = { 
                    Text(
                        if (executionMode == ExecutionMode.JURY) 
                            "Enter your jury question for deliberation" 
                        else 
                            "Enter your idea or question"
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (isRunning) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (executionMode == ExecutionMode.JURY) {
                                    currentTrialState?.trial?.id?.let { trialId ->
                                        juryService.stopTrial(trialId)
                                    }
                                } else {
                                    // For parallel mode, we could implement cancellation if needed
                                    // For now, just reset the state
                                    parallelResults = emptyList()
                                }
                                isRunning = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Trial")
                    }
                } else {
                    Button(
                        onClick = {
                            if (juryQuestion.isNotBlank()) {
                                isRunning = true
                                scope.launch {
                                    if (executionMode == ExecutionMode.JURY) {
                                        juryService.conductTrial(juryQuestion, personas)
                                            .collect { trialState ->
                                                currentTrialState = trialState
                                                if (trialState.isComplete) {
                                                    isRunning = false
                                                }
                                            }
                                    } else {
                                        // Parallel mode - use AgentRunnerService
                                        agentRunnerService.runAgents(juryQuestion, personas)
                                            .collect { newResults ->
                                                parallelResults = newResults
                                                if (newResults.none { it.isLoading }) {
                                                    isRunning = false
                                                }
                                            }
                                    }
                                }
                            }
                        },
                        enabled = !isRunning && juryQuestion.isNotBlank()
                    ) {
                        Icon(
                            if (executionMode == ExecutionMode.JURY) Icons.Default.Gavel else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isRunning) {
                                if (executionMode == ExecutionMode.JURY) "The Jury is Deliberating..." else "The Jury is Thinking..."
                            } else {
                                if (executionMode == ExecutionMode.JURY) "Convene Jury" else "Summon The Jury"
                            }
                        )
                    }
                }
            }

            // Results Display
            if (executionMode == ExecutionMode.JURY) {
                // Trial Display for Jury mode
                currentTrialState?.let { trialState ->
                    TrialDisplay(
                        trialState = trialState,
                        personas = personas,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Parallel Results Grid for Parallel mode
                if (parallelResults.isNotEmpty()) {
                    LazyVerticalGrid(
                        state = parallelGridState,
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(items = parallelResults, key = { it.personaId }) { result ->
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
    }
}

@Composable
private fun ModeSelectionCard(
    currentMode: ExecutionMode,
    onModeChange: (ExecutionMode) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Execution Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Parallel Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = currentMode == ExecutionMode.PARALLEL,
                        onClick = { if (enabled) onModeChange(ExecutionMode.PARALLEL) },
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Parallel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Quick individual responses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Jury Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = currentMode == ExecutionMode.JURY,
                        onClick = { if (enabled) onModeChange(ExecutionMode.JURY) },
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Jury Deliberation",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Guided discussion with verdict",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialDisplay(
    trialState: TrialState,
    personas: List<AgentPersona>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new interactions are added
    LaunchedEffect(trialState.trial.interactions.size) {
        if (trialState.trial.interactions.isNotEmpty()) {
            listState.animateScrollToItem(trialState.trial.interactions.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Trial Status Header
        TrialStatusHeader(
            trialState = trialState,
            personas = personas
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Trial Transcript
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Trial Transcript",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = trialState.trial.interactions.sortedBy { it.timestamp },
                        key = { it.id }
                    ) { interaction ->
                        InteractionCard(
                            interaction = interaction,
                            personas = personas
                        )
                    }
                    
                    // Show thinking indicators for currently active personas
                    if (trialState.currentlyThinking.isNotEmpty()) {
                        item {
                            ThinkingIndicator(
                                thinkingPersonas = trialState.currentlyThinking,
                                personas = personas
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialStatusHeader(
    trialState: TrialState,
    personas: List<AgentPersona>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (trialState.trial.status) {
                TrialStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                TrialStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Trial Status: ${trialState.trial.status.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Question: ${trialState.trial.originalQuestion}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Participants: ${personas.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            if (trialState.trial.status == TrialStatus.COMPLETED) {
                Icon(
                    Icons.Default.Gavel,
                    contentDescription = "Trial Complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractionCard(
    interaction: TrialInteraction,
    personas: List<AgentPersona>
) {
    val isModeratorSpeaking = interaction.speaker == "moderator"
    val persona = if (!isModeratorSpeaking) {
        personas.find { it.name == interaction.speaker || it.id == interaction.speaker }
    } else null
    
    val backgroundColor = when (interaction.type) {
        InteractionType.INITIAL_QUESTION -> MaterialTheme.colorScheme.primaryContainer
        InteractionType.VERDICT -> MaterialTheme.colorScheme.tertiaryContainer
        InteractionType.FOLLOW_UP_QUESTION -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Speaker and timestamp header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isModeratorSpeaking) Icons.Default.Gavel else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isModeratorSpeaking) "Moderator" else (persona?.name ?: interaction.speaker),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = formatTimestamp(interaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Interaction type badge
            if (interaction.type != InteractionType.INITIAL_RESPONSE && interaction.type != InteractionType.FOLLOW_UP_RESPONSE) {
                Text(
                    text = interaction.type.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // Content
            Text(
                text = interaction.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Round indicator
            Text(
                text = "Round ${interaction.roundNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(
    thinkingPersonas: List<String>,
    personas: List<AgentPersona>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            val thinkingNames = thinkingPersonas.mapNotNull { personaId ->
                if (personaId == "moderator") "Moderator"
                else personas.find { it.id == personaId }?.name
            }
            
            Text(
                text = if (thinkingNames.size == 1) {
                    "${thinkingNames.first()} is thinking..."
                } else {
                    "${thinkingNames.joinToString(", ")} are thinking..."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
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