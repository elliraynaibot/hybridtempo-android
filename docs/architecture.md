# HybridTempo Architecture

## Direction

HybridTempo is a native Android app built with Kotlin, Jetpack Compose, and Material 3.

The app should feel Android-native rather than cross-platform. Use platform conventions, Compose state patterns, Android permissions, and Android health integrations directly.

## App Architecture

Use MVVM with repository boundaries:

- Compose screens render immutable UI state and emit user events.
- ViewModels own screen state and orchestration.
- Repositories own persistence, Firebase, audio metadata, and future Health Connect access.
- Domain use cases own recommendation logic and protocol selection.

## Initial Modules

Start simple in a single Android app module:

- `core/design`
- `core/model`
- `core/data`
- `feature/onboarding`
- `feature/checkin`
- `feature/recommendation`
- `feature/session`
- `feature/history`

If the codebase grows, split into Gradle modules later. Do not over-modularize the first milestone.

## Backend

Firebase is the primary backend:

- Firebase Auth
- Firestore
- Firebase Storage
- Firebase Analytics
- Crashlytics
- Firebase Cloud Messaging

Phase 1 can use fake/local repositories until the user loop feels right. Add Firebase once the local MVP flow is usable.

## Recommendation Engine

Start deterministic and local:

- Map check-in plus workout context to protocol category.
- Choose duration based on user preference and time available.
- Return a clear rationale.

Later, add Gemini through Firebase AI Logic or Cloud Functions for personalization and copy variation. AI should adapt protocols and explanation, not become an unrestricted chatbot.

## Audio

Use Media3 / ExoPlayer for audio playback.

MVP can start with local placeholder audio or silent timed protocols while the interaction model is built. Firebase Storage can host production audio later.

## Health Connect

Health Connect is Phase 2. Keep repository interfaces ready for imported workouts, sleep, heart rate, and activity duration, but do not block Phase 1 on permissions and health data complexity.

