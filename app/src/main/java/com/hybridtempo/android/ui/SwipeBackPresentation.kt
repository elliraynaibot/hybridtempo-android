package com.hybridtempo.android.ui

private const val SwipeBackThreshold = 90f

fun swipeBackIndicatorProgress(dragDistance: Float): Float {
    return (dragDistance / SwipeBackThreshold).coerceIn(0f, 1f)
}

fun swipeBackThreshold(): Float = SwipeBackThreshold
