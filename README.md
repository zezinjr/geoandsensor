# Rastreador de Caminhada — (FPFtech)

Aplicativo Android de tela única desenvolvido para rastreamento de caminhada, combinando leitura contínua de localização por GPS e sensores nativos do dispositivo.

---

## 🛠️ Decisões Técnicas e Arquitetura

### 1. Arquitetura e Gestão de Estado (MVVM + StateFlow)
- **Estado Imutável:** O estado da UI é centralizado na *data class* `TrackingUiState` e exposto via `StateFlow` dentro do `TrackingViewModel`.
- **Sobrevivência à Rotação de Tela:** Por estar ancorado no `ViewModel`, todo o progresso da sessão (distância percorrida e contagem de passos) é preservado durante eventos de mudança de configuração, como a rotação do dispositivo, sem reinicializar os dados.

### 2. Gestão de Memória e Prevenção de Leaks (Lifecycle Safety)
- **Liberação de Listeners:** Para evitar vazamento de memória e consumo desnecessário de bateria, o desligamento dos recursos é garantido em dois níveis:
  - Na UI via `DisposableEffect` (interrompe o GPS quando a Composable sai da hierarquia).
  - No `ViewModel.onCleared()` com a execução explícita de `sensorManager.unregisterListener(this)` e `fusedLocationClient.removeLocationUpdates(...)`.

### 3. Localização e Modos de Precisão
- **Cálculo de Distância:** A distância percorrida é acumulada a cada nova posição recebida, calculando o deslocamento incremental entre a última coordenada válida (`lastLocation`) e a nova através do método nativo `Location.distanceTo`.
- **Alternância de Precisão:** O aplicativo permite chavear dinamicamente entre:
  - **Alta Precisão (`PRIORITY_HIGH_ACCURACY`):** Foco em GPS para maior exatidão.
  - **Economia de Bateria (`PRIORITY_BALANCED_POWER_ACCURACY`):** Uso de triangulação via Wi-Fi e redes celulares.

### 4. Sensores Nativos
- **Bússola (`TYPE_ROTATION_VECTOR`):** A leitura do vetor de rotação é convertida em matriz de orientação (`SensorManager.getOrientation`) para extrair o azimute em graus ($0^\circ$ a $360^\circ$).
- **Contador de Passos (`TYPE_STEP_COUNTER`):** Calcula a diferença relativa de passos desde o início da sessão a partir do valor acumulado desde o boot do sistema.

### 5. Tratamento de Casos Limite (Edge Cases)
- **Ausência de Sensor de Passos:** O sistema verifica a existência do sensor no hardware. Se `getDefaultSensor` retornar `null`, a propriedade `isStepCounterAvailable` é definida como `false`, exibindo um alerta visual informativo na UI sem causar crashes.
- **GPS Inativo:** O aplicativo monitora o estado de disponibilidade da localização (`onLocationAvailability`) e exibe um card de aviso vermelho caso o GPS do sistema seja desligado.

---
