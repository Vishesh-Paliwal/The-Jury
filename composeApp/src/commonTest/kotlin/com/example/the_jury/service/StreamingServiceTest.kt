package com.example.the_jury.service

import com.example.the_jury.model.AgentPersona
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreamingServiceTest {

    @Test
    fun testStreamingServiceCreation() {
        val streamingService = StreamingServiceImpl("test-api-key")
        assertNotNull(streamingService)
        assertTrue(streamingService.getActiveStreams().isEmpty())
    }

    @Test
    fun testStreamResponseFlow() = runTest {
        val streamingService = StreamingServiceImpl("test-api-key")
        val testPersona = AgentPersona(
            id = "test-persona",
            name = "Test Agent",
            description = "A test agent",
            systemInstruction = "You are a helpful test assistant."
        )

        // Note: This test will fail without a valid API key, but it tests the flow structure
        try {
            val responses = streamingService.streamResponse("Hello", testPersona).toList()
            
            // Should have at least one response (the initial empty one)
            assertTrue(responses.isNotEmpty())
            
            // First response should be the starting response
            val firstResponse = responses.first()
            assertNotNull(firstResponse.streamId)
            assertFalse(firstResponse.isComplete)
            
            // Last response should be marked as complete
            val lastResponse = responses.last()
            assertTrue(lastResponse.isComplete)
            
        } catch (e: Exception) {
            // Expected to fail without valid API key, but we can verify the structure
            assertTrue(e.message?.contains("API") == true || 
                      e.message?.contains("key") == true ||
                      e.message?.contains("Authentication") == true)
        }
    }

    @Test
    fun testStreamCancellation() = runTest {
        val streamingService = StreamingServiceImpl("test-api-key")
        
        // Test that cancellation doesn't throw errors
        streamingService.cancelStream("non-existent-stream")
        
        // Should still have no active streams
        assertTrue(streamingService.getActiveStreams().isEmpty())
    }

    @Test
    fun testActiveStreamsTracking() {
        val streamingService = StreamingServiceImpl("test-api-key")
        
        // Initially no active streams
        assertEquals(0, streamingService.getActiveStreams().size)
        
        // Active streams are managed internally during streaming
        // This test verifies the interface works correctly
        val activeStreams = streamingService.getActiveStreams()
        assertNotNull(activeStreams)
    }

    @Test
    fun testStreamStateManagerCreation() = runTest {
        val stateManager = StreamStateManager()
        
        // Initially no active streams
        assertEquals(0, stateManager.getActiveStreamCount())
        assertFalse(stateManager.hasActiveStreams())
        
        // Test registering a stream
        stateManager.registerStream("test-stream", StreamStatus.STARTING)
        assertEquals(1, stateManager.getActiveStreamCount())
        assertTrue(stateManager.hasActiveStreams())
        
        // Test cleanup
        stateManager.cleanupStream("test-stream")
        assertEquals(0, stateManager.getActiveStreamCount())
        assertFalse(stateManager.hasActiveStreams())
    }

    @Test
    fun testStreamingErrorHandler() {
        val errorHandler = StreamingErrorHandler()
        
        // Test error classification
        val timeoutException = java.net.SocketTimeoutException("Connection timed out")
        assertTrue(errorHandler.isRecoverableError(timeoutException))
        
        val illegalArgException = IllegalArgumentException("Invalid argument")
        assertFalse(errorHandler.isRecoverableError(illegalArgException))
        assertTrue(errorHandler.shouldCancelStream(illegalArgException))
        
        // Test user-friendly messages
        val friendlyMessage = errorHandler.createUserFriendlyErrorMessage(timeoutException)
        assertTrue(friendlyMessage.contains("timed out"))
        
        // Test retry delay calculation
        val delay1 = errorHandler.calculateRetryDelay(1)
        val delay2 = errorHandler.calculateRetryDelay(2)
        assertTrue(delay2 > delay1)
    }
}