package com.example.toolpack.feature.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolpack.feature.chat.service.MockLLMService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PetViewModel(
    private val llmService: MockLLMService = MockLLMService()
) : ViewModel() {
    private val _state = MutableStateFlow(PetState())
    val state: StateFlow<PetState> = _state.asStateFlow()

    fun onEvent(event: PetEvent) {
        viewModelScope.launch {
            // Update state using reducer
            val newState = petReducer(_state.value, event)
            _state.value = newState
            
            // Handle side effects based on event
            handleSideEffects(event)
        }
    }
    
    private fun handleSideEffects(event: PetEvent) {
        when (event) {
            PetEvent.OnToolkitClicked -> {
                // Show toast "开发中"
                // For now, just log; will be implemented with Toast in UI layer
                println("Toolkit clicked: 开发中")
            }
            is PetEvent.OnLongPress -> {
                // Trigger haptic feedback
                // For now, just log
                println("Long press haptic feedback")
            }
            is PetEvent.OnSendMessage -> {
                // Generate mock LLM response after delay
                viewModelScope.launch {
                    try {
                        val response = llmService.generateResponse(event.text)
                        if (response.isNotEmpty()) {
                            onEvent(PetEvent.OnBotMessage(response))
                        }
                    } catch (e: Exception) {
                        // Log error; optionally show error message
                        println("LLM error: ${e.message}")
                    }
                }
            }
            else -> {
                // No side effect
            }
        }
    }
}