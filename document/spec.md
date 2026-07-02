# Project Specification: Desktop Pet & Toolkit App (MVP)

## 1. Overview

Develop a native Android MVP application featuring a confined, interactive 2D desktop pet with LLM-based conversational capabilities, establishing a UI framework for a future toolkit extension.

## 2. Goals & Non-Goals

### Goals

* **Platform**: Native Android Application.
* **Pet Core**: 2D Image-based rendering with 3 distinct states (Idle, Interaction, Drag).
* **Confinement**: Pet operates strictly within the App Activity boundaries (No System Overlay).
* **Interaction**:
* **Gestures**: Long-press to drag, Double-tap to chat, Single-tap for feedback.
* **LLM**: Text-based conversation integration.


* **Toolkit**: UI Placeholder implementation.

### Non-Goals

* **System Overlay**: No `SYSTEM_ALERT_WINDOW` permission or functionality.
* **Toolkit Logic**: No functional tools implemented beyond the UI entry point.
* **Advanced Animation**: No 3D models or skeletal animation systems.

## 3. Core Constraints

* **Tech Stack**: Android Native (Kotlin recommended).
* **Permissions**: Standard Internet permission only; explicitly **zero** sensitive floating window permissions.
* **Performance**: LLM network requests must run on a background thread; UI must remain responsive (60fps) during data fetching.
* **Layout**: `ConstraintLayout` or `FrameLayout` required to manage z-ordering of the Pet View over the background.

## 4. Acceptance Criteria

### 4.1. Initialization & UI Layout

* **AC-01**: Startup State
* **Given** the app is launched for the first time,
* **Then** the Pet View renders at the **bottom-right** of the screen,
* **And** the top navigation bar displays a button labeled "工具箱" (Toolkit).



### 4.2. Gesture System (Conflict Resolution)

* **AC-02**: Drag Operation
* **Given** the pet is in an Idle state,
* **When** the user performs a **Long Press** (duration > threshold),
* **Then** the device triggers a system Haptic Feedback (Vibration),
* **And** the Pet View center coordinates update to follow the user's finger until release.


* **AC-03**: Chat Trigger
* **Given** the pet is in an Idle state,
* **When** the user performs a **Double Tap** on the pet,
* **Then** the soft keyboard expands,
* **And** an input field becomes visible above the keyboard.


* **AC-04**: Simple Interaction
* **Given** the pet is in an Idle state,
* **When** the user performs a **Single Tap**,
* **Then** the pet executes a visual feedback change (e.g., image swap) immediately.



### 4.3. Chat & LLM Integration

* **AC-05**: Chat UI Positioning
* **Given** the chat interface is active,
* **When** a message bubble is rendered,
* **Then** the bubble is anchored to the **Left** side of the Pet View.


* **AC-06**: Loading State
* **Given** the user has sent a message to the LLM,
* **When** the app is awaiting the network response,
* **Then** the Pet View remains in the **Idle/Static** state (No specific loading animation is triggered).


* **AC-07**: Keyboard Occlusion
* **Given** the soft keyboard is expanded,
* **When** the keyboard height overlaps with the Pet's position,
* **Then** the Input Box must remain visible (Pet occlusion is acceptable per findings).



### 4.4. Toolkit Placeholder

* **AC-08**: Feature Gating
* **Given** the user is on the main screen,
* **When** the user taps the "工具箱" (Toolkit) button,
* **Then** an Android Toast appears with the exact text: "开发中".

