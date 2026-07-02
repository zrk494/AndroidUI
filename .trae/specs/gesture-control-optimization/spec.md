# 手势控制优化 - Product Requirement Document

## Overview
- **Summary**: 优化当前桌面宠物的手势控制实现，提升用户交互体验，解决手势冲突问题，增加视觉反馈，完善手势交互逻辑。
- **Purpose**: 提升桌面宠物的交互流畅性，解决手势冲突问题，提供更直观、更流畅的用户体验。
- **Target Users**: 桌面宠物应用的所有用户。

## Goals
- 解决手势冲突问题（长按、双击、拖拽之间的冲突）
- 增加拖拽前需要长按触发拖拽模式
- 提供清晰的视觉反馈
- 优化拖拽边界处理
- 提升整体交互流畅度

## Non-Goals (Out of Scope)
- 不增加新的手势类型（如滑动、缩放等）
- 不修改聊天界面的交互逻辑
- 不修改工具箱功能
- 不集成真实的 Live2D 模型

## Background & Context
当前实现存在以下问题：
1. 两个独立的 pointerInput 修饰符可能导致手势冲突
2. 拖拽可以随时进行，没有长按触发机制
3. 缺乏视觉反馈来指示当前状态
4. 点击和双击没有明确的视觉反馈
5. 拖拽时没有边界阻尼效果

## Functional Requirements
- **FR-1**: 实现长按触发拖拽模式，只有长按后才能拖拽宠物
- **FR-2**: 合并手势检测到单一 pointerInput 中，避免手势冲突
- **FR-3**: 为各种手势提供清晰的视觉反馈
- **FR-4**: 优化拖拽边界处理，增加阻尼效果
- **FR-5**: 确保手势事件正确传递和消费

## Non-Functional Requirements
- **NFR-1**: 手势响应延迟 < 100ms
- **NFR-2**: 动画流畅度保持 60fps
- **NFR-3**: 代码结构清晰，易于维护和扩展
- **NFR-4**: 兼容不同屏幕尺寸和分辨率

## Constraints
- **Technical**: 使用 Jetpack Compose，保持 MVI 架构
- **Business**: 2 天内完成优化
- **Dependencies**: 无外部依赖库

## Assumptions
- 用户希望手势优化不会引入新的 bug
- 现有的 MVI 架构可以满足优化后的手势逻辑
- 用户设备支持触觉反馈

## Acceptance Criteria

### AC-1: 长按触发拖拽
- **Given**: 宠物处于空闲状态
- **When**: 用户长按宠物超过阈值时间
- **Then**: 设备触发触觉反馈，宠物进入拖拽模式，视觉上有缩放/高亮效果
- **Verification**: `human-judgment`
- **Notes**: 长按阈值采用系统默认值

### AC-2: 拖拽模式下移动宠物
- **Given**: 宠物处于拖拽模式
- **When**: 用户拖动手指
- **Then**: 宠物跟随手指移动，保持在屏幕边界内，边界有阻尼效果
- **Verification**: `human-judgment`

### AC-3: 拖拽结束
- **Given**: 宠物处于拖拽模式
- **When**: 用户释放手指
- **Then**: 宠物退出拖拽模式，恢复原始大小
- **Verification**: `human-judgment`

### AC-4: 单击反馈
- **Given**: 宠物处于空闲状态
- **When**: 用户单击宠物
- **Then**: 宠物有视觉反馈（如轻微跳动或透明度变化）
- **Verification**: `human-judgment`

### AC-5: 双击切换聊天
- **Given**: 宠物处于空闲状态
- **When**: 用户双击宠物
- **Then**: 聊天界面显示/隐藏，无拖拽误触发
- **Verification**: `human-judgment`

### AC-6: 手势无冲突
- **Given**: 任意状态下
- **When**: 用户执行任意手势操作
- **Then**: 只有预期的手势被触发，无冲突
- **Verification**: `human-judgment`

### AC-7: 代码可编译
- **Given**: 优化完成后
- **When**: 执行编译
- **Then**: 项目编译成功，无错误
- **Verification**: `programmatic`

## Open Questions
- [ ] 是否需要添加位置记忆功能（记住宠物上次的位置？
- [ ] 是否需要添加回弹动画（释放后回弹到默认位置？
- [ ] 长按时间阈值是否需要可配置？
