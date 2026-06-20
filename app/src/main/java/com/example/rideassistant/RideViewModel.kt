package com.example.rideassistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RideViewModel @Inject constructor(
    private val parser: RideIntentParser,
    private val repository: RideRepository
) : ViewModel() {
    private val _state = MutableLiveData<RideUiState>(RideUiState.Idle)
    val state: LiveData<RideUiState> = _state

    fun process(text: String) {
        parser.parse(text).fold(onSuccess = { parsed ->
            val pickup = repository.resolveSavedPlace(parsed.pickup)
            if (pickup == null) return@fold _state.postValue(RideUiState.NeedsSavedPlace(parsed.pickup))
            val destination = repository.resolveSavedPlace(parsed.destination)
            if (destination == null) return@fold _state.postValue(RideUiState.NeedsSavedPlace(parsed.destination))
            val request = parsed.copy(pickup = pickup, destination = destination)
            _state.postValue(RideUiState.Ready(request, repository.options(request)))
        }, onFailure = { _state.postValue(RideUiState.Error(it.message ?: "I couldn't understand that request.")) })
    }

    fun savePlace(name: String, address: String) {
        if (address.isBlank()) _state.value = RideUiState.Error("Enter an address for $name.")
        else { repository.savePlace(name, address); process("take me to $name") }
    }
}
