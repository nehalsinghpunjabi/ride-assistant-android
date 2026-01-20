package com.example.rideassistant

import android.content.ActivityNotFoundException
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.util.Log


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Get text sent by Google Assistant (or test input)
        val spokenText =
            intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra("query")
                ?: return

        sendToAI(spokenText)
    }

    private fun sendToAI(text: String) {
        Log.d("RideAI", "sendToAI called with: $text")

        val client = OkHttpClient()

        val body = """
        { "text": "$text" }
    """.trimIndent()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:3000/ride")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                Log.d("RideAI", "HTTP response received")

                val responseBody = response.body?.string()
                Log.d("RideAI", "Response body: $responseBody")

                if (responseBody == null) return

                val json = JSONObject(responseBody)
                handleAction(json)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("RideAI", "HTTP call failed", e)
            }
        })
    }

    private fun openRideDirectly(platform: String) {
        val uri = when (platform.lowercase()) {
            "uber" -> Uri.parse("uber://?action=setPickup&pickup=my_location")
            "ola" -> Uri.parse("ola://book?pickup=current")
            else -> return
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            val storeUri = Uri.parse(
                if (platform.lowercase() == "uber")
                    "market://details?id=com.ubercab"
                else
                    "market://details?id=com.olacabs.customer"
            )
            startActivity(Intent(Intent.ACTION_VIEW, storeUri))
        }

        finish()
    }


    private fun handleAction(json: JSONObject) {

        if (json.optString("action") != "OPEN_RIDE_APP") return

        val platform = json.optString("platform")
        val pickup = json.optString("pickup")
        val drop = json.optString("drop")
        val rideType = json.optString("ride_type")

        val userPrefs = UserPreferences(this)

        if (
            userPrefs.isSilentHomeWorkEnabled() &&
            pickup == "current_location" &&
            (drop == "home" || drop == "work")
        ) {
            Toast.makeText(
                this,
                "Booking $platform to $drop…",
                Toast.LENGTH_SHORT
            ).show()

            openRideDirectly(platform)
            return
        }


        val confirmIntent = Intent(this, ConfirmActivity::class.java)
        confirmIntent.putExtra("platform", platform)
        confirmIntent.putExtra("pickup", json.optString("pickup"))
        confirmIntent.putExtra("drop", json.optString("drop"))
        confirmIntent.putExtra("ride_type", json.optString("ride_type"))

        startActivity(confirmIntent)
        finish()

    }



}