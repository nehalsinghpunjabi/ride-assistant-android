package com.example.rideassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class RideTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

        // 🔹 Start background handler
        val serviceIntent = Intent(context, RideService::class.java)
        serviceIntent.putExtra("voice_text", text)
        context.startForegroundService(serviceIntent)

        Toast.makeText(
            context,
            "Ride command received",
            Toast.LENGTH_SHORT
        ).show()
    }
}
