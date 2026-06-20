package com.example.rideassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("ride_places", Context.MODE_PRIVATE)
    private val savedNames = setOf("home", "work", "college")

    fun resolveSavedPlace(value: String): String? {
        val key = value.trim().lowercase()
        return if (key in savedNames) prefs.getString("place_$key", null) else value
    }

    fun savePlace(name: String, address: String) {
        prefs.edit().putString("place_${name.lowercase()}", address.trim()).apply()
    }

    fun options(request: RideRequest): List<RideOption> {
        // These ranges are deliberately labelled estimates: live provider APIs are not public.
        val type = if (request.rideType == RideType.ANY) RideType.AUTO else request.rideType
        return listOf(
            RideOption(RideProvider.RAPIDO, "₹90–160", "4–9 min", type),
            RideOption(RideProvider.OLA, "₹110–190", "5–10 min", type),
            RideOption(RideProvider.UBER, "₹120–210", "4–8 min", type)
        ).let { all -> request.provider?.let { p -> all.sortedBy { it.provider != p } } ?: all }
    }

    fun providerIntent(request: RideRequest, provider: RideProvider): Intent {
        val pickup = Uri.encode(request.pickup)
        val drop = Uri.encode(request.destination)
        val uri = when (provider) {
            RideProvider.UBER -> if (request.pickup.equals("current location", true))
                Uri.parse("uber://?action=setPickup&pickup=my_location&dropoff[formatted_address]=$drop")
            else Uri.parse("uber://?action=setPickup&pickup[formatted_address]=$pickup&dropoff[formatted_address]=$drop")
            RideProvider.OLA -> Uri.parse("olacabs://app/launch?lat=0&lng=0&drop_name=$drop")
            RideProvider.RAPIDO -> Uri.parse("rapido://ride?pickup=$pickup&drop=$drop")
        }
        return Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
    }

    fun fallbackIntent(request: RideRequest): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(request.pickup)}&destination=${Uri.encode(request.destination)}&travelmode=driving")
    )
}
