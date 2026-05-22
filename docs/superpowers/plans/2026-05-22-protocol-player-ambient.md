# Protocol Player and Ambient Audio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the simple countdown with a structured breathwork protocol player and add optional looping ambient audio support.

**Architecture:** Extend the recommendation model with a `BreathworkProtocol` containing repeatable phases. The Compose session player derives the active phase from elapsed seconds and renders the current/next cue. Ambient playback is isolated in a small Media3-backed controller that no-ops when no raw audio resource is present.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX Media3 ExoPlayer.

---

### Task 1: Protocol Models

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/data/HybridTempoModels.kt`
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoViewModel.kt`

- [ ] Add `BreathPhase` and `BreathworkProtocol`.
- [ ] Add `protocol` to `BreathworkRecommendation`.
- [ ] Update deterministic recommendation branches to return safe phase templates.

### Task 2: Ambient Audio Hook

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/hybridtempo/android/audio/AmbientAudioController.kt`

- [ ] Add Media3 ExoPlayer dependency.
- [ ] Create a controller that looks up `res/raw/ambient_loop` by name at runtime.
- [ ] Loop playback while enabled and the session is running.
- [ ] No-op safely if no audio file exists.

### Task 3: Session Player UI

**Files:**
- Modify: `app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt`

- [ ] Derive active phase, next phase, and cycle from elapsed time.
- [ ] Animate the breathing ring based on phase type.
- [ ] Add ambient on/off control.
- [ ] Keep pause/resume/finish.

### Task 4: Verify and Commit

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add protocol player ambient support`.
