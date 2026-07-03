# Android UI Polish — Spec 3: 聊天 UI 视觉与交互打磨

**日期**: 2026-07-03
**状态**: 已批准 / 已实现
**前置**: spec-2（Live2D 桌宠本体已就位）

## 1. Overview

在 spec-2 完成 Live2D 桌宠本体后，对聊天 UI 做纯视觉与交互打磨：引入 loading/error 状态机、Material3 配色、中文化、空状态提示与自动滚动。为后续 agent-android 真实 LLM 集成留好状态接入点。

## 2. Scope

### In Scope
- MVI 层扩展：PetState 加 `isLoading`/`errorMessage`，PetEvent 加 `OnLLMLoading`/`OnLLMError`
- PetViewModel 的 OnSendMessage 加 loading/error dispatch
- ChatBubble/ChatInput Material3 化 + 中文化
- PetScreen 加 loading/error/empty 状态 + 自动滚动
- PetReducerTest 覆盖新分支

### Out of Scope
- agent-android 真实 LLM 集成（后续阶段，但本 spec 的状态机为它留接入点）
- 修改 MockLLMService（保持其 10% 抛异常 + 5% 空回复用于测试 error/empty 路径）
- Live2DView 改动（spec-2 已完成）

## 3. 设计决策

### 3.1 状态机
```
用户发送消息 → OnSendMessage(reducer 追加用户消息 + 清空 input)
            → ViewModel side effect: dispatch OnLLMLoading(isLoading=true)
            → 调 llmService.generateResponse
            ├─ 成功非空 → OnBotMessage(追加机器人消息, isLoading=false, errorMessage=null)
            ├─ 成功为空 → OnLLMError("收到空回复")
            └─ 抛异常    → OnLLMError(e.message)
```

### 3.2 配色（Material3）
- 用户气泡：`primary` 背景 + `onPrimary` 文字
- 机器人气泡：`surfaceVariant` 背景 + `onSurfaceVariant` 文字
- 加载气泡：`surfaceVariant` 背景 + `onSurfaceVariant` 圆点（alpha 动画）
- 错误气泡：`errorContainer` 背景 + `onErrorContainer` 文字
- 字体：`MaterialTheme.typography.bodyMedium`

### 3.3 交互
- loading 期间禁用输入框和发送按钮（`enabled = !isLoading && text.isNotBlank()`）
- 错误气泡"重试"按钮：取最后一条用户消息重新 dispatch OnSendMessage
- 消息列表自动滚动：LaunchedEffect 监听 `chatMessages.size`/`isLoading`/`errorMessage` 变化

### 3.4 中文化范围
- 输入框 placeholder: "输入消息..."
- 发送按钮 contentDescription: "发送"
- 空状态: "和桌宠开始对话吧~"
- 错误重试: "重试"
- 工具箱 Toast: "开发中"（已存在）
- TopAppBar "ToolPack" 保留（应用名）
- MockLLMService 英文回复**不改**（Out of Scope）

## 4. 文件结构

```
app/src/main/java/com/example/toolpack/feature/pet/
  ├── PetState.kt         [修改] +isLoading +errorMessage
  ├── PetEvent.kt         [修改] +OnLLMLoading +OnLLMError
  ├── PetReducer.kt       [修改] +2 分支, OnBotMessage 更新
  └── PetViewModel.kt     [修改] OnSendMessage side effect 重写

app/src/main/java/com/example/toolpack/feature/chat/ui/
  ├── ChatBubble.kt       [修改] Material3 配色 + widthIn(240) + bodyMedium
  └── ChatInput.kt        [修改] Material3 TextField + IconButton + isLoading

app/src/main/java/com/example/toolpack/feature/pet/ui/
  └── PetScreen.kt        [修改] loading/error/empty + 自动滚动 + 3 Composable

app/src/test/java/com/example/toolpack/feature/pet/
  └── PetReducerTest.kt   [修改] +3 测试, OnBotMessage 加断言
```

## 5. 实现计划

### Task 1: MVI 层扩展
- PetState: `isLoading: Boolean = false`, `errorMessage: String? = null`
- PetEvent: `object OnLLMLoading`, `data class OnLLMError(message)`
- PetReducer: OnLLMLoading/OnLLMError 分支 + OnBotMessage 加 `isLoading=false, errorMessage=null`
- PetViewModel: OnSendMessage 先 dispatch OnLLMLoading，try/catch dispatch OnLLMError/OnBotMessage

### Task 2: ChatBubble + ChatInput
- ChatBubble: 硬编码颜色 → MaterialTheme.colorScheme，`widthIn(max=240.dp)`，bodyMedium
- ChatInput: BasicTextField → Material3 TextField，Button "Send" → IconButton + Icons.Default.Send，加 `isLoading` 参数，中文 placeholder

### Task 3: PetScreen
- rememberLazyListState + LaunchedEffect 自动滚动
- if/else: 空状态("和桌宠开始对话吧~") / 消息列表 + loading item + error item
- LoadingBubble（三点 alpha 动画）、Dot、ErrorBubble（errorContainer + "重试"）
- ChatInput 调用传 `isLoading = state.isLoading`

### Task 4: 测试更新
- OnBotMessage 测试加 `assertFalse(isLoading)` + `assertNull(errorMessage)`
- 新增 OnLLMLoading / OnLLMError / OnBotMessage-clears 测试

## 6. 真机回归清单
- [ ] 发送消息后输入框禁用，显示三点跳动加载气泡
- [ ] 收到回复后加载气泡消失，消息追加，输入框恢复
- [ ] 模拟 10% 异常：显示错误气泡 + "重试"，点重试重新发送
- [ ] 模拟 5% 空回复：显示"收到空回复"错误气泡
- [ ] 无消息时显示"和桌宠开始对话吧~"
- [ ] 新消息到达时自动滚动到底部
- [ ] 气泡配色跟随主题（亮/暗模式）

## 7. Global Constraints
- 不修改 MockLLMService（其随机异常/空回复用于测试 error 路径）
- 不修改 Live2DView（spec-2 成果）
- 不修改 PetScreen 的 Live2DView 调用和聊天窗口位置计算逻辑

## 8. 风险
- **沙箱无网络**: 无法运行 `./gradlew test`，测试逻辑静态核对。
- **MockLLMService 随机性**: 10%/5% 概率路径需多次触发才能在真机回归中复现。

## 9. 验收标准
- [x] PetState 含 isLoading/errorMessage
- [x] PetEvent 含 OnLLMLoading/OnLLMError
- [x] PetReducer 处理 4 个相关分支（OnBotMessage 更新 + OnLLMLoading + OnLLMError）
- [x] PetViewModel 先 dispatch loading，try/catch dispatch error/bot
- [x] PetReducerTest 覆盖新分支（3 新测试 + OnBotMessage 加断言）
- [x] ChatBubble Material3 配色 + 最大宽度
- [x] ChatInput Material3 TextField + 图标按钮 + isLoading 禁用
- [x] PetScreen loading/error/empty/自动滚动
- [x] 中文化（输入消息…/发送/重试/和桌宠开始对话吧~）
- [x] 设计文档已提交
