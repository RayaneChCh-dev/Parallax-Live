package com.example.parallaxlive.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parallaxlive.R
import com.example.parallaxlive.models.LiveConfig
import com.example.parallaxlive.utils.FirebaseAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity for configuring the fake live stream
 */
class ConfigurationActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var viewersSeekBar: SeekBar
    private lateinit var viewersCountTextView: TextView
    private lateinit var messageTypeRadioGroup: RadioGroup
    private lateinit var customMessageEditText: EditText
    private lateinit var livePurposeEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var activityDescriptionEditText: EditText
    private lateinit var startLiveButton: Button
    private lateinit var signOutButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseAuthHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firebaseAuthHelper = FirebaseAuthHelper(this)

        // Initialize views
        usernameTextView = findViewById(R.id.tv_username)
        viewersSeekBar = findViewById(R.id.seekbar_viewers)
        viewersCountTextView = findViewById(R.id.tv_viewers_count)
        messageTypeRadioGroup = findViewById(R.id.radio_group_message_type)
        customMessageEditText = findViewById(R.id.et_custom_message)
        livePurposeEditText = findViewById(R.id.et_live_purpose)
        locationEditText = findViewById(R.id.et_location)
        activityDescriptionEditText = findViewById(R.id.et_activity_description)
        startLiveButton = findViewById(R.id.btn_start_live)
        signOutButton = findViewById(R.id.btn_sign_out)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // Set username from Firebase authentication
        val username = firebaseAuthHelper.getUsername()
        usernameTextView.text = getString(R.string.hello_username, username)

        // Set initial viewers count
        updateViewersCountText(viewersSeekBar.progress)

        // Set initial message type visibility
        toggleCustomMessageVisibility()
    }

    private fun setupListeners() {
        // Viewers count seek bar listener
        viewersSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateViewersCountText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
        })

        // Message type radio group listener
        messageTypeRadioGroup.setOnCheckedChangeListener { _, _ ->
            toggleCustomMessageVisibility()
        }

        // Start live button listener
        startLiveButton.setOnClickListener {
            if (validateInputs()) {
                startLiveStream()
            }
        }

        // Set up sign out button listener
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun validateInputs(): Boolean {
        val livePurpose = livePurposeEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val activityDescription = activityDescriptionEditText.text.toString().trim()

        if (livePurpose.isEmpty()) {
            livePurposeEditText.error = "Please enter the purpose of your live stream"
            return false
        }

        if (location.isEmpty()) {
            locationEditText.error = "Please enter your current location"
            return false
        }

        if (activityDescription.isEmpty()) {
            activityDescriptionEditText.error = "Please describe what you'll be doing"
            return false
        }

        if (messageTypeRadioGroup.checkedRadioButtonId == R.id.radio_custom_message &&
            customMessageEditText.text.toString().trim().isEmpty()) {
            customMessageEditText.error = "Please enter a custom message style"
            return false
        }

        return true
    }

    private fun updateViewersCountText(progress: Int) {
        // Transform progress (0-100) to viewers count (10-1000)
        val viewersCount = 10 + (progress * 9.9).toInt()
        viewersCountTextView.text = viewersCount.toString()
    }

    private fun toggleCustomMessageVisibility() {
        val isCustomMessage = messageTypeRadioGroup.checkedRadioButtonId == R.id.radio_custom_message
        customMessageEditText.isEnabled = isCustomMessage
        customMessageEditText.alpha = if (isCustomMessage) 1.0f else 0.5f
    }

    private fun startLiveStream() {
        // Create live configuration
        val liveConfig = LiveConfig(
            viewersCount = 10 + (viewersSeekBar.progress * 9.9).toInt(),
            messageType = when (messageTypeRadioGroup.checkedRadioButtonId) {
                R.id.radio_positive_messages -> LiveConfig.MessageType.POSITIVE
                R.id.radio_questions -> LiveConfig.MessageType.QUESTIONS
                R.id.radio_custom_message -> LiveConfig.MessageType.CUSTOM
                else -> LiveConfig.MessageType.POSITIVE
            },
            customMessage = if (messageTypeRadioGroup.checkedRadioButtonId == R.id.radio_custom_message) {
                customMessageEditText.text.toString()
            } else {
                ""
            },
            livePurpose = livePurposeEditText.text.toString().trim(),
            location = locationEditText.text.toString().trim(),
            userActivityDescription = activityDescriptionEditText.text.toString().trim()
        )

        // Start MainActivity with configuration
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LIVE_CONFIG, liveConfig)
        }
        startActivity(intent)
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Return to welcome screen
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}