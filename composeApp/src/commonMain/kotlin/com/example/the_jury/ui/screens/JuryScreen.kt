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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.foundation.clickable
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
import com.example.the_jury.service.TrialService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.example.the_jury.ui.components.StreamingAgentResultCard
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
        val trialService: TrialService = koinInject()
        val personas by repository.personas.collectAsState()
        val scope = rememberCoroutineScope()

        var juryQuestion by rememberSaveable { mutableStateOf("") }
        var currentTrialState by remember { mutableStateOf<TrialState?>(null) }
        var parallelResults by remember { mutableStateOf<List<AgentResult>>(emptyList()) }
        var isRunning by remember { mutableStateOf(false) }
        var executionMode by rememberSaveable { mutableStateOf(ExecutionMode.PARALLEL) }
        val parallelGridState = rememberLazyGridState()
        
        // Past trials state
        var pastTrials by remember { mutableStateOf<List<Trial>>(emptyList()) }
        var showPastTrials by rememberSaveable { mutableStateOf(false) }
        var selectedPastTrial by remember { mutableStateOf<Trial?>(null) }
        
        // Full page transcript state
        var isTranscriptFullPage by rememberSaveable { mutableStateOf(false) }
        
        // Observe loading and error states from JuryService
        // Requirements: 3.5 - loading states during data retrieval
        val isServiceLoading by juryService.isLoading.collectAsState()
        val serviceError by juryService.error.collectAsState()
        var hasInitialized by remember { mutableStateOf(false) }
        
        // Initialize JuryService on first composition to load existing trials
        // Requirements: 3.2 - restore chat history on app startup
        LaunchedEffect(Unit) {
            if (!hasInitialized) {
                juryService.initialize()
                hasInitialized = true
                // Load past trials
                pastTrials = trialService.getAllTrials()
            }
        }
        
        // Show loading state during initialization
        if (!hasInitialized || isServiceLoading) {
            LoadingScreen(message = "Loading trial history...")
            return
        }
        
        // Show error state if initialization failed
        serviceError?.let { error ->
            ErrorScreen(
                error = error,
                onRetry = {
                    juryService.clearError()
                    scope.launch {
                        juryService.initialize()
                        pastTrials = trialService.getAllTrials()
                    }
                },
                onDismiss = { juryService.clearError() }
            )
            return
        }

        // Check if we have active results to show
        val hasActiveResults = (executionMode == ExecutionMode.JURY && currentTrialState != null) ||
                              (executionMode == ExecutionMode.PARALLEL && parallelResults.isNotEmpty())

        // Show full page transcript if enabled
        if (isTranscriptFullPage && currentTrialState != null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                FullPageTranscript(
                    trialState = currentTrialState!!,
                    personas = personas,
                    onClose = { isTranscriptFullPage = false }
                )
            }
            return
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Collapsible input section - shrinks when trial is running/has results
            if (!hasActiveResults) {
                // Full input section when no results
                
                // Past Trials Section (collapsible)
                if (pastTrials.isNotEmpty()) {
                    PastTrialsSection(
                        pastTrials = pastTrials,
                        isExpanded = showPastTrials,
                        onToggleExpand = { showPastTrials = !showPastTrials },
                        onSelectTrial = { trial ->
                            selectedPastTrial = trial
                            currentTrialState = TrialState(trial = trial, isComplete = true)
                            executionMode = ExecutionMode.JURY
                        },
                        personas = personas
                    )
                }
                
                // Mode Selection
                ModeSelectionCard(
                    currentMode = executionMode,
                    onModeChange = { 
                        executionMode = it
                        if (it == ExecutionMode.PARALLEL) {
                            selectedPastTrial = null
                            currentTrialState = null
                        }
                    },
                    enabled = !isRunning
                )

                // Input Area - fixed height
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
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4
                )

                // Action Button
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
                                                pastTrials = trialService.getAllTrials()
                                            }
                                        }
                                } else {
                                    agentRunnerService.runAgentsWithStreaming(juryQuestion, personas)
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
                    enabled = !isRunning && juryQuestion.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        if (executionMode == ExecutionMode.JURY) Icons.Default.Gavel else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (executionMode == ExecutionMode.JURY) "Convene Jury" else "Summon The Jury")
                }
            } else {
                // Compact header when results are showing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (executionMode == ExecutionMode.JURY) "Jury Deliberation" else "Parallel Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = juryQuestion.take(80) + if (juryQuestion.length > 80) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isRunning) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (executionMode == ExecutionMode.JURY) {
                                                    currentTrialState?.trial?.id?.let { trialId ->
                                                        juryService.stopTrial(trialId)
                                                    }
                                                } else {
                                                    agentRunnerService.cancelStreaming(personas)
                                                    parallelResults = emptyList()
                                                }
                                                isRunning = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        currentTrialState = null
                                        parallelResults = emptyList()
                                        selectedPastTrial = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("New Trial", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Results Display - takes all remaining space
            if (executionMode == ExecutionMode.JURY) {
                currentTrialState?.let { trialState ->
                    TrialDisplay(
                        trialState = trialState,
                        personas = personas,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onToggleFullPage = { isTranscriptFullPage = true }
                    )
                }
            } else {
                if (parallelResults.isNotEmpty()) {
                    LazyVerticalGrid(
                        state = parallelGridState,
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(items = parallelResults, key = { it.personaId }) { result ->
                            val persona = personas.find { it.id == result.personaId }
                            StreamingAgentResultCard(
                                agentName = persona?.name ?: "Unknown Agent",
                                result = result,
                                onRetry = if (result.error != null && persona != null) {
                                    {
                                        scope.launch {
                                            agentRunnerService.retryAgentStreaming(juryQuestion, persona)
                                                .collect { retryResult ->
                                                    parallelResults = parallelResults.map { existing ->
                                                        if (existing.personaId == retryResult.personaId) retryResult else existing
                                                    }
                                                }
                                        }
                                    }
                                } else null
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
    modifier: Modifier = Modifier,
    onToggleFullPage: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new interactions are added
    LaunchedEffect(trialState.trial.interactions.size) {
        if (trialState.trial.interactions.isNotEmpty()) {
            listState.animateScrollToItem(trialState.trial.interactions.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Compact Trial Status Header
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
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trialState.trial.status.name.replace('_', ' '),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${trialState.trial.interactions.size} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Trial Transcript - takes remaining space
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trial Transcript",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onToggleFullPage,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "View Full Page",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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

/**
 * Full page view of the trial transcript for better readability
 */
@Composable
private fun FullPageTranscript(
    trialState: TrialState,
    personas: List<AgentPersona>,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new interactions are added
    LaunchedEffect(trialState.trial.interactions.size) {
        if (trialState.trial.interactions.isNotEmpty()) {
            listState.animateScrollToItem(trialState.trial.interactions.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with close button
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trial Transcript - Full View",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status: ${trialState.trial.status.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Question: ${trialState.trial.originalQuestion}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Participants: ${personas.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.FullscreenExit,
                        contentDescription = "Exit Full Page",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transcript content
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

/**
 * Loading screen for data restoration
 * Requirements: 3.5 - loading indicators during data retrieval
 */
@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error screen for handling empty state and error conditions
 * Requirements: 3.5 - handle empty state and error conditions
 */
@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Gavel,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Initialization Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Continue Anyway")
                    }
                    
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}


/**
 * Collapsible section showing past trials
 * Requirements: 3.2 - restore chat history on app startup
 */
@Composable
private fun PastTrialsSection(
    pastTrials: List<Trial>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectTrial: (Trial) -> Unit,
    personas: List<AgentPersona>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // Header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Past Trials",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Past Trials (${pastTrials.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            // Expandable content
            if (isExpanded) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = pastTrials.sortedByDescending { it.createdAt },
                        key = { it.id }
                    ) { trial ->
                        PastTrialItem(
                            trial = trial,
                            onClick = { onSelectTrial(trial) },
                            personas = personas
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastTrialItem(
    trial: Trial,
    onClick: () -> Unit,
    personas: List<AgentPersona>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when (trial.status) {
                TrialStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                TrialStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trial.originalQuestion.take(50) + if (trial.originalQuestion.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                // Status badge
                Text(
                    text = trial.status.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (trial.status) {
                        TrialStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        TrialStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (trial.status) {
                                TrialStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                TrialStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${trial.interactions.size} interactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(trial.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
