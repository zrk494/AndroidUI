# Android UI Polish — Spec 1: 工程环境修复 + Bug 状态核查 + PetReducer 单元测试

**日期**: 2026-07-03
**状态**: 已批准 / 已实现
**前置**: 无（基于初始化 commit `f9f95a4`）

## 1. Overview

在开始 Live2D WebView 重写（spec-2）和聊天 UI 打磨（spec-3）之前，先修复工程环境让项目能在不同开发机上构建，并核查已知 Bug 状态，同时为 PetReducer 建立单元测试安全网——确保 spec-2 重写 Live2DView 时 reducer 层的契约不被破坏。

## 2. Scope

### In Scope
- `gradle.properties` 删除硬编码 JDK 路径
- 已知 Bug 状态核查（仅核查，不在本 spec 修复）
- `PetReducer` 纯函数单元测试（11 个）

### Out of Scope
- Live2DView 重写（spec-2）
- 聊天 UI 视觉打磨（spec-3）
- 修复本 spec 核查出的 Bug（除非属于本 spec 验收项）

## 3. 设计决策

### 3.1 JDK 配置策略
**问题**: `gradle.properties` 末尾硬编码 `org.gradle.java.home=C\:/Users/26947/.jdks/jbr-21.0.9`，指向特定用户的本地 JDK 21 路径，在其他开发机上构建失败。

**决策**: 删除该行，依赖 `app/build.gradle.kts` 中已有的 `kotlin { jvmToolchain(11) }` 自动定位已安装的 JDK。同时在原位置加注释说明降级方案（`local.properties` 中设 `java.home`）。

**理由**: `jvmToolchain(11)` 是 Gradle 官方推荐的跨环境 JDK 定位机制，比硬编码路径更健壮；JDK 11 是 AGP 8.x 兼容的最低版本，普遍可用。

### 3.2 Bug 核查范围
本 spec 只核查不改：核查结果记录在"风险"章节，修复留后续 spec。

### 3.3 测试范围
PetReducer 是纯函数（state, event) -> state，无副作用，适合单元测试。覆盖：
- 手势状态切换（OnLongPress/OnDragEnd/OnTap）— 为 spec-2 Live2DView 重写提供安全网
- `clampToScreenBounds` 边界处理（含零值早返分支）
- 聊天状态（OnDoubleTap toggle / OnSendMessage / OnBotMessage）
- OnScreenSizeAvailable（默认位置 + resize clamp）

## 4. 文件结构

```
gradle.properties                                    [修改]
app/src/test/java/com/example/toolpack/feature/pet/
  └── PetReducerTest.kt                              [新建]
```

## 5. 实现计划

### Task 1: 修复 gradle.properties
- 删除第 24 行 `org.gradle.java.home=...`
- 加 4 行注释说明 jvmToolchain + local.properties 降级方案

### Task 2: 创建 PetReducerTest
11 个测试方法，按 reducer 分支分组：
- 手势（3）: OnLongPress/OnDragEnd/OnTap
- clamp（3）: OnDrag clamp / OnDrag move / 早返分支
- 聊天（3）: OnDoubleTap toggle / OnSendMessage / OnBotMessage
- ScreenSize（2）: 默认位置 / resize clamp

## 6. 真机回归清单
（本 spec 不涉及 UI 改动，回归项聚焦构建）
- [ ] 在 Windows/macOS/Linux 三平台上 `./gradlew assembleDebug` 成功
- [ ] 在未设置 `JAVA_HOME` 的环境下，jvmToolchain 能定位到 JDK 11+
- [ ] `./gradlew test` 通过 11 个 PetReducerTest

## 7. Global Constraints
- 不修改 `app/build.gradle.kts`（JDK 工具链配置已存在）
- 不修改 `PetReducer.kt` / `PetState.kt` / `PetEvent.kt`（本 spec 只测不改）
- 测试用 JUnit 4（项目现有依赖）

## 8. 风险
- **沙箱无网络**: 无法运行 `./gradlew test`（AGP 下载需联网），测试逻辑以静态核对方式验证，待有网络环境跑通。
- **Bug 核查结果**: Live2DView 当前用静态 `ic_launcher_foreground` 图片占位（非真实 Live2D），将在 spec-2 修复；PetViewModel 的 OnSendMessage 直接同步调 LLM，将在 spec-3 加 loading/error 状态。

## 9. 验收标准
- [x] `gradle.properties` 不再含 `org.gradle.java.home` 硬编码行
- [x] `PetReducerTest.kt` 含 11 个 `@Test` 方法
- [x] 测试覆盖 OnLongPress/OnDragEnd/OnTap/clampToScreenBounds(零值)/OnDoubleTap/OnSendMessage/OnBotMessage/OnScreenSizeAvailable
- [x] 不修改 reducer/state/event 源码
- [x] 设计文档已提交
