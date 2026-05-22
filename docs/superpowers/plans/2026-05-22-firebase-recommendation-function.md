# Firebase Recommendation Function Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Firebase Cloud Functions callable endpoint that accepts the recommendation contract and returns a constrained breathwork protocol.

**Architecture:** Add a TypeScript Firebase Functions project under `functions/`. Export `recommendBreathwork` as a v2 callable function requiring Firebase Auth. Keep deterministic fallback server-side for now; Gemini can replace the selection logic later without changing the Android contract.

**Tech Stack:** Firebase Cloud Functions v2, TypeScript, Node.js 22, Firebase Admin SDK.

---

### Task 1: Functions Scaffold

- [ ] Add `functions/package.json`.
- [ ] Add `functions/tsconfig.json`.
- [ ] Update `firebase.json` with functions source/predeploy.

### Task 2: Callable Endpoint

- [ ] Add `recommendBreathwork`.
- [ ] Require authenticated callable context.
- [ ] Validate profile, check-in, and trend fields.
- [ ] Return constrained protocol response.

### Task 3: Verification

- [ ] Install function dependencies.
- [ ] Run `npm --prefix functions run build`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Commit with `feat: add firebase recommendation function`.
