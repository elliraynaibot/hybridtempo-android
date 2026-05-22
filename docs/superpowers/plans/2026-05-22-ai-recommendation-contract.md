# AI Recommendation Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an AI-ready recommendation boundary while keeping deterministic fallback behavior active.

**Architecture:** Move hardcoded recommendation selection out of `HybridTempoViewModel` into a `RecommendationEngine` interface. Add contract data models for backend request/response, a deterministic fallback implementation, and a backend stub that delegates to fallback until Firebase Functions/Gemini is implemented.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, Firebase-ready architecture.

---

### Task 1: Contract Models

- [ ] Create `RecommendationRequest`.
- [ ] Create profile, check-in, and trend context models.
- [ ] Create `RecommendationResponse` with source metadata.

### Task 2: Engines

- [ ] Create `RecommendationEngine`.
- [ ] Move current branch logic into `DeterministicRecommendationEngine`.
- [ ] Create `BackendRecommendationEngine` stub that delegates to fallback.

### Task 3: ViewModel Wiring

- [ ] Replace direct `buildRecommendation` calls with engine calls.
- [ ] Include recent check-ins in request trend context.
- [ ] Keep existing app behavior unchanged.

### Task 4: Docs and Verification

- [ ] Document backend JSON request/response contract.
- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add ai recommendation contract`.
