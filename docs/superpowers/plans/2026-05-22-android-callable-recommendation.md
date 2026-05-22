# Android Callable Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Android to call the deployed Firebase callable recommendation endpoint while preserving deterministic fallback behavior.

**Architecture:** Add Firebase Functions dependency. Convert `RecommendationEngine` to suspend. `BackendRecommendationEngine` calls `recommendBreathwork`, validates/parses the response, and falls back to `DeterministicRecommendationEngine` on any failure.

**Tech Stack:** Kotlin, Firebase Functions Android SDK, coroutines play services, Jetpack Compose.

---

### Task 1: Dependency and Async Engine

- [ ] Add `firebase-functions`.
- [ ] Make `RecommendationEngine.recommend` suspend.
- [ ] Update deterministic engine signature.

### Task 2: Callable Backend Engine

- [ ] Serialize `RecommendationRequest` to callable map.
- [ ] Call `recommendBreathwork` in `us-central1`.
- [ ] Parse `BreathworkRecommendation` and `BreathworkProtocol`.
- [ ] Fallback on errors.

### Task 3: ViewModel and UI

- [ ] Refresh recommendations from coroutines.
- [ ] Store `RecommendationSource`.
- [ ] Show backend/fallback label in UI.

### Task 4: Verify and Commit

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: call firebase recommendation function`.
