package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectUiStatus

data class ProfileHealthConnectPresentation(
    val title: String,
    val statusLabel: String,
    val body: String,
    val connectAction: String,
    val canConnect: Boolean,
)

fun HealthConnectUiStatus.toProfileHealthConnectPresentation(
    enabled: Boolean,
): ProfileHealthConnectPresentation {
    val canConnect = availability == HealthConnectAvailability.Available
    return ProfileHealthConnectPresentation(
        title = "Google Health Connect",
        statusLabel = toProfileHealthStatusLabel(enabled),
        body = "Connect Google Health through Health Connect so HybridTempo can use completed workouts, sleep, and resting heart rate when available.",
        connectAction = if (enabled) "Refresh connection" else "Connect Google Health",
        canConnect = canConnect,
    )
}

private fun HealthConnectUiStatus.toProfileHealthStatusLabel(enabled: Boolean): String = when {
    availability == HealthConnectAvailability.Unavailable -> "Unavailable on this device"
    availability == HealthConnectAvailability.UpdateRequired -> "Update Health Connect to connect"
    enabled && metrics?.hasData == true -> "Connected: recent health data is available"
    enabled && hasRequiredPermissions -> "Connected: waiting for recent Google Health data"
    enabled -> "Enabled, but permissions need review"
    else -> "Not connected"
}
