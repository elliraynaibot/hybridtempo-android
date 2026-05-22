# Check-In Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load recent check-ins and show energy, soreness, and stress trends so athletes can see recovery state changes over time.

**Architecture:** Extend the repository with `recentCheckIns`, keep trend calculations local in the Compose layer, and show compact trend cards on the History screen. No chart dependency.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Firestore.

---

### Task 1: Repository Support

- [ ] Add `recentCheckIns` to `HybridTempoRepository`.
- [ ] Implement Firestore query ordered by `createdAt`.
- [ ] Parse `DailyCheckIn` documents.

### Task 2: ViewModel State

- [ ] Add `recentCheckIns` to `HybridTempoUiState`.
- [ ] Load check-ins on init.
- [ ] Refresh check-ins after saving a check-in.

### Task 3: Trend UI

- [ ] Add trend summary helpers.
- [ ] Add Energy, Stress, Soreness cards to History.
- [ ] Add today state summary from latest check-in.

### Task 4: Verify and Commit

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add check-in recovery trends`.
