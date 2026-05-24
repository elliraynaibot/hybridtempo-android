# AI Recommendation Contract

HybridTempo should not call an AI model directly from the Android app. The app sends structured athlete state to a backend endpoint and receives a constrained breathwork protocol.

## Request

```json
{
  "profile": {
    "trainingStyle": "Hybrid",
    "weeklyTrainingFrequency": 5,
    "goals": ["recovery", "race prep"],
    "preferredSessionLength": 5,
    "raceName": "HYROX Toronto",
    "raceDate": "2026-10-01"
  },
  "checkIn": {
    "energy": 6,
    "soreness": 7,
    "stress": 5,
    "workoutType": "Hybrid",
    "workoutIntensity": 8,
    "timeAvailable": 5
  },
  "recentTrends": {
    "energy": [6, 7, 5],
    "soreness": [7, 6, 8],
    "stress": [5, 6, 7]
  }
}
```

## Response

```json
{
  "protocol": "Downregulation",
  "durationMinutes": 5,
  "rationale": "High training load with elevated soreness.",
  "cadence": "4 second inhale · 6 second exhale",
  "quota": {
    "limit": 5,
    "used": 1,
    "remaining": 4,
    "resetDate": "2026-05-22"
  },
  "breathworkProtocol": {
    "category": "downregulation",
    "title": "Downregulation",
    "durationMinutes": 5,
    "ambientTrackName": "ambient_loop",
    "phases": [
      {
        "label": "Inhale",
        "seconds": 4,
        "instruction": "Draw air in through the nose",
        "scaleTarget": 1.12
      },
      {
        "label": "Exhale",
        "seconds": 6,
        "instruction": "Let the exhale do the work",
        "scaleTarget": 0.72
      }
    ]
  }
}
```

## Guardrails

- The backend must return structured JSON only.
- The app should validate duration, phase seconds, phase labels, and protocol category.
- If validation fails, use the deterministic fallback.
- The AI should choose or adapt safe templates, not invent medical or therapeutic advice.
- The backend enforces a server-side cap of 5 AI recommendations per user per UTC day.
- If the daily cap is reached, the function returns `resource-exhausted`; the app should use the deterministic fallback and explain that the local protocol still works.

## Android Integration

The Android app calls the callable Firebase Function:

```text
recommendBreathwork
```

Requirements:

- `app/google-services.json` must exist.
- Anonymous Auth must be enabled.
- The user must be signed in before the callable is invoked.
- The function must be deployed to `us-central1`.

If the callable fails because Firebase is missing, the user is offline, Auth is unavailable, the quota is exhausted, or the response is invalid, the app uses `DeterministicRecommendationEngine`.

The app should only call the backend when the user taps `Get recommendation`. Live slider edits should use the deterministic local preview so casual adjustments do not consume quota.
