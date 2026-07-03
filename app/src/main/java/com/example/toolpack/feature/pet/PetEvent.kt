package com.example.toolpack.feature.pet

import androidx.compose.ui.geometry.Offset

sealed class PetEvent {
    // Gesture events
    data class OnTap(val position: Offset) : PetEvent()
    data class OnDoubleTap(val position: Offset) : PetEvent()
    data class OnLongPress(val position: Offset) : PetEvent()
    data class OnDrag(val delta: Offset) : PetEvent()
    data class OnDragEnd(val offset: Offset) : PetEvent()
    
    // Chat events
    data class OnChatInputChanged(val text: String) : PetEvent()
    data class OnSendMessage(val text: String) : PetEvent()
    data class OnBotMessage(val text: String) : PetEvent()
    // LLM 请求生命周期事件
    object OnLLMLoading : PetEvent()
    data class OnLLMError(val message: String) : PetEvent()
    
    // UI events
    object OnToolkitClicked : PetEvent()
    object OnKeyboardVisible : PetEvent()
    object OnKeyboardHidden : PetEvent()
    data class OnScreenSizeAvailable(val width: Float, val height: Float, val petSize: Float) : PetEvent()
}