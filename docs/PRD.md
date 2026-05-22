# HybridTempo Product Requirements Document

## Product Vision

HybridTempo is an Android-first AI-guided breathwork and recovery app designed for hybrid athletes.

The app helps athletes regulate their nervous system before and after training using personalized breathwork recommendations informed by workout intensity, recovery state, readiness, race preparation, and training context.

HybridTempo is not a generic meditation app, therapy app, chatbot wellness assistant, or workout tracker.

HybridTempo is:

> Breathwork designed around how you train.

## Target Audience

Primary users:

- HYROX athletes
- Hybrid athletes
- Runners
- Functional fitness athletes
- CrossFit and F45 trainees
- Recovery-focused athletes
- Android wearable users

Secondary users:

- Endurance athletes
- Wellness-oriented athletes
- Nervous-system-aware fitness users

## MVP Goal

Deliver a focused recovery and regulation loop:

1. User logs workout context or connects training data.
2. User completes a quick readiness check-in.
3. HybridTempo recommends a breathwork session.
4. User completes the session.
5. User builds awareness of nervous system state over time.

## Phase 1 MVP Features

### Onboarding

Required inputs:

- Training style
- Weekly training frequency
- Preferred session duration
- Goals

Optional inputs:

- Race date
- Health Connect intent

Goals:

- Recovery
- Activation
- Focus
- Race prep
- Sleep support

### Daily Check-In

Inputs:

- Energy
- Soreness
- Stress
- Mood
- Time available
- Training completed today
- Workout intensity

UX target: complete in less than 30 seconds.

### Workout Context

Manual MVP workout types:

- Intervals
- Strength
- Hybrid
- Conditioning
- Recovery
- Race simulation
- Long run
- Mobility

Additional inputs:

- Duration
- Intensity, 1-10
- Workout time

### Recommendation Engine

Purpose: generate a matched breathwork session based on workout load, recovery state, time available, race proximity, soreness, stress, and sleep quality.

AI should act as a recommendation and protocol adaptation layer. It should not act as an open-ended chatbot, therapist, or medical diagnosis system.

### Breathwork Categories

- Activation
- Downregulation
- Sleep Transition
- Recovery
- Race Prep

### Audio System

MVP:

- Pre-recorded audio
- Ambient sound support
- Breathing animation
- Timer
- Inhale/exhale pacing

Session lengths:

- 3 minutes
- 5 minutes
- 10 minutes

### Core Screens

- Landing / Welcome
- Onboarding
- Daily Check-In
- Recommendation
- Active Session Player
- History

## Android Direction

MVP:

- Native Android app
- Material 3
- Android-first UX
- Health Connect-ready architecture

Future:

- Health Connect integration
- Wear OS support
- Android widgets
- Quick-launch recovery actions
- Training-aware notifications

## Non-Goals

Do not build:

- Social feed
- Workout marketplace
- Calorie tracker
- Generic AI chat
- Training plan generator
- Therapy features
- Medical advice system

