package com.hybridtempo.android.ui

import com.hybridtempo.android.health.HealthConnectAvailability
import com.hybridtempo.android.health.HealthConnectUiStatus

data class ProfileHealthConnectPresentation(
    val title: String,
    val statusLabel: String,
    val body: String,
    val dataDiagnostic: String,
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
        body = "Connect Google Health through Health Connect so HybridTempo can use completed workouts as context. Breath checks still work without it.",
        dataDiagnostic = toHealthDataDiagnostic(enabled = enabled),
        connectAction = if (enabled) "Refresh" else "Connect",
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

private fun HealthConnectUiStatus.toHealthDataDiagnostic(
    enabled: Boolean,
): String = when {
    availability != HealthConnectAvailability.Available -> "Workout import is unavailable on this device."
    !enabled -> "Optional: connect later if you want workout imports."
    metrics?.hasData == true -> "Recent health data is available."
    else -> "Connected. Waiting for recent Google Health data."
}
