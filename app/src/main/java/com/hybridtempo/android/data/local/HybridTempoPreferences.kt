package com.hybridtempo.android.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object HybridTempoPreferences {
    val HasCompletedOnboarding = booleanPreferencesKey("has_completed_onboarding")
    val PreferredSessionMinutes = intPreferencesKey("preferred_session_minutes")
    val EveningReminderEnabled = booleanPreferencesKey("evening_reminder_enabled")
    val EveningReminderHour = intPreferencesKey("evening_reminder_hour")
    val EveningReminderMinute = intPreferencesKey("evening_reminder_minute")
    val HealthConnectEnabled = booleanPreferencesKey("health_connect_enabled")
    val FirebaseSyncEnabled = booleanPreferencesKey("firebase_sync_enabled")
    val LastFirebaseSyncAt = stringPreferencesKey("last_firebase_sync_at")
}
