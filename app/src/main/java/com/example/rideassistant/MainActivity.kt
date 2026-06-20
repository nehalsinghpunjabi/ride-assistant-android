package com.example.rideassistant

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), RecognitionListener {
    private val viewModel: RideViewModel by viewModels()
    private lateinit var speech: SpeechRecognizer
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var summary: TextView
    private lateinit var options: LinearLayout
    private var retryCount = 0

    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else showError("Microphone permission is required for voice commands. You can still type a request.")
    }
    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) showCurrentLocation() else showError("Location permission denied. Type a pickup address or enable it in Settings.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        input = findViewById(R.id.rideInput)
        status = findViewById(R.id.statusText)
        summary = findViewById(R.id.rideSummary)
        options = findViewById(R.id.providerOptions)
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }

        findViewById<View>(R.id.voiceButton).setOnClickListener { requestVoice() }
        findViewById<View>(R.id.searchButton).setOnClickListener { viewModel.process(input.text.toString()) }
        findViewById<View>(R.id.homeButton).setOnClickListener { viewModel.process("take me home") }
        findViewById<View>(R.id.workButton).setOnClickListener { viewModel.process("take me to work") }
        findViewById<View>(R.id.collegeButton).setOnClickListener { viewModel.process("take me to college") }
        viewModel.state.observe(this, ::render)

        handleExternalIntent(intent)
        requestLocation()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleExternalIntent(intent) }

    private fun handleExternalIntent(intent: Intent) {
        val text = when {
            intent.data?.scheme == "rideassistant" -> {
                val pickup = intent.data?.getQueryParameter("pickup")
                val destination = intent.data?.getQueryParameter("destination")
                val type = intent.data?.getQueryParameter("type")
                listOfNotNull("book", type, "from", pickup, "to", destination).joinToString(" ")
            }
            else -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra("query")
        }
        if (!text.isNullOrBlank()) { input.setText(text); viewModel.process(text) }
    }

    private fun requestVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return showError("Speech recognition is unavailable. Install or enable the Google speech service.")
        retryCount = 0
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
        else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        status.text = getString(R.string.listening)
        speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
        })
    }

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) showCurrentLocation() else locationPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun showCurrentLocation() {
        val manager = getSystemService(android.location.LocationManager::class.java)
        if (!manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder(this).setTitle("Location is off").setMessage("Turn on location services to use your current pickup.")
                .setPositiveButton("Open settings") { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }.setNegativeButton("Not now", null).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
            if (location == null) return@addOnSuccessListener showError("No recent location is available. Go outdoors, enable precise location, and retry.")
            val point = LatLng(location.latitude, location.longitude)
            (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync { map ->
                map.clear(); map.addMarker(MarkerOptions().position(point).title("Current pickup")); map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
            }
        }.addOnFailureListener { showError("Couldn't get your location. Check Google Play services and try again.") }
    }

    private fun render(state: RideUiState) {
        when (state) {
            RideUiState.Idle -> status.text = getString(R.string.ready)
            RideUiState.Locating -> status.text = getString(R.string.locating)
            is RideUiState.Error -> showError(state.message)
            is RideUiState.NeedsSavedPlace -> promptSavedPlace(state.name)
            is RideUiState.Ready -> {
                status.text = getString(R.string.review_ride)
                summary.visibility = View.VISIBLE
                summary.text = getString(R.string.route_summary, state.request.pickup, state.request.destination, state.request.rideType.label)
                renderOptions(state)
            }
        }
    }

    private fun renderOptions(state: RideUiState.Ready) {
        options.removeAllViews()
        state.options.forEachIndexed { index, option ->
            val button = Button(this).apply {
                text = getString(R.string.provider_option, option.provider.label, option.estimatedFare, option.estimatedEta) + if (index == 0) "  • Recommended estimate" else ""
                isAllCaps = false
                setOnClickListener { openProvider(state.request, option.provider) }
            }
            options.addView(button)
        }
        findViewById<TextView>(R.id.estimateDisclaimer).visibility = View.VISIBLE
    }

    private fun openProvider(request: RideRequest, provider: RideProvider) {
        val repository = RideRepository(applicationContext)
        try { startActivity(repository.providerIntent(request, provider)) }
        catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "${provider.label} isn't installed; opening route preview.", Toast.LENGTH_LONG).show()
            startActivity(repository.fallbackIntent(request))
        }
    }

    private fun promptSavedPlace(name: String) {
        val field = EditText(this).apply { hint = "Full address for $name" }
        AlertDialog.Builder(this).setTitle("Set ${name.replaceFirstChar { it.uppercase() }}")
            .setMessage("This saved place has not been configured yet.").setView(field)
            .setPositiveButton("Save") { _, _ -> viewModel.savePlace(name, field.text.toString()) }.setNegativeButton("Cancel", null).show()
    }

    private fun showError(message: String) { status.text = message; Toast.makeText(this, message, Toast.LENGTH_LONG).show() }

    override fun onReadyForSpeech(params: Bundle?) { status.text = getString(R.string.speak_now) }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) { findViewById<ProgressBar>(R.id.voiceLevel).progress = ((rmsdB + 2) * 7).toInt().coerceIn(0, 100) }
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { status.text = getString(R.string.processing) }
    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_NO_MATCH && retryCount++ < 1) { speech.cancel(); startListening() }
        else showError(when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service is offline. Type your request or try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Wait a moment and retry."
            else -> "I couldn't hear that clearly. Please retry or type your request."
        })
    }
    override fun onResults(results: Bundle?) {
        val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return showError("No speech detected.")
        input.setText(heard); viewModel.process(heard)
    }
    override fun onPartialResults(partialResults: Bundle?) { partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { status.text = it } }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onDestroy() { speech.destroy(); super.onDestroy() }
}
