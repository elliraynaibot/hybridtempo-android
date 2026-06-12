package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectUiStatus

data class HealthConnectInsightPresentation(
    val title: String,
    val body: String,
    val actionLabel: String?,
)

fun HealthConnectUiStatus.toHealthConnectInsightPresentation(): HealthConnectInsightPresentation {
    return when {
        availability == HealthConnectAvailability.Unavailable -> HealthConnectInsightPresentation(
            title = "Manual review",
            body = "Health Connect is not available on this device. Breath checks and reflections still give you a useful before-and-after signal.",
            actionLabel = null,
        )

        availability == HealthConnectAvailability.UpdateRequired -> HealthConnectInsightPresentation(
            title = "Optional workout context",
            body = "Update Health Connect if you want completed workouts to prefill the review. Breath checks still work without it.",
            actionLabel = "Update Health Connect",
        )

        enabled -> HealthConnectInsightPresentation(
            title = "Optional workout context",
            body = "Completed workouts can help label the session. Breath rhythm checks are the main before-and-after signal.",
            actionLabel = null,
        )

        else -> HealthConnectInsightPresentation(
            title = "Optional workout context",
            body = "Connect Health Connect later to import completed workouts. You can still use breath checks before and after training now.",
            actionLabel = "Connect in settings",
        )
    }
}
