# Firebase Setup

HybridTempo uses Firebase Auth and Firestore for user-scoped app data.

## Android App Config

1. Open Firebase Console.
2. Create or select a Firebase project.
3. Add an Android app with package name `com.hybridtempo.android`.
4. Download `google-services.json`.
5. Place the file at `app/google-services.json`.

The file is intentionally ignored by git.

## Authentication

Enable Anonymous Authentication for the current MVP:

1. Firebase Console > Authentication > Sign-in method.
2. Enable `Anonymous`.

This lets the app create a Firebase user immediately without building full account onboarding first.

## Firestore

Deploy security rules from this repo:

```bash
firebase deploy --only firestore
```

The rules restrict all user data to:

```text
users/{firebaseAuthUid}
```

Each signed-in user can only read and write their own profile, check-ins, and sessions.
