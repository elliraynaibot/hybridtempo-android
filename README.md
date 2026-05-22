# HybridTempo Android

HybridTempo is an Android-first AI-guided breathwork and recovery app for hybrid athletes.

Positioning:

> Breathwork designed around how you train.

This repo is intentionally clean and native Android-first. The first milestone is a focused Phase 1 MVP with onboarding, daily check-in, workout context, breathwork recommendations, an active session player, and history.

## Local Build

```bash
./gradlew :app:assembleDebug
```

## Firebase Setup

The app is wired for Firebase Auth and Firestore, but it does not commit Firebase secrets.

1. Create/register an Android app in Firebase with package name `com.hybridtempo.android`.
2. Download `google-services.json`.
3. Place it at `app/google-services.json`.
4. Rebuild the app.

Without `app/google-services.json`, the UI still runs but shows preview persistence messaging instead of writing to Firestore.

See [docs/firebase-setup.md](docs/firebase-setup.md) for Auth and Firestore rules setup.

## Ambient Audio

The protocol player supports an optional looping ambient track.

Add a file such as `ambient_loop.mp3` or `ambient_loop.wav` at:

```text
app/src/main/res/raw/ambient_loop.mp3
```

If the file is missing, the app keeps working and the ambient toggle safely no-ops.
