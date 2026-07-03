# Android UI Polish — Spec 2: Live2D 桌宠本体（WebView + pixi-live2d-display）

**日期**: 2026-07-03
**状态**: 已批准 / 已实现
**前置**: spec-1（reducer 测试安全网已就位）

## 1. Overview

将 Live2DView 从静态占位图（`ic_launcher_foreground`）替换为真实的 Live2D shizuku 模型渲染，使用 WebView + pixi-live2d-display 方案。保持组件签名不变，PetScreen 调用点零改动。

## 2. Scope

### In Scope
- Live2D 运行时库（pixi.js + live2d core + pixi-live2d-display cubism2 bundle）
- shizuku 模型资产
- `index.html`（WebView 入口）
- `Live2DView.kt` 重写（Image → WebView）
- 触摸事件协调（Compose 主导 + WebView 不消费 + evaluateJavascript 转发）

### Out of Scope
- 聊天 UI 视觉打磨（spec-3）
- agent-android 真实 LLM 集成（后续阶段）
- Cubism 4/5 模型支持（仅 Cubism 2.1）

## 3. 设计决策

### 3.1 库选型
- **pixi-live2d-display** guansss v0.4.0（非 naari3 fork——后者仅支持 Cubism 5，不适用 shizuku 的 Cubism 2.1 格式）
- **PixiJS** 6.5.10（pixi-live2d-display 0.4.x 兼容版本）
- **Cubism 2.1 Core**: `live2d.min.js`（**非** `live2dcubismcore.min.js`），从 dylanNew/live2d 社区镜像获取（Live2D 官方已停止提供 Cubism 2.1 SDK）

### 3.2 触摸事件协调（方案 A）
问题：WebView 默认消费触摸事件，会阻断 Compose 的 pointerInput 手势识别。

方案：
- Compose `pointerInput` 顶层负责所有手势（tap/doubleTap/longPress/drag）
- WebView `setOnTouchListener { false }` 不消费事件
- `autoInteract: false` 禁用 pixi-live2d-display 的自动交互
- 点击时通过 `evaluateJavascript("window.live2dBridge.tap(x,y)")` 转发坐标给 Live2D 模型做命中测试

### 3.3 透明背景
- `setBackgroundColor(Color.TRANSPARENT)` + `LAYER_TYPE_HARDWARE`
- **不可用 LAYER_TYPE_SOFTWARE**——会禁用 WebGL，导致 Live2D 无法渲染
- `index.html`: `backgroundAlpha: 0` + CSS `background: transparent`

### 3.4 加载顺序
严格顺序：`live2d.min.js` → `pixi.min.js` → `cubism2.min.js`
- cubism2 bundle 依赖全局 `PIXI` 对象，pixi 必须先加载

### 3.5 资源泄漏防护
`AndroidView` 提供 `onRelease`，在 Composable 离开组合时 `WebView.destroy()` + 清空 ref，避免渲染进程泄漏（broad final review 发现的 MAJOR 问题）。

## 4. 文件结构

```
app/src/main/assets/live2d/
  ├── index.html                      [新建] WebView 入口
  ├── pixi.min.js                     [新建] ~460KB
  ├── live2d.min.js                   [新建] ~129KB Cubism 2.1 core
  ├── cubism2.min.js                  [新建] ~41KB pixi-live2d-display bundle
  └── shizuku/                        [新建] 模型目录
      ├── shizuku.model.json
      ├── shizuku.moc                 (~691KB)
      ├── shizuku.physics.json
      ├── shizuku.pose.json
      ├── expressions/                (4 个)
      ├── motions/                    (18 个)
      ├── shizuku.1024/               (6 张纹理)
      └── sounds/                     (15 个 mp3)

app/src/main/java/com/example/toolpack/feature/pet/ui/
  └── Live2DView.kt                   [重写] Image → WebView
```

## 5. 实现计划

### Task 1: 下载 Live2D 运行时与模型
- pixi.min.js@6.5.10（注意路径 `dist/browser/pixi.min.js`，`dist/pixi.min.js` 会 404）
- live2d.min.js（dylanNew/live2d 镜像）
- cubism2.min.js@0.4.0
- shizuku 完整模型（按 shizuku.model.json 引用，无遗漏无多余）

### Task 2: 创建 index.html
- 严格加载顺序
- PIXI.Application 透明背景
- `window.live2dBridge`（tap/focus/expression/motion）
- 模型居中缩放
- try/catch + AndroidBridge.onError 错误上报

### Task 3: 重写 Live2DView.kt
- 签名不变（PetScreen 零改动）
- Image → AndroidView { WebView }
- 透明 + LAYER_TYPE_HARDWARE + JS 启用 + file:// 跨域
- setOnTouchListener { false } + autoInteract:false
- evaluateJavascript 转发点击坐标
- onRelease destroy WebView
- Live2DBridge @JavascriptInterface（onModelLoaded/onError）

## 6. 真机回归清单
- [ ] WebView 透明背景，无白色/黑色方块
- [ ] shizuku 模型正常显示，无 JS 控制台错误
- [ ] 点击桌宠触发 onTap + Live2D 命中测试（动作/表情）
- [ ] 双击切换聊天窗口
- [ ] 长按震动反馈 + 进入拖拽模式
- [ ] 拖拽流畅，边界 clamp 生效
- [ ] 旋转屏幕/返回后 WebView 不泄漏（onRelease destroy）

## 7. Global Constraints
- 不修改 PetScreen.kt 的 Live2DView 调用（签名兼容）
- 不引入新的 Gradle 依赖（WebView/PixiJS 均为前端资源）
- 模型资产走 `file:///android_asset/`，不需网络权限

## 8. 风险
- **坐标偏差**: 点击转发 `pos.x/pos.y` 是 Compose Box 局部 px，与 WebView canvas 的 devicePixelRatio + 模型缩放可能存在偏差，命中测试可能落空——属真机回归项，必要时加坐标转换。
- **Cubism 2.1 SDK 来源**: live2d.min.js 来自社区镜像，非官方。如需官方支持需迁移到 Cubism 5 + naari3 fork（模型也要换）。
- **沙箱无网络**: 无法构建验证，依赖静态 import/API 检查。

## 9. 验收标准
- [x] assets/live2d/ 含 index.html + 3 个 JS + shizuku 完整目录
- [x] Live2DView.kt 重写为 WebView 版本，签名与原版一致
- [x] index.html 含 autoInteract:false + live2dBridge + 透明背景
- [x] WebView 透明 + LAYER_TYPE_HARDWARE + JS 启用 + setOnTouchListener 不消费
- [x] onRelease 销毁 WebView（防泄漏）
- [x] 设计文档已提交
