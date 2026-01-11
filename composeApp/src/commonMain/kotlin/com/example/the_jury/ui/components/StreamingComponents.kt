package com.example.the_jury.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.the_jury.model.AgentPersona
import com.example.the_jury.model.AgentResult

/**
 * Streaming message card that displays incremental text as it arrives.
 * Requirements: 4.3 - loading indicators for active streams
 * Requirements: 4.2, 4.4 - error recovery and message integrity
 */
@Composable
fun StreamingAgentResultCard(
    agentName: String,
    result: AgentResult,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                result.error != null -> MaterialTheme.colorScheme.errorContainer
                result.isLoading -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Agent header with streaming indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = agentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Streaming status indicator
                if (result.isLoading) {
                    StreamingIndicator()
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content area
            when {
                result.error != null -> {
                    StreamingErrorIndicator(
                        error = result.error,
                        onRetry = onRetry
                    )
                }
                result.response.isNotEmpty() -> {
                    StreamingText(
                        text = result.response,
                        isStreaming = result.isLoading
                    )
                }
                result.isLoading -> {
                    StreamingPlaceholder()
                }
            }
        }
    }
}

/**
 * Animated streaming indicator for active streams.
 * Requirements: 4.3 - loading indicators for active streams
 */
@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (index == 0) alpha else if (index == 1) alpha * 0.7f else alpha * 0.4f
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Streaming...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Text component that shows streaming text with a typing cursor effect.
 * Requirements: 4.1 - display text incrementally as it arrives
 */
@Composable
private fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val displayText = if (isStreaming && text.isNotEmpty()) {
        text + TypingCursor()
    } else {
        text
    }
    
    Text(
        text = displayText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/**
 * Animated typing cursor for streaming text.
 */
@Composable
private fun TypingCursor(): String {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val visible by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_visibility"
    )
    
    return if (visible > 0.5f) "▋" else ""
}

/**
 * Placeholder component shown while waiting for streaming to start.
 */
@Composable
private fun StreamingPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "placeholder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "placeholder_alpha"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 2) 0.6f else 1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
            )
        }
    }
}

/**
 * Compact streaming indicator for use in lists or smaller spaces.
 */
@Composable
fun CompactStreamingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_streaming")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    CircularProgressIndicator(
        modifier = modifier.size(16.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Error indicator for streaming failures.
 * Requirements: 4.2, 4.4 - error recovery and message integrity
 */
@Composable
fun StreamingErrorIndicator(
    error: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Streaming Error",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            if (onRetry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}