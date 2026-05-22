# Local Recovery Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local Android notifications for a daily evening recovery reminder.

**Architecture:** Store reminder preferences on the athlete profile. Request Android notification permission from the profile form. Schedule an inexact daily `AlarmManager` reminder locally on the device, delivered by a `BroadcastReceiver`.

**Tech Stack:** Kotlin, Jetpack Compose, Android notification channels, AlarmManager, NotificationCompat.

---

### Task 1: Notification Infrastructure

- [ ] Add notification permission.
- [ ] Add reminder receiver.
- [ ] Add scheduler that creates a channel and schedules/cancels inexact daily alarms.

### Task 2: Profile Settings

- [ ] Add reminder settings to `AthleteProfile`.
- [ ] Persist reminder settings to Firestore.
- [ ] Add evening reminder toggle and time presets to profile settings.

### Task 3: Verify and Commit

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add local recovery reminders`.
