# Onboarding Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-run onboarding that captures athlete context, persists it to Firestore, and uses it in recommendations.

**Architecture:** Extend the current single-ViewModel Compose app. Add a profile draft and onboarding-complete state to `HybridTempoViewModel`, then add an `OnboardingScreen` before the existing welcome/check-in flow. Keep repository interfaces stable.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Firebase Auth, Firestore, StateFlow.

---

### Task 1: Profile Draft State

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoViewModel.kt`

- [ ] Add `AthleteProfileDraft`.
- [ ] Add `profileDraft` and `hasCompletedOnboarding` to `HybridTempoUiState`.
- [ ] Add `updateProfileDraft`.
- [ ] Add `saveProfile` using `repository.upsertProfile`.

### Task 2: Recommendation Defaults

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoViewModel.kt`

- [ ] Use `preferredSessionLength` as the default check-in duration after profile save.
- [ ] Let selected goals influence the recommendation branch where applicable.

### Task 3: Onboarding UI

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt`

- [ ] Add `Onboarding` to `AppScreen`.
- [ ] Route first launch to onboarding until profile is saved.
- [ ] Add controls for training style, weekly frequency, goals, duration, and race date text.
- [ ] Save profile and move to welcome.

### Task 4: Verify and Commit

**Files:**
- Modify: docs and app files above.

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add athlete onboarding profile`.
