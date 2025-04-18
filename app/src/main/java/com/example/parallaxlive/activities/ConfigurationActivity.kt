package com.example.parallaxlive.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.parallaxlive.R
import com.example.parallaxlive.models.LiveConfig
import com.example.parallaxlive.utils.FirebaseAuthHelper  // Change this import

/**
 * Activity for configuring the fake live stream
 */
class ConfigurationActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var viewersSeekBar: SeekBar
    private lateinit var viewersCountTextView: TextView
    private lateinit var messageTypeRadioGroup: RadioGroup
    private lateinit var customMessageEditText: EditText
    private lateinit var startLiveButton: Button

    private lateinit var firebaseAuthHelper: FirebaseAuthHelper  // Change this variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        firebaseAuthHelper = FirebaseAuthHelper(this)  // Change this initialization

        // Initialize views
        usernameTextView = findViewById(R.id.tv_username)
        viewersSeekBar = findViewById(R.id.seekbar_viewers)
        viewersCountTextView = findViewById(R.id.tv_viewers_count)
        messageTypeRadioGroup = findViewById(R.id.radio_group_message_type)
        customMessageEditText = findViewById(R.id.et_custom_message)
        startLiveButton = findViewById(R.id.btn_start_live)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // Set username from Firebase authentication
        val username = firebaseAuthHelper.getUsername()  // This method needs to be available in FirebaseAuthHelper
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
            startLiveStream()
        }
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
            }
        )

        // Start MainActivity with configuration
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LIVE_CONFIG, liveConfig)
        }
        startActivity(intent)
    }
}