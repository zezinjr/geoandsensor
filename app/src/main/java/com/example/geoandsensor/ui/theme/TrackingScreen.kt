package com.example.geoandsensor.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.geoandsensor.model.TrackingUiState
import com.example.geoandsensor.viewmodel.TrackingViewModel

@Composable
fun TrackingScreen(viewModel: TrackingViewModel) {
    val uiState: TrackingUiState by viewModel.uiState.collectAsState(initial = TrackingUiState())

    DisposableEffect(Unit) {
        viewModel.startLocationUpdates()
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rastreador de Caminhada", style = MaterialTheme.typography.headlineMedium)

        // Linha 39: Checagem booleana do GPS
        if (!uiState.isGpsEnabled) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))) {
                Text(
                    "Sinal de GPS ausente ou desativado!",
                    color = Color.Red,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Latitude: ${uiState.latitude}")
                Text("Longitude: ${uiState.longitude}")
                Text("Precisão: ${uiState.accuracy} m")
                Text("Distância Acumulada: %.2f m".format(uiState.totalDistance))
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Azimute (Bússola): ${uiState.azimuthDegrees}°")

                Spacer(modifier = Modifier.height(8.dp))

                // Checagem booleana do sensor de passos
                if (uiState.isStepCounterAvailable) {
                    Text("Passos na sessão: ${uiState.stepCount}")
                } else {
                    Text(
                        "Aviso: Sensor de passos indisponível neste dispositivo.",
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.toggleAccuracyMode() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isHighAccuracy) "Modo: Alta Precisão (GPS)"
                else "Modo: Economia de Bateria"
            )
        }
    }
}