package com.example.toolpack.feature.pet

import androidx.compose.ui.geometry.Offset

data class PetState(
    // Pet positioning and animation
    val petPosition: Offset = Offset.Zero,
    val petAnimationState: AnimationState = AnimationState.IDLE,
    val isDragging: Boolean = false,
    
    // Visual feedback states
    val petScale: Float = 1.0f,
    val petAlpha: Float = 1.0f,
    
    // Screen dimensions for boundary calculations
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    val petSize: Float = 0f, // Pet size in pixels
    
    // Chat UI state
    val chatVisible: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val keyboardVisible: Boolean = false,
    // LLM 请求状态：loading 期间禁用输入并显示加载气泡，errorMessage 非空时显示错误气泡
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // UI visibility
    val toolkitButtonVisible: Boolean = true
) {
    /**
     * Calculates default position for pet (bottom-right of screen).
     */
    fun calculateDefaultPosition(): Offset {
        if (screenWidth == 0f || screenHeight == 0f || petSize == 0f) return Offset(100f, 100f) // Fallback
        // Position pet at bottom-right, considering pet size
        return Offset(screenWidth - petSize, screenHeight - petSize)
    }
}

sealed class AnimationState {
    object IDLE : AnimationState()
    object INTERACTION : AnimationState()
    object DRAG : AnimationState()
    data class Live2DModel(val modelName: String) : AnimationState()
}

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)