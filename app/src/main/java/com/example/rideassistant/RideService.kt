package com.example.rideassistant

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.widget.Toast

class RideService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, notification())

        val text = intent?.getStringExtra("voice_text") ?: ""

        // 🔹 VERY basic trigger (AI already handles parsing)
        val uri = Uri.parse("uber://?action=setPickup&pickup=my_location")

        Toast.makeText(
            this,
            "Opening ride app…",
            Toast.LENGTH_SHORT
        ).show()

        val rideIntent = Intent(Intent.ACTION_VIEW, uri)
        rideIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(rideIntent)

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val channelId = "ride_service"

        val channel = NotificationChannel(
            channelId,
            "Ride Assistant",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Ride Assistant")
            .setContentText("Processing ride command")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}
