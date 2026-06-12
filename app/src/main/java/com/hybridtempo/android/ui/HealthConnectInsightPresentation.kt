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
            body = "Health Connect is not available on this device. You can still rate breath control and compare patterns over time.",
            actionLabel = null,
        )

        availability == HealthConnectAvailability.UpdateRequired -> HealthConnectInsightPresentation(
            title = "HR-backed review",
            body = "Update Health Connect to add heart rate context after training.",
            actionLabel = "Update Health Connect",
        )

        enabled -> HealthConnectInsightPresentation(
            title = "HR-backed review",
            body = "After your workout syncs, use heart rate context to see whether breathing helped keep effort steadier.",
            actionLabel = null,
        )

        else -> HealthConnectInsightPresentation(
            title = "HR-backed review",
            body = "Connect Health Connect after training to compare your breath-control rating with heart rate response.",
            actionLabel = "Connect in settings",
        )
    }
}
