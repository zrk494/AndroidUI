package com.example.toolpack.feature.pet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.toolpack.R
import kotlin.math.roundToInt

/**
 * Live2D 宠物视图组件
 * 
 * 该组件负责显示和处理 Live2D 宠物的交互，包括：
 * 1. 显示宠物图像（目前使用占位图，将来会替换为真正的 Live2D 模型）
 * 2. 处理各种手势操作：点击、双击、长按和拖拽
 * 3. 提供触觉反馈（长按操作）
 * 4. 根据位置参数定位宠物
 * 5. 支持长按后才能拖拽的逻辑
 * 6. 提供视觉反馈（缩放和透明度）
 *
 * @param position 宠物的位置偏移量
 * @param isDragging 是否正在拖拽状态
 * @param petScale 宠物的缩放比例
 * @param petAlpha 宠物的透明度
 * @param onTap 点击事件回调
 * @param onDoubleTap 双击事件回调
 * @param onLongPress 长按事件回调
 * @param onDrag 拖拽事件回调
 * @param onDragEnd 拖拽结束事件回调
 * @param modifier 修饰符
 */
@Composable
fun Live2DView(
    position: Offset,
    isDragging: Boolean,
    petScale: Float,
    petAlpha: Float,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val canDrag = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(120.dp)
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .scale(petScale)
            .alpha(petAlpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { pos ->
                        onTap(pos)
                    },
                    onDoubleTap = { pos ->
                        onDoubleTap(pos)
                    },
                    onLongPress = { pos ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        canDrag.value = true
                        onLongPress(pos)
                    }
                )
            }
            .pointerInput(canDrag.value) {
                if (canDrag.value) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = {
                            canDrag.value = false
                            onDragEnd(Offset.Zero)
                        }
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Live2D Pet",
            modifier = Modifier.size(120.dp)
        )
    }
}