# Android Desktop Pet & Toolkit App Design Specification

## 1. Overview
**Project**: MVP Android application with interactive Live2D desktop pet and toolkit UI placeholder.

**Selected Approach**: Hybrid – Live2D Library + MVI Architecture + Mock LLM (Approach 3)

**Core Features**:
- Live2D animated pet confined within app boundaries
- Gesture interactions: tap, double-tap, long-press drag
- Chat interface with mock LLM responses
- Toolkit button placeholder with "开发中" toast
- MVI (Model-View-Intent) unidirectional data flow

## 2. Architecture

### 2.1 MVI Pattern
```
┌─────────────────┐    Events    ┌──────────────┐    State    ┌────────────┐
│   UI Composables │ ───────────> │   ViewModel  │ ──────────> │   Reducer  │
│                 │               │              │             │            │
│   State Updates │ <──────────── │  Side Effects│ <────────── │   State    │
└─────────────────┘               └──────────────┘             └────────────┘
```

### 2.2 Component Hierarchy
- **MainActivity**: Single Activity with Jetpack Compose
- **PetViewModel**: Manages state and processes events
- **PetState**: Immutable data class representing UI state
- **PetEvent**: Sealed class for all user interactions
- **PetReducer**: Pure function for state transitions

## 3. State Management

### 3.1 State Definition
```kotlin
data class PetState(
    // Pet positioning and animation
    val petPosition: Offset = calculateDefaultPosition(),
    val petAnimationState: AnimationState = AnimationState.IDLE,
    val isDragging: Boolean = false,
    
    // Chat UI state
    val chatVisible: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val keyboardVisible: Boolean = false,
    
    // UI visibility
    val toolkitButtonVisible: Boolean = true
)

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
```

### 3.2 Event System
```kotlin
sealed class PetEvent {
    // Gesture events
    data class OnTap(val position: Offset) : PetEvent()
    data class OnDoubleTap(val position: Offset) : PetEvent()
    data class OnLongPress(val position: Offset) : PetEvent()
    data class OnDrag(val offset: Offset) : PetEvent()
    data class OnDragEnd(val offset: Offset) : PetEvent()
    
    // Chat events
    data class OnChatInputChanged(val text: String) : PetEvent()
    data class OnSendMessage(val text: String) : PetEvent()
    
    // UI events
    object OnToolkitClicked : PetEvent()
    object OnKeyboardVisible : PetEvent()
    object OnKeyboardHidden : PetEvent()
}
```

### 3.3 Reducer Function
```kotlin
fun petReducer(state: PetState, event: PetEvent): PetState {
    return when (event) {
        is PetEvent.OnTap -> state.copy(
            petAnimationState = AnimationState.INTERACTION
        )
        is PetEvent.OnDoubleTap -> state.copy(
            chatVisible = true,
            keyboardVisible = true
        )
        is PetEvent.OnLongPress -> state.copy(
            isDragging = true,
            petAnimationState = AnimationState.DRAG
        )
        is PetEvent.OnDrag -> state.copy(
            petPosition = event.offset
        )
        is PetEvent.OnDragEnd -> state.copy(
            isDragging = false,
            petAnimationState = AnimationState.IDLE
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
        PetEvent.OnToolkitClicked -> state // Side effect handled in ViewModel
        PetEvent.OnKeyboardVisible -> state.copy(keyboardVisible = true)
        PetEvent.OnKeyboardHidden -> state.copy(keyboardVisible = false)
    }
}
```

## 4. Live2D Integration

### 4.1 Library Selection
**Primary Option**: `live2d-android` third-party library (GitHub)
**Fallback Options**:
1. Custom wrapper around Live2D Cubism Core
2. WebView with Live2D web SDK
3. Frame-based animation simulation

### 4.2 Model Management
- Load `.moc3` model files from `assets/live2d/` directory
- Support for texture files (`.png`) and motion files (`.json`)
- Model lifecycle: load on demand, unload on background

### 4.3 Live2DView Composable
```kotlin
@Composable
fun Live2DView(
    state: AnimationState,
    position: Offset,
    onEvent: (PetEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(position.x.dp, position.y.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onEvent(PetEvent.OnTap(it)) },
                    onDoubleTap = { onEvent(PetEvent.OnDoubleTap(it)) },
                    onLongPress = { onEvent(PetEvent.OnLongPress(it)) }
                )
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    onEvent(PetEvent.OnDrag(Offset(delta, 0f)))
                },
                onDragStarted = { /* Long press already handled */ },
                onDragStopped = { onEvent(PetEvent.OnDragEnd(state.petPosition)) }
            )
    ) {
        // Third-party Live2D view integration
        AndroidView(
            factory = { context ->
                Live2DView(context).apply {
                    loadModel("shizuku.moc3")
                }
            }
        )
    }
}
```

## 5. UI Components

### 5.1 Screen Layout
```kotlin
@Composable
fun PetAppScreen(viewModel: PetViewModel) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desktop Pet") },
                actions = {
                    ToolkitButton(
                        visible = state.toolkitButtonVisible,
                        onClick = { viewModel.processEvent(PetEvent.OnToolkitClicked) }
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background
            Background()
            
            // Live2D Pet
            Live2DView(
                state = state.petAnimationState,
                position = state.petPosition,
                onEvent = viewModel::processEvent
            )
            
            // Chat UI
            if (state.chatVisible) {
                ChatBubble(
                    messages = state.chatMessages,
                    position = state.petPosition,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                
                if (state.keyboardVisible) {
                    ChatInput(
                        text = state.inputText,
                        onTextChange = { text ->
                            viewModel.processEvent(PetEvent.OnChatInputChanged(text))
                        },
                        onSend = { text ->
                            viewModel.processEvent(PetEvent.OnSendMessage(text))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
```

### 5.2 Gesture Resolution
**Priority Order**:
1. **Long-press** (>300ms): Triggers drag mode with haptic feedback
2. **Double-tap** (max 500ms between taps): Opens chat interface
3. **Single-tap**: Plays interaction animation

**Gesture Conflict Resolution**:
- Long-press detection cancels tap detection
- Double-tap timer resets on second tap timeout
- Drag movement overrides other animations

## 6. Mock LLM Service

### 6.1 Implementation
```kotlin
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
        return responses.random()
    }
}
```

### 6.2 Integration with ViewModel
```kotlin
class PetViewModel(
    private val llmService: MockLLMService
) : ViewModel() {
    // ... state management
    
    fun processEvent(event: PetEvent) {
        viewModelScope.launch {
            when (event) {
                is PetEvent.OnSendMessage -> {
                    // Add user message
                    val newState = petReducer(state.value, event)
                    _state.value = newState
                    
                    // Generate mock response
                    val response = llmService.generateResponse(event.text)
                    val botMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _state.value = state.value.copy(
                        chatMessages = state.value.chatMessages + botMessage
                    )
                }
                // ... other events
            }
        }
    }
}
```

## 7. Toolkit Placeholder

### 7.1 Implementation
```kotlin
@Composable
fun ToolkitButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        IconButton(onClick = onClick) {
            Text("工具箱")
        }
    }
}

// In ViewModel side effect
PetEvent.OnToolkitClicked -> {
    // Show toast
    showToast("开发中")
    state // No state change
}
```

## 8. Dependencies

### 8.1 build.gradle.kts Additions
```kotlin
dependencies {
    // Live2D library (to be determined after research)
    implementation("com.github.someuser:live2d-android:1.0.0")
    
    // MVI and Architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    
    // Gesture detection
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
}
```

## 9. Package Structure
```
com.example.toolpack
├── ui
│   ├── MainActivity.kt
│   ├── PetAppScreen.kt
│   └── components
│       ├── Live2DView.kt
│       ├── ChatComponents.kt
│       │   ├── ChatBubble.kt
│       │   ├── ChatInput.kt
│       │   └── MessageList.kt
│       ├── ToolkitButton.kt
│       └── Background.kt
├── data
│   ├── model
│   │   ├── PetState.kt
│   │   ├── PetEvent.kt
│   │   ├── ChatMessage.kt
│   │   └── AnimationState.kt
│   └── repository
│       ├── MockLLMRepository.kt
│       └── Live2DModelLoader.kt
├── domain
│   ├── reducer
│   │   └── PetReducer.kt
│   └── usecase
│       ├── GestureUseCase.kt
│       └── ChatUseCase.kt
└── presentation
    └── PetViewModel.kt
```

## 10. Testing Strategy

### 10.1 Unit Tests
- **PetReducerTest**: Verify state transitions for all events
- **PetViewModelTest**: Test ViewModel logic and side effects
- **GestureUseCaseTest**: Validate gesture timing and conflict resolution

### 10.2 UI Tests
- **GestureDetectionTest**: Tap, double-tap, long-press recognition
- **ChatUITest**: Input field visibility, message display
- **ToolkitTest**: Toast display on button click

### 10.3 Acceptance Test Mapping
| AC from spec.md | Test Case |
|-----------------|-----------|
| AC-01: Startup State | Verify pet position at bottom-right |
| AC-02: Drag Operation | Long press triggers drag with haptic |
| AC-03: Chat Trigger | Double tap opens chat with keyboard |
| AC-04: Simple Interaction | Single tap changes animation state |
| AC-05: Chat UI Positioning | Chat bubble anchored to pet left |
| AC-06: Loading State | Pet remains idle during mock LLM delay |
| AC-07: Keyboard Occlusion | Input box visible when keyboard overlaps |
| AC-08: Feature Gating | Toolkit button shows "开发中" toast |

## 11. Risk Mitigation

### 11.1 Technical Risks
1. **Live2D Library Availability**: Fallback to frame-based animation
2. **Gesture Conflict**: Clear priority system with timing thresholds
3. **Performance**: Background thread for LLM, efficient state updates

### 11.2 Scope Management
- MVP focuses on core pet interaction
- Mock LLM allows deferral of real API integration
- Toolkit functionality limited to UI placeholder

## 12. Success Criteria
- [ ] Live2D pet renders and animates
- [ ] All three gestures work correctly
- [ ] Chat interface opens on double-tap
- [ ] Mock LLM responses appear after delay
- [ ] Toolkit button shows correct toast
- [ ] All acceptance criteria from spec.md met
- [ ] App runs at 60fps during interactions