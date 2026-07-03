package com.example.toolpack.feature.pet.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolpack.feature.chat.ui.ChatBubble
import com.example.toolpack.feature.chat.ui.ChatInput
import com.example.toolpack.feature.pet.PetEvent
import com.example.toolpack.feature.pet.PetViewModel

/**
 * 宠物主界面
 *
 * 该组件是应用的主界面，包含以下功能：
 * 1. 顶部导航栏，显示应用标题和工具箱按钮
 * 2. Live2D宠物视图，支持点击、双击、长按和拖拽操作
 * 3. 聊天界面，当双击宠物时显示，支持发送和接收消息
 * 4. 屏幕尺寸检测，用于计算宠物和聊天窗口的位置
 *
 * @param viewModel 宠物视图模型，管理宠物状态和行为
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PetScreen(
    viewModel: PetViewModel = viewModel()
) {
    // 收集宠物状态
    val state by viewModel.state.collectAsState()
    // 屏幕密度，用于像素和dp之间的转换
    val density = LocalDensity.current
    // 宠物尺寸（像素）
    val petSizePx = with(density) { 120.dp.toPx() }
    // 屏幕尺寸状态
    var boxSize by remember { mutableStateOf(Size.Zero) }

    // 当屏幕尺寸可用时，通知ViewModel
    LaunchedEffect(boxSize) {
        if (boxSize.width > 0 && boxSize.height > 0) {
            viewModel.onEvent(PetEvent.OnScreenSizeAvailable(boxSize.width, boxSize.height, petSizePx))
        }
    }

    // 键盘可见性检测
    val keyboardBottom: Int = with(density) { WindowInsets.ime.getBottom(this) }
    val keyboardVisible = keyboardBottom > 0
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            viewModel.onEvent(PetEvent.OnKeyboardVisible)
        } else {
            viewModel.onEvent(PetEvent.OnKeyboardHidden)
        }
    }

    // 脚手架布局，包含顶部导航栏
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ToolPack") },
                actions = {
                    // 工具箱按钮
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            // 触发工具箱点击事件
                            viewModel.onEvent(PetEvent.OnToolkitClicked)
                            // 显示"开发中"提示
                            Toast.makeText(context, "开发中", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("工具箱")
                    }
                }
            )
        }
    ) { innerPadding ->
        // 主容器，用于测量屏幕尺寸
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { size ->
                    boxSize = size.toSize()
                }
        ) {
            // 只在屏幕尺寸数据加载完成后才显示宠物
            if (state.screenWidth > 0 && state.screenHeight > 0 && state.petSize > 0) {
                Live2DView(
                    position = state.petPosition,
                    isDragging = state.isDragging,
                    petScale = state.petScale,
                    petAlpha = state.petAlpha,
                    onTap = { viewModel.onEvent(PetEvent.OnTap(it)) },
                    onDoubleTap = { viewModel.onEvent(PetEvent.OnDoubleTap(it)) },
                    onLongPress = { viewModel.onEvent(PetEvent.OnLongPress(it)) },
                    onDrag = { viewModel.onEvent(PetEvent.OnDrag(it)) },
                    onDragEnd = { viewModel.onEvent(PetEvent.OnDragEnd(it)) },
                    modifier = Modifier
                )
            }

            // 聊天界面（当可见时显示）
            if (state.chatVisible) {
                // 聊天窗口配置
                val chatWidth = 280.dp
                val chatPadding = 8.dp
                val petSize = 120.dp
                val petXDp = state.petPosition.x.dp
                val petYDp = state.petPosition.y.dp
                val screenWidthDp = boxSize.width.dp
                val screenHeightDp = boxSize.height.dp
                val keyboardBottomDp = keyboardBottom.toInt().dp
                val availableHeight = screenHeightDp - keyboardBottomDp
                val maxChatHeight = 400.dp

                // 计算聊天窗口水平位置：优先显示在宠物左侧，若宠物靠近左侧边缘则显示在右侧
                val chatX = if (petXDp > screenWidthDp / 2) {
                    // 宠物在右侧，聊天窗口显示在宠物左侧
                    if (chatPadding > petXDp - chatWidth - chatPadding) chatPadding else petXDp - chatWidth - chatPadding
                } else {
                    // 宠物在左侧，聊天窗口显示在宠物右侧
                    if (screenWidthDp - chatWidth - chatPadding < petXDp + petSize + chatPadding) screenWidthDp - chatWidth - chatPadding else petXDp + petSize + chatPadding
                }

                // 计算聊天窗口垂直位置：与宠物顶部对齐，但保持在屏幕内（考虑键盘高度）
                val innerMin = if (petYDp < availableHeight - maxChatHeight - chatPadding) petYDp else availableHeight - maxChatHeight - chatPadding
                val chatY = if (chatPadding > innerMin) chatPadding else innerMin

                // 聊天窗口布局
                Column(
                    modifier = Modifier
                        .width(chatWidth)
                        .offset(x = chatX, y = chatY)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(chatPadding)
                ) {
                    // 聊天列表滚动状态
                    val listState = rememberLazyListState()

                    // 当消息数量、加载状态或错误信息变化时，自动滚动到底部
                    LaunchedEffect(state.chatMessages.size, state.isLoading, state.errorMessage) {
                        val totalItems = state.chatMessages.size +
                            (if (state.isLoading) 1 else 0) +
                            (if (state.errorMessage != null) 1 else 0)
                        if (totalItems > 0) {
                            listState.animateScrollToItem(totalItems - 1)
                        }
                    }

                    // 消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.chatMessages.isEmpty() && !state.isLoading && state.errorMessage == null) {
                            // 空状态：提示用户开始对话
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "和桌宠开始对话吧~",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(state.chatMessages) { message ->
                                ChatBubble(
                                    text = message.text,
                                    isUser = message.isUser,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            // 加载中：显示机器人侧的跳动圆点气泡
                            if (state.isLoading) {
                                item {
                                    LoadingBubble()
                                }
                            }
                            // 错误状态：显示错误文案与重试按钮
                            state.errorMessage?.let { errorMsg ->
                                item {
                                    ErrorBubble(
                                        message = errorMsg,
                                        onRetry = {
                                            val lastUserText = state.chatMessages.lastOrNull { it.isUser }?.text ?: ""
                                            if (lastUserText.isNotBlank()) {
                                                viewModel.onEvent(PetEvent.OnSendMessage(lastUserText))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 输入框
                    ChatInput(
                        text = state.inputText,
                        onTextChange = { viewModel.onEvent(PetEvent.OnChatInputChanged(it)) },
                        onSend = { viewModel.onEvent(PetEvent.OnSendMessage(state.inputText)) },
                        modifier = Modifier.padding(top = 8.dp),
                        isLoading = state.isLoading
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetScreenPreview() {
    PetScreen()
}

/**
 * 加载中气泡：机器人侧显示三个跳动的圆点
 */
@Composable
private fun LoadingBubble() {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Dot(alpha1)
                Dot(alpha2)
                Dot(alpha3)
            }
        }
    }
}

/**
 * 单个跳动圆点
 */
@Composable
private fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    )
}

/**
 * 错误气泡：机器人侧显示错误文案与重试按钮
 */
@Composable
private fun ErrorBubble(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
