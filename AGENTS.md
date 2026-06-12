# HybridTempo Agent Instructions

HybridTempo is an Android-first Kotlin app that teaches athletes breath skills and measures whether those skills improve heart-rate control and recovery during training.

Use [docs/PRD.md](docs/PRD.md) as the product source of truth and [docs/architecture.md](docs/architecture.md) as the technical direction.

## Product Principles

- Recommend one breath skill at a time.
- Organize content by athlete problems, not generic meditation categories.
- Health data must be handled conservatively and privately.
- Do not make medical claims.
- Do not add aggressive breath holds, hyperventilation, or unsafe breathing challenges.
- The app must work without Health Connect.
- Health Connect should enhance insights, not block core usage.
- The product loop is training context -> breath skill -> primer -> workout cue -> reflection -> Breath Impact Review.

## Current Codebase Note

The app currently contains prototype UI and routing in:

```text
app/src/main/java/com/hybridtempo/android/ui/HybridTempoApp.kt
```

Do not keep growing that file for new PRD work. New implementation should move toward feature, domain, data, and core packages.

## Target Architecture

Use Kotlin, Jetpack Compose, Room, DataStore, Coroutines, Flow, WorkManager, and Health Connect.

Preferred package boundaries:

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

Use a single Android app module until there is a clear need to split Gradle modules.

## Data Strategy

The MVP should be local-first and Firebase-backed.

- Use Room as the source of truth for structured app data.
- Use DataStore as the source of truth for lightweight profile and preferences.
- Use Firebase for authentication, sync, backup, analytics, and crash reporting.
- Use Health Connect for user-granted workout and heart-rate reads.
- Use WorkManager for durable import/sync jobs.
- Save user-facing data locally first, then sync to Firebase when available.
- Firestore or Cloud Functions failures should not erase local data or block core breathwork tracking.

Primary models:

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

## Recommendation Rules

The recommendation engine should be deterministic before AI.

It must:

- Work without Health Connect.
- Choose exactly one primary breath skill.
- Explain why that skill was chosen.
- Provide a clear cue.
- Define what the app will measure or ask about later.

Avoid chatbot-style UX as the primary interface.

## Health Connect Rules

Request only permissions that support visible user-facing features.

MVP should read:

```text
Exercise sessions
Heart rate
Resting heart rate only if used by UI
```

Do not request sleep, HRV, or write permissions until the UI actually uses them.

Handle these states gracefully:

- Health Connect unavailable.
- Health Connect not installed.
- Permission denied.
- No recent workouts.
- Missing HR data.
- Sparse HR data.

## Safety

Never add protocols that involve:

- Max breath holds.
- Hyperventilation.
- Underwater breathwork.
- Pushing through dizziness, chest pain, faintness, or unusual symptoms.

Use safe language:

- "suggests"
- "may indicate"
- "trending"
- "compared with similar sessions"

Avoid:

- "proves"
- "diagnoses"
- "guarantees"

## Testing Expectations

Add unit tests for:

- RecommendationEngine.
- HeartRateAnalyzer.
- SimilarWorkoutMatcher.
- InsightGenerator.
- Seed breath skill integrity.

Do not depend on live Health Connect in unit tests. Use fake repositories and sample heart-rate data.

Run verification before claiming completion:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Build And Distribution

Local debug build:

```bash
./gradlew :app:assembleDebug
```

Firebase App Distribution is used for device review builds when explicitly requested by the user:

```bash
firebase appdistribution:distribute app/build/outputs/apk/debug/app-debug.apk --app 1:6129252807:android:c5ad0fead76231d00e81e2 --testers elliraynai.bot@gmail.com --project hybridtempo-ba273
```

Do not upload to Firebase App Distribution unless the user explicitly asks.
