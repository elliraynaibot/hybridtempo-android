# Onboarding Profile Design

## Goal

Add a first-run onboarding flow that captures athlete context and persists it as the reusable profile for future recommendations.

## Approach

Use first-run onboarding plus an editable profile path later. For this checkpoint, onboarding is shown before the main app until the user saves a profile in the current app session. The profile is written through the existing repository to `users/{uid}` when Firebase is configured.

## Inputs

- Training style: hybrid, running, strength, functional fitness, recovery focused
- Weekly training frequency: 1-7 sessions
- Preferred session duration: 3, 5, or 10 minutes
- Goals: recovery, activation, focus, race prep, sleep support
- Optional race date as a text field for now

## Data Flow

The Compose onboarding screen edits an `AthleteProfileDraft`. `HybridTempoViewModel` converts the draft to `AthleteProfile`, saves it through `HybridTempoRepository.upsertProfile`, and updates app state. The recommendation engine uses `preferredSessionLength` and selected goals as defaults.

## Error Handling

If Firebase is not configured, the app keeps running and shows the existing preview persistence message. Once `app/google-services.json` is present and Anonymous Auth is enabled, profile saves write to Firestore.

## Non-Goals

- Full settings screen
- Native date picker
- Google sign-in
- Health Connect
