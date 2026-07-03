package com.example.toolpack.feature.pet

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PetReducer 纯函数单元测试。
 *
 * 覆盖手势状态切换(为 spec-2 Live2DView 重写提供 reducer 层安全网)、
 * clampToScreenBounds 边界处理(含早返分支)、聊天状态、OnScreenSizeAvailable。
 */
class PetReducerTest {

    private fun initialState(
        screenWidth: Float = 1000f,
        screenHeight: Float = 2000f,
        petSize: Float = 300f,
        petPosition: Offset = Offset(700f, 1700f) // 右下角默认
    ) = PetState(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        petSize = petSize,
        petPosition = petPosition
    )

    // --- Bug 2 reducer 层安全网: 手势状态切换 ---

    @Test
    fun `OnLongPress enters drag mode with scale`() {
        val state = initialState()
        val next = petReducer(state, PetEvent.OnLongPress(Offset.Zero))
        assertTrue(next.isDragging)
        assertEquals(1.1f, next.petScale)
        assertEquals(AnimationState.DRAG, next.petAnimationState)
    }

    @Test
    fun `OnDragEnd exits drag mode and resets visual`() {
        val dragging = initialState().copy(isDragging = true, petScale = 1.1f, petAlpha = 0.7f)
        val next = petReducer(dragging, PetEvent.OnDragEnd(Offset.Zero))
        assertFalse(next.isDragging)
        assertEquals(1.0f, next.petScale)
        assertEquals(1.0f, next.petAlpha)
        assertEquals(AnimationState.IDLE, next.petAnimationState)
    }

    @Test
    fun `OnTap sets interaction state and alpha`() {
        val state = initialState()
        val next = petReducer(state, PetEvent.OnTap(Offset.Zero))
        assertEquals(AnimationState.INTERACTION, next.petAnimationState)
        assertEquals(0.7f, next.petAlpha)
    }

    // --- clampToScreenBounds 边界处理 ---

    @Test
    fun `OnDrag clamps position to screen bounds`() {
        val state = initialState(
            screenWidth = 1000f, screenHeight = 2000f, petSize = 300f,
            petPosition = Offset(950f, 1900f) // 已在右下角
        )
        // 向右下拖 100px,应被 clamp
        val next = petReducer(state, PetEvent.OnDrag(Offset(100f, 100f)))
        // maxX = 1000-300 = 700, maxY = 2000-300 = 1700
        assertEquals(700f, next.petPosition.x, 0.01f)
        assertEquals(1700f, next.petPosition.y, 0.01f)
    }

    @Test
    fun `OnDrag moves position within bounds`() {
        val state = initialState(
            screenWidth = 1000f, screenHeight = 2000f, petSize = 300f,
            petPosition = Offset(100f, 100f)
        )
        val next = petReducer(state, PetEvent.OnDrag(Offset(50f, 50f)))
        assertEquals(150f, next.petPosition.x, 0.01f)
        assertEquals(150f, next.petPosition.y, 0.01f)
    }

    @Test
    fun `clampToScreenBounds returns offset unchanged when screen dims are zero`() {
        // 早返分支:screenWidth/Height/petSize 为 0
        val offset = Offset(500f, 500f)
        val result = clampToScreenBounds(offset, screenWidth = 0f, screenHeight = 0f, petSize = 0f)
        assertEquals(offset, result)
    }

    // --- 聊天状态 ---

    @Test
    fun `OnDoubleTap toggles chatVisible`() {
        val state = initialState().copy(chatVisible = false)
        val next = petReducer(state, PetEvent.OnDoubleTap(Offset.Zero))
        assertTrue(next.chatVisible)
        assertTrue(next.keyboardVisible)

        val next2 = petReducer(next, PetEvent.OnDoubleTap(Offset.Zero))
        assertFalse(next2.chatVisible)
    }

    @Test
    fun `OnSendMessage appends user message and clears input`() {
        val state = initialState().copy(inputText = "hello")
        val next = petReducer(state, PetEvent.OnSendMessage("hello"))
        assertEquals(1, next.chatMessages.size)
        assertEquals("hello", next.chatMessages[0].text)
        assertTrue(next.chatMessages[0].isUser)
        assertEquals("", next.inputText)
    }

    @Test
    fun `OnBotMessage appends bot message`() {
        val state = initialState()
        val next = petReducer(state, PetEvent.OnBotMessage("hi"))
        assertEquals(1, next.chatMessages.size)
        assertEquals("hi", next.chatMessages[0].text)
        assertFalse(next.chatMessages[0].isUser)
        // 成功收到回复后应清除 loading 和 error
        assertFalse(next.isLoading)
        assertNull(next.errorMessage)
    }

    @Test
    fun `OnLLMLoading sets isLoading true and clears error`() {
        val state = initialState().copy(errorMessage = "old error")
        val next = petReducer(state, PetEvent.OnLLMLoading)
        assertTrue(next.isLoading)
        assertNull(next.errorMessage)
    }

    @Test
    fun `OnLLMError sets errorMessage and clears loading`() {
        val state = initialState().copy(isLoading = true)
        val next = petReducer(state, PetEvent.OnLLMError("网络错误"))
        assertFalse(next.isLoading)
        assertEquals("网络错误", next.errorMessage)
    }

    @Test
    fun `OnBotMessage clears loading and error`() {
        val state = initialState().copy(isLoading = true, errorMessage = "err")
        val next = petReducer(state, PetEvent.OnBotMessage("reply"))
        assertFalse(next.isLoading)
        assertNull(next.errorMessage)
    }

    // --- OnScreenSizeAvailable ---

    @Test
    fun `OnScreenSizeAvailable sets default position when current is zero`() {
        val state = PetState() // petPosition = Offset.Zero
        val next = petReducer(state, PetEvent.OnScreenSizeAvailable(1000f, 2000f, 300f))
        assertEquals(1000f, next.screenWidth, 0.01f)
        assertEquals(2000f, next.screenHeight, 0.01f)
        assertEquals(300f, next.petSize, 0.01f)
        // 默认右下角:1000-300=700, 2000-300=1700
        assertEquals(700f, next.petPosition.x, 0.01f)
        assertEquals(1700f, next.petPosition.y, 0.01f)
    }

    @Test
    fun `OnScreenSizeAvailable clamps existing position to new bounds`() {
        val state = PetState(
            petPosition = Offset(950f, 1950f),
            screenWidth = 1000f, screenHeight = 2000f, petSize = 300f
        )
        // 屏幕变小
        val next = petReducer(state, PetEvent.OnScreenSizeAvailable(800f, 1500f, 300f))
        // maxX = 800-300 = 500, maxY = 1500-300 = 1200
        assertEquals(500f, next.petPosition.x, 0.01f)
        assertEquals(1200f, next.petPosition.y, 0.01f)
    }
}
