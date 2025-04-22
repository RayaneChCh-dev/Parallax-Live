package com.example.parallaxlive.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.parallaxlive.R
import com.example.parallaxlive.models.LiveConfig
import com.example.parallaxlive.utils.FirebaseAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth

class ConfigurationActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var viewersSlider: Slider
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

        auth = FirebaseAuth.getInstance()
        firebaseAuthHelper = FirebaseAuthHelper(this)

        initViews()
        setupViews()
        setupListeners()
    }

    private fun initViews() {
        usernameTextView = findViewById(R.id.tv_username)
        viewersSlider = findViewById(R.id.slider_viewers)
        viewersCountTextView = findViewById(R.id.tv_viewers_count)
        messageTypeRadioGroup = findViewById(R.id.radio_group_message_type)
        customMessageEditText = findViewById(R.id.et_custom_message)
        livePurposeEditText = findViewById(R.id.et_live_purpose)
        locationEditText = findViewById(R.id.et_location)
        activityDescriptionEditText = findViewById(R.id.et_activity_description)
        startLiveButton = findViewById(R.id.btn_start_live)
        signOutButton = findViewById(R.id.btn_sign_out)
    }

    private fun setupViews() {
        val username = firebaseAuthHelper.getUsername()
        usernameTextView.text = getString(R.string.hello_username, username)

        updateViewersCountText(viewersSlider.value.toInt())
        toggleCustomMessageVisibility()
    }

    private fun setupListeners() {
        viewersSlider.addOnChangeListener { _, value, _ ->
            updateViewersCountText(value.toInt())
        }

        messageTypeRadioGroup.setOnCheckedChangeListener { _, _ ->
            toggleCustomMessageVisibility()
        }

        startLiveButton.setOnClickListener {
            if (validateInputs()) {
                startLiveStream()
            }
        }

        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun updateViewersCountText(progress: Int) {
        val viewersCount = 10 + (progress * 9.9).toInt()
        viewersCountTextView.text = viewersCount.toString()
    }

    private fun toggleCustomMessageVisibility() {
        val isCustom = messageTypeRadioGroup.checkedRadioButtonId == R.id.radio_custom_message
        customMessageEditText.isEnabled = isCustom
        customMessageEditText.alpha = if (isCustom) 1.0f else 0.5f
    }

    private fun validateInputs(): Boolean {
        val livePurpose = livePurposeEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val activityDescription = activityDescriptionEditText.text.toString().trim()

        return when {
            livePurpose.isEmpty() -> {
                livePurposeEditText.error = "Please enter the purpose of your live stream"
                false
            }
            location.isEmpty() -> {
                locationEditText.error = "Please enter your current location"
                false
            }
            activityDescription.isEmpty() -> {
                activityDescriptionEditText.error = "Please describe what you'll be doing"
                false
            }
            messageTypeRadioGroup.checkedRadioButtonId == R.id.radio_custom_message &&
                    customMessageEditText.text.toString().trim().isEmpty() -> {
                customMessageEditText.error = "Please enter a custom message style"
                false
            }
            else -> true
        }
    }

    private fun startLiveStream() {
        val viewersCount = 10 + (viewersSlider.value.toInt() * 9.9).toInt()

        val messageType = when (messageTypeRadioGroup.checkedRadioButtonId) {
            R.id.radio_positive_messages -> LiveConfig.MessageType.POSITIVE
            R.id.radio_questions -> LiveConfig.MessageType.QUESTIONS
            R.id.radio_custom_message -> LiveConfig.MessageType.CUSTOM
            else -> LiveConfig.MessageType.POSITIVE
        }

        val config = LiveConfig(
            viewersCount = viewersCount,
            messageType = messageType,
            customMessage = if (messageType == LiveConfig.MessageType.CUSTOM) {
                customMessageEditText.text.toString().trim()
            } else "",
            livePurpose = livePurposeEditText.text.toString().trim(),
            location = locationEditText.text.toString().trim(),
            userActivityDescription = activityDescriptionEditText.text.toString().trim()
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LIVE_CONFIG, config)
        }
        startActivity(intent)
    }

    private fun signOut() {
        auth.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
