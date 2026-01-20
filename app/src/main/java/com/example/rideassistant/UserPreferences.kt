package com.example.rideassistant

import android.content.Context

class UserPreferences(context: Context) {

    private val prefs =
        context.getSharedPreferences("ride_prefs", Context.MODE_PRIVATE)

    fun setPreferredPlatform(platform: String) {
        prefs.edit().putString("preferred_platform", platform).apply()
    }

    fun getPreferredPlatform(): String? {
        return prefs.getString("preferred_platform", null)
    }

    fun setPreferredRideType(type: String) {
        prefs.edit().putString("preferred_ride_type", type).apply()
    }

    fun getPreferredRideType(): String? {
        return prefs.getString("preferred_ride_type", null)
    }
    fun setSilentHomeWork(enabled: Boolean) {
        prefs.edit().putBoolean("silent_home_work", enabled).apply()
    }

    fun isSilentHomeWorkEnabled(): Boolean {
        return prefs.getBoolean("silent_home_work", false)
    }

}
