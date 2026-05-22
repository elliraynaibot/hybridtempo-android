# Phase 1 Plan

## Goal

Build the first complete local MVP loop:

1. Onboard athlete context.
2. Complete a daily check-in.
3. Add workout context.
4. Receive a breathwork recommendation.
5. Complete a guided session.
6. See the session in history.

## Milestone 1: Native Shell

- Scaffold Kotlin Android project.
- Add Jetpack Compose and Material 3.
- Add app theme.
- Add simple navigation between Welcome, Check-In, Recommendation, Session, and History.
- Use fake repositories only.

## Milestone 2: Onboarding and Check-In

- Build onboarding form.
- Build quick daily check-in.
- Store state locally in ViewModel/fake repository.
- Keep check-in under 30 seconds.

## Milestone 3: Recommendation Engine

- Add deterministic protocol selection.
- Inputs: goal, time available, workout type, intensity, soreness, stress, energy.
- Output: category, title, duration, inhale/exhale pattern, rationale.

## Milestone 4: Session Player

- Build active session screen.
- Add timer.
- Add breathing animation.
- Add pause/resume.
- Save completed session locally.

## Milestone 5: History

- Show completed sessions.
- Show simple streak/count.
- Show recent recommendation categories.

## Milestone 6: Firebase Foundation

- Add Firebase Auth.
- Add Firestore persistence.
- Add Analytics and Crashlytics.
- Keep repository interfaces stable.

## Phase 1 Non-Goals

- Health Connect
- Wear OS
- Widgets
- Subscription
- Generic AI chat
- Workout tracking
- Calorie tracking

