# History Progress Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the basic history list with a progress dashboard showing streak, totals, minutes, recent sessions, and protocol mix.

**Architecture:** Keep analytics local and derived from `recentSessions` in the ViewModel/UI layer. Add small pure helper functions in `HybridTempoApp.kt` for summary calculations and render the dashboard with existing Material 3 cards.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Firestore-backed session history.

---

### Task 1: Summary Helpers

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt`

- [ ] Add `HistorySummary`.
- [ ] Add functions for total sessions, total minutes, category counts, and current streak.

### Task 2: Dashboard UI

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt`

- [ ] Replace `HistoryScreen` body with top stat cards.
- [ ] Add empty state when no sessions exist.
- [ ] Add recent sessions list.
- [ ] Add protocol mix list.

### Task 3: Verify and Commit

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add history progress dashboard`.
