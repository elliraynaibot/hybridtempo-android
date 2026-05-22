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
