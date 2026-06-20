package com.example.rideassistant

import java.util.Locale
import javax.inject.Inject

class RideIntentParser @Inject constructor() {
    fun parse(raw: String): Result<RideRequest> = runCatching {
        val text = raw.trim().replace(Regex("[.?!]+$"), "")
        require(text.isNotBlank()) { "I didn't hear a ride request." }
        val lower = text.lowercase(Locale.ROOT)
        val provider = when {
            Regex("\\buber\\b").containsMatchIn(lower) -> RideProvider.UBER
            Regex("\\bola\\b").containsMatchIn(lower) -> RideProvider.OLA
            Regex("\\brapido\\b").containsMatchIn(lower) -> RideProvider.RAPIDO
            else -> null
        }
        val type = when {
            Regex("\\b(bike|motorbike|bike taxi)\\b").containsMatchIn(lower) -> RideType.BIKE
            Regex("\\b(auto|rickshaw|tuk ?tuk)\\b").containsMatchIn(lower) -> RideType.AUTO
            Regex("\\b(cab|taxi|car)\\b").containsMatchIn(lower) -> RideType.CAB
            else -> RideType.ANY
        }
        val fromTo = Regex("\\bfrom\\s+(.+?)\\s+to\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
        val pickup: String
        val destination: String
        if (fromTo != null) {
            pickup = clean(fromTo.groupValues[1])
            destination = clean(fromTo.groupValues[2])
        } else {
            pickup = "current location"
            val to = Regex("\\b(?:to|for)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
            destination = clean(to?.groupValues?.get(1) ?: when {
                Regex("\\b(take|drive) me home\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "home"
                else -> throw IllegalArgumentException("Please say a destination, for example ‘ride to Pune Airport’."
                )
            })
        }
        require(destination.isNotBlank()) { "Please say where you want to go." }
        RideRequest(pickup, destination, type, provider)
    }

    private fun clean(value: String): String = value
        .replace(Regex("\\b(?:using|with|by)\\s+(?:uber|ola|rapido)\\b.*$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\b(?:in|by)\\s+(?:an?\\s+)?(?:auto|cab|taxi|bike)\\b.*$", RegexOption.IGNORE_CASE), "")
        .trim(' ', ',', '.')
}
