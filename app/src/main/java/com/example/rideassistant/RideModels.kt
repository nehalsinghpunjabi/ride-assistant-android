package com.example.rideassistant

data class RideRequest(
    val pickup: String = "current location",
    val destination: String,
    val rideType: RideType = RideType.ANY,
    val provider: RideProvider? = null
)

enum class RideType(val label: String) { ANY("Any ride"), BIKE("Bike"), AUTO("Auto"), CAB("Cab") }
enum class RideProvider(val label: String) { UBER("Uber"), OLA("Ola"), RAPIDO("Rapido") }

data class RideOption(
    val provider: RideProvider,
    val estimatedFare: String,
    val estimatedEta: String,
    val rideType: RideType,
    val isEstimate: Boolean = true
)

sealed interface RideUiState {
    data object Idle : RideUiState
    data object Locating : RideUiState
    data class Ready(val request: RideRequest, val options: List<RideOption>) : RideUiState
    data class NeedsSavedPlace(val name: String) : RideUiState
    data class Error(val message: String) : RideUiState
}
