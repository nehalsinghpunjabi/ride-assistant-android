package com.example.rideassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FailSafeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fail_safe)

        val installUberBtn = findViewById<Button>(R.id.installUberBtn)
        val installOlaBtn = findViewById<Button>(R.id.installOlaBtn)

        installUberBtn.setOnClickListener {
            openPlayStore("com.ubercab")
        }

        installOlaBtn.setOnClickListener {
            openPlayStore("com.olacabs.customer")
        }
    }

    private fun openPlayStore(packageName: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        startActivity(intent)
        finish()
    }
}
