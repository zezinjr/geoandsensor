package com.example.geoandsensor.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.example.geoandsensor.model.TrackingUiState
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TrackingViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val sensorManager = application.getSystemService(Application.SENSOR_SERVICE) as SensorManager

    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var initialStepCount: Int? = null

    init {
        setupSensors()
    }

    private fun setupSensors() {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            _uiState.update { it.copy(isStepCounterAvailable = false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        stopLocationUpdates()

        val priority = if (_uiState.value.isHighAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val locationRequest = LocationRequest.Builder(priority, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { newLoc ->
                    val currentDistance = _uiState.value.totalDistance
                    val addedDistance = lastLocation?.distanceTo(newLoc) ?: 0f

                    lastLocation = newLoc

                    _uiState.update { state ->
                        state.copy(
                            latitude = newLoc.latitude,
                            longitude = newLoc.longitude,
                            accuracy = newLoc.accuracy,
                            totalDistance = currentDistance + addedDistance,
                            isGpsEnabled = true
                        )
                    }
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                _uiState.update { it.copy(isGpsEnabled = availability.isLocationAvailable) }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun toggleAccuracyMode() {
        _uiState.update { it.copy(isHighAccuracy = !it.isHighAccuracy) }
        startLocationUpdates()
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                var azimuth = Math.toDegrees(orientation[0].toDouble()).toInt()
                if (azimuth < 0) azimuth += 360

                _uiState.update { it.copy(azimuthDegrees = azimuth) }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt()
                if (initialStepCount == null) {
                    initialStepCount = totalStepsSinceBoot
                }
                val sessionSteps = totalStepsSinceBoot - (initialStepCount ?: totalStepsSinceBoot)
                _uiState.update { it.copy(stepCount = sessionSteps) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }
}