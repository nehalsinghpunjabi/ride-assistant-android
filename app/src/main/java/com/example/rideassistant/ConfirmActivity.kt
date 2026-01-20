package com.example.rideassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class ConfirmActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)

        Log.d("ConfirmActivity", "Confirm screen loaded")

        userPrefs = UserPreferences(this)

        // 🔹 Values from AI
        var platform = intent.getStringExtra("platform")
        val pickup = intent.getStringExtra("pickup")
        val drop = intent.getStringExtra("drop")
        val rideType = intent.getStringExtra("ride_type")

        if (platform.isNullOrBlank() || pickup.isNullOrBlank() || drop.isNullOrBlank()) {
            Log.e("ConfirmActivity", "Missing intent data, closing")
            finish()
            return
        }

        // 🧠 SMART DEFAULT: platform
        if (platform == "unknown") {
            platform = userPrefs.getPreferredPlatform() ?: "uber"
        }

        // 🧠 SMART DEFAULT: ride type
        val finalRideType = getSmartRideType(rideType)

        // 🔹 UI refs
        val rideSummary = findViewById<TextView>(R.id.rideSummary)
        val confirmBtn = findViewById<Button>(R.id.confirmRideBtn)
        val cancelBtn = findViewById<Button>(R.id.cancelRideBtn)

        rideSummary.text =
            "Platform: $platform\nRide: $finalRideType\nFrom: $pickup\nTo: $drop"

        // ✅ CONFIRM BUTTON (only place where ride opens)
        confirmBtn.setOnClickListener {

            userPrefs.setPreferredPlatform(platform)
            userPrefs.setPreferredRideType(finalRideType)

            Toast.makeText(this, "Opening $platform…", Toast.LENGTH_SHORT).show()

            openRideApp(platform)
        }

        cancelBtn.setOnClickListener {
            Toast.makeText(this, "Ride cancelled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 🔹 Smart ride type logic
    private fun getSmartRideType(explicitRideType: String?): String {
        if (!explicitRideType.isNullOrBlank() && explicitRideType != "unknown") {
            return explicitRideType
        }

        val saved = userPrefs.getPreferredRideType()
        if (!saved.isNullOrBlank()) return saved

        val hour = java.util.Calendar.getInstance()
            .get(java.util.Calendar.HOUR_OF_DAY)

        return if (hour in 18..23 || hour in 0..5) "cab" else "auto"
    }

    // 🔹 Open Uber / Ola OR show fail-safe screen
    private fun openRideApp(platform: String) {

        val packageName = when (platform.lowercase()) {
            "uber" -> "com.ubercab"
            "ola" -> "com.olacabs.customer"
            else -> null
        }

        if (packageName == null) {
            finish()
            return
        }

        // 🚨 FAIL-SAFE: app not installed
        if (!AppUtils.isAppInstalled(this, packageName)) {
            Log.w("ConfirmActivity", "$platform not installed, opening FailSafe")

            val intent = Intent(this, FailSafeActivity::class.java)
            intent.putExtra("platform", platform)
            startActivity(intent)
            finish()
            return
        }

        // ✅ App installed → open deep link
        val uri = when (platform.lowercase()) {
            "uber" -> Uri.parse("uber://?action=setPickup&pickup=my_location")
            "ola" -> Uri.parse("ola://book?pickup=current")
            else -> null
        }

        if (uri != null) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        finish()
    }
}
