package com.example.toolpack.feature.pet.ui

import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/**
 * Live2D 宠物视图组件
 *
 * 该组件负责显示和处理 Live2D 宠物的交互，包括：
 * 1. 显示宠物图像（使用 WebView + pixi-live2d-display 渲染真正的 Live2D 模型）
 * 2. 处理各种手势操作：点击、双击、长按和拖拽
 * 3. 提供触觉反馈（长按操作）
 * 4. 根据位置参数定位宠物
 * 5. 支持长按后才能拖拽的逻辑
 * 6. 提供视觉反馈（缩放和透明度）
 * 7. 通过 JavascriptInterface 与 WebView 内的 Live2D 模型双向通信
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
    // WebView 引用，在 factory 中赋值，用于点击时转发坐标给 JS 做命中测试
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

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
                        // 转发点击坐标给 WebView 内的 Live2D 模型进行命中测试
                        webViewRef.value?.evaluateJavascript(
                            "window.live2dBridge && window.live2dBridge.tap(${pos.x}, ${pos.y});",
                            null
                        )
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    }
                    addJavascriptInterface(Live2DBridge(), "AndroidBridge")
                    loadUrl("file:///android_asset/live2d/index.html")
                    // 不消费触摸事件，交由 Compose 的 pointerInput 处理
                    setOnTouchListener { _, _ -> false }
                }.also { webView ->
                    webViewRef.value = webView
                }
            },
            onRelease = { webView ->
                // Composable 离开组合时销毁 WebView，避免渲染进程/资源泄漏
                webView.destroy()
                webViewRef.value = null
            }
        )
    }
}

/**
 * JS 与原生通信桥接对象
 *
 * 通过 addJavascriptInterface 注册为 window.AndroidBridge，
 * 供 WebView 内的 JS 调用以通知原生层模型加载状态等事件。
 */
private class Live2DBridge {
    @JavascriptInterface
    fun onModelLoaded() {
        // 模型加载完成，可在此触发 Compose 状态更新（当前为空实现，留扩展点）
    }

    @JavascriptInterface
    fun onError(message: String) {
        // 模型加载失败，可在此触发 Compose 错误状态（当前为空实现，留扩展点）
    }
}
