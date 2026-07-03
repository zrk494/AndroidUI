package com.example.toolpack.feature.pet

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Clamps pet position to stay within screen bounds.
 */
fun clampToScreenBounds(offset: Offset, screenWidth: Float, screenHeight: Float, petSize: Float): Offset {
    if (screenWidth == 0f || screenHeight == 0f || petSize == 0f) return offset
    val minX = 0f
    val maxX = screenWidth - petSize
    val minY = 0f
    val maxY = screenHeight - petSize
    
    val clampedX = offset.x.coerceIn(minX, maxX)
    val clampedY = offset.y.coerceIn(minY, maxY)
    
    return Offset(clampedX, clampedY)
}

fun petReducer(state: PetState, event: PetEvent): PetState {
    return when (event) {
        is PetEvent.OnTap -> state.copy(
            petAnimationState = AnimationState.INTERACTION,
            petAlpha = 0.7f
        )
        is PetEvent.OnDoubleTap -> {
            val newChatVisible = !state.chatVisible
            state.copy(
                chatVisible = newChatVisible,
                keyboardVisible = newChatVisible
            )
        }
        is PetEvent.OnLongPress -> state.copy(
            isDragging = true,
            petAnimationState = AnimationState.DRAG,
            petScale = 1.1f
        )
        is PetEvent.OnDrag -> {
            val newPosition = state.petPosition + event.delta
            state.copy(
                petPosition = clampToScreenBounds(newPosition, state.screenWidth, state.screenHeight, state.petSize)
            )
        }
        is PetEvent.OnDragEnd -> state.copy(
            isDragging = false,
            petAnimationState = AnimationState.IDLE,
            petScale = 1.0f,
            petAlpha = 1.0f
        )
        is PetEvent.OnChatInputChanged -> state.copy(
            inputText = event.text
        )
        is PetEvent.OnSendMessage -> {
            val newMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = event.text,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            state.copy(
                chatMessages = state.chatMessages + newMessage,
                inputText = ""
            )
        }
        is PetEvent.OnBotMessage -> {
            val newMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = event.text,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            state.copy(
                chatMessages = state.chatMessages + newMessage,
                isLoading = false,
                errorMessage = null
            )
        }
        PetEvent.OnLLMLoading -> state.copy(isLoading = true, errorMessage = null)
        is PetEvent.OnLLMError -> state.copy(isLoading = false, errorMessage = event.message)
        PetEvent.OnToolkitClicked -> state // Side effect handled in ViewModel
        PetEvent.OnKeyboardVisible -> state.copy(keyboardVisible = true)
        PetEvent.OnKeyboardHidden -> state.copy(keyboardVisible = false)
        is PetEvent.OnScreenSizeAvailable -> {
            val newScreenWidth = event.width
            val newScreenHeight = event.height
            val newPetSize = event.petSize
            
            // Calculate new pet position: if current position is zero (initial), use default bottom-right
            // Otherwise, clamp existing position to new screen bounds
            val newPetPosition = if (state.petPosition == Offset.Zero) {
                // Temporary state to calculate default position
                state.copy(screenWidth = newScreenWidth, screenHeight = newScreenHeight, petSize = newPetSize)
                    .calculateDefaultPosition()
            } else {
                clampToScreenBounds(state.petPosition, newScreenWidth, newScreenHeight, newPetSize)
            }
            
            state.copy(
                screenWidth = newScreenWidth,
                screenHeight = newScreenHeight,
                petSize = newPetSize,
                petPosition = newPetPosition
            )
        }
    }
}