package com.example.toolpack.feature.chat.service

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.random.Random

class MockLLMService {
    private val responses = listOf(
        "Hello! I'm your desktop pet.",
        "Nice to meet you!",
        "How can I help you today?",
        "The weather looks great!",
        "Let's have a conversation!"
    )
    
    suspend fun generateResponse(input: String): String {
        delay(1000) // Simulate network delay
        
        // Simulate random network failures (10% chance)
        if (Random.nextFloat() < 0.1f) {
            throw IOException("Network error: Failed to connect to LLM service")
        }
        
        // Simulate empty response (5% chance)
        if (Random.nextFloat() < 0.05f) {
            return "" // Empty response to test error handling
        }
        
        return responses.random()
    }
}