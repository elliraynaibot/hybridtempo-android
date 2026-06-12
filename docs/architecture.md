# HybridTempo Architecture

## Product Direction

HybridTempo is an Android-first breath skills and heart-rate recovery coach for athletes.

The core product loop is:

```text
Training context
-> recommended breath skill
-> pre-workout primer
-> workout cue
-> post-workout reflection
-> heart-rate recovery review
-> next recommendation
```

The app should not become a generic breathwork app, meditation app, or chatbot. It should recommend one athlete-focused breath skill at a time and explain whether that skill appears to be helping effort control and recovery.

## Current State

The app is currently a single-module Kotlin Android prototype using Jetpack Compose, Firebase Auth/Firestore/Functions, Health Connect scaffolding, and a large UI file:

```text
app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt
```

That prototype is useful for validating screens and flow, but it is not the target architecture. The PRD changes the product toward a local-first data model, breath skills library, workout planning, Health Connect import, post-workout reflection, and Breath Impact Review.

## Target Architecture

Use a single Android app module initially, with package-level boundaries. Split Gradle modules later only if the codebase size justifies it.

Target packages:

```text
feature/onboarding
feature/today
feature/train
feature/review
feature/settings
feature/planner
feature/primer
feature/reflection

domain/model
domain/recommendation
domain/hranalysis
domain/usecase
domain/repository

data/local
data/healthconnect
data/repository
data/seed

core/design
core/navigation
core/time
core/safety
```

## UI Architecture

Use Jetpack Compose with MVVM.

Rules:

- Compose screens render immutable UI state.
- UI events are sent up to ViewModels.
- ViewModels coordinate use cases and repositories.
- Domain logic must not live inside composables.
- Shared visual primitives live in `core/design`.
- Avoid growing `HybridTempoApp.kt`; new PRD work should move into feature packages.

## Navigation

The MVP should move toward four main tabs:

```text
Today
Train
Review
Settings
```

Initial feature flow:

```text
Onboarding -> Today
Today -> Plan Session -> Recommendation -> Primer
Review -> Import Workout -> Reflection -> Breath Impact Review
Train -> Skill List -> Skill Detail -> Primer
```

## Data Architecture

The MVP should be local-first and Firebase-backed.

Use:

- Room as the source of truth for structured app data.
- DataStore as the source of truth for lightweight profile/preferences.
- Firebase for authentication, cloud backup, cross-device sync, analytics, and crash reporting.
- Health Connect for user-granted workout and heart-rate reads.
- WorkManager for durable background imports when needed.

User-facing writes should save locally first and sync to Firebase afterward. Firebase is available and should be used, but temporary Firebase auth, network, rules, or billing failures should not erase user data or block the core breathwork loop.

Primary local records:

```text
UserProfile
BreathSkill
WorkoutPlan
ImportedWorkout
HeartRateSample
PostWorkoutReflection
HeartRateAnalysisResult
InsightCard
```

Firebase sync records should track:

```text
local record id
remote record id
sync status
last synced at
last sync error
```

## Domain Layer

Keep domain logic deterministic and testable first.

Core services:

```kotlin
interface RecommendationEngine
interface HeartRateAnalyzer
interface SimilarWorkoutMatcher
interface InsightGenerator
```

Recommendation engine responsibilities:

- Choose exactly one primary breath skill.
- Explain why it was chosen.
- Provide a workout cue.
- Define a measurement focus.
- Work without Health Connect.

Heart-rate analysis responsibilities:

- Clean sparse/noisy HR samples.
- Calculate data coverage.
- Calculate early spike.
- Calculate HRR60.
- Calculate time above target.
- Calculate confidence.
- Avoid medical claims.

## Health Connect

Health Connect is an enhancement, not a requirement.

MVP reads:

```text
Exercise sessions
Heart rate
Resting heart rate, only if used by UI
```

Do not request sleep, HRV, or write permissions until the app has user-facing features that require them.

Health Connect failure modes must be normal states:

- Not installed.
- Unavailable.
- Permission denied.
- No workouts.
- No HR data.
- Sparse HR data.

## Safety And Privacy

HybridTempo must stay conservative.

Never add:

- Max breath holds.
- Hyperventilation protocols.
- Underwater breathwork.
- Instructions to push through dizziness, chest pain, or faintness.

Use language like:

- "suggests"
- "may indicate"
- "trending"
- "compared with similar sessions"

Avoid:

- "proves"
- "diagnoses"
- "guarantees"

Do not send raw heart-rate samples to analytics.

## Testing Strategy

Unit test:

- RecommendationEngine.
- HeartRateAnalyzer.
- SimilarWorkoutMatcher.
- InsightGenerator.
- Seed data integrity.

Use fake repositories and sample data. Do not depend on live Health Connect in unit tests.

Build verification:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Migration Plan

Milestone 1 should not try to rebuild the whole app at once.

Recommended order:

1. Add domain models and seed breath skills.
2. Add local repository abstractions.
3. Add Train tab library from seed data.
4. Add workout planner and deterministic recommendation engine.
5. Add primer flow.
6. Add Health Connect import.
7. Add reflection.
8. Add heart-rate analysis and Breath Impact Review.

Existing prototype screens can be reused where they match the PRD, but new PRD work should move out of the monolithic app file.
