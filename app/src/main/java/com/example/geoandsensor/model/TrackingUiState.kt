package com.example.geoandsensor.model

data class TrackingUiState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val totalDistance: Float = 0.0f,
    val azimuthDegrees: Int = 0,
    val stepCount: Int = 0,
    val isStepCounterAvailable: Boolean = true,
    val isHighAccuracy: Boolean = true,
    val isGpsEnabled: Boolean = true
)