package com.example.parallaxlive.activities

import android.Manifest
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.example.parallaxlive.R
import com.example.parallaxlive.adapters.MessageAdapter
import com.example.parallaxlive.adapters.ReactionAdapter
import com.example.parallaxlive.models.ClaudeApiService
import com.example.parallaxlive.models.FakeMessage
import com.example.parallaxlive.models.LiveConfig
import com.example.parallaxlive.utils.CameraHelper
import com.example.parallaxlive.utils.ClaudeRepository
import com.example.parallaxlive.utils.MessageGenerator
import com.example.parallaxlive.utils.FirebaseAuthHelper
import com.example.parallaxlive.utils.DatabaseHelper
import com.example.parallaxlive.utils.RetrofitClient
import com.example.parallaxlive.utils.ViewerCountManager
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        const val EXTRA_LIVE_CONFIG = "extra_live_config"
    }

    // UI components
    private lateinit var cameraTextureView: TextureView
    private lateinit var endLiveButton: ImageButton
    private lateinit var flipCameraButton: ImageButton
    private lateinit var viewersCountTextView: TextView
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomSheet: View

    // Adapters
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var reactionAdapter: ReactionAdapter

    // Helpers
    private lateinit var viewerCountManager: ViewerCountManager
    private lateinit var cameraHelper: CameraHelper
    private lateinit var messageGenerator: MessageGenerator
    private lateinit var firebaseAuthHelper: FirebaseAuthHelper
    private lateinit var databaseHelper: DatabaseHelper

    // State
    private lateinit var liveConfig: LiveConfig
    private var currentViewersCount = 0
    private var isLiveActive = false
    private var viewersTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase helpers
        firebaseAuthHelper = FirebaseAuthHelper(this)
        databaseHelper = DatabaseHelper()

        // Get the enhanced LiveConfig from the intent
        liveConfig = intent.getParcelableExtra(EXTRA_LIVE_CONFIG) ?: LiveConfig(
            viewersCount = 100,
            messageType = LiveConfig.MessageType.POSITIVE,
            livePurpose = "General livestream",
            location = "Unknown location",
            userActivityDescription = "Casual streaming"
        )

        // Initialize UI components
        initUI()

        // Check camera permissions
        if (allPermissionsGranted()) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Start the fake live stream
        startLiveStream()
    }

    private fun initUI() {
        // Find views
        cameraTextureView = findViewById(R.id.texture_view_camera)
        endLiveButton = findViewById(R.id.btn_end_live)
        flipCameraButton = findViewById(R.id.btn_flip_camera)
        viewersCountTextView = findViewById(R.id.tv_viewers_count)
        messageRecyclerView = findViewById(R.id.recycler_messages)
        bottomSheet = findViewById(R.id.bottom_sheet)

        // Setup bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Setup message recycler view
        messageAdapter = MessageAdapter { message, reactionType ->
            handleMessageReaction(message, reactionType)
        }
        messageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                // Show newest messages at the top
                stackFromEnd = true
                reverseLayout = true
            }
            adapter = messageAdapter
        }

        // Setup reaction adapter (for floating emojis)
        reactionAdapter = ReactionAdapter()

        // Setup button listeners
        endLiveButton.setOnClickListener {
            endLiveStream()
        }

        flipCameraButton.setOnClickListener {
            if (::cameraHelper.isInitialized) {
                cameraHelper.switchCamera()
            }
        }

        // Setup emoji reaction buttons
        setupEmojiButtons()
    }

    private fun setupEmojiButtons() {
        // Find all emoji buttons in the grid
        val emojiButtons = listOf(
            R.id.btn_emoji_1, R.id.btn_emoji_2, R.id.btn_emoji_3, R.id.btn_emoji_4,
            R.id.btn_emoji_5, R.id.btn_emoji_6, R.id.btn_emoji_7, R.id.btn_emoji_8
        )

        // Set click listeners for all emoji buttons
        emojiButtons.forEach { buttonId ->
            findViewById<View>(buttonId).setOnClickListener {
                // Display floating emoji reaction
                val emoji = (it as android.widget.Button).text.toString()
                reactionAdapter.addReaction(emoji)

                // Show a toast for demonstration
                Toast.makeText(this, "Sent $emoji", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCamera() {
        // Initialize camera helper
        cameraHelper = CameraHelper(this, this, cameraTextureView)
        cameraHelper.startCamera()
    }

    private fun startLiveStream() {
        isLiveActive = true

        // Create API service and repository
        val retrofit = RetrofitClient.getInstance()
        val claudeApiService = retrofit.create(ClaudeApiService::class.java)
        val claudeRepository = ClaudeRepository(claudeApiService)

        // Initialize message generator with Claude repository
        messageGenerator = MessageGenerator(liveConfig, claudeRepository) { message ->
            runOnUiThread {
                messageAdapter.addMessage(message)
            }
        }
        messageGenerator.startGenerating()

        // Initialize and start viewer count manager
        viewerCountManager = ViewerCountManager(liveConfig) { count ->
            runOnUiThread {
                viewersCountTextView.text = count.toString()
            }
        }
        viewerCountManager.start()

        // Update live stream title/topic based on purpose
        updateLiveTitle()
    }

    private fun endLiveStream() {
        isLiveActive = false

        // Stop message generator
        if (::messageGenerator.isInitialized) {
            messageGenerator.stopGenerating()
        }

        // Stop viewer count manager
        if (::viewerCountManager.isInitialized) {
            viewerCountManager.stop()
        }

        // Return to configuration screen
        finish()
    }

    private fun updateLiveTitle() {
        // Find the live title TextView (you'll need to add this to your layout)
        val liveTitleTextView = findViewById<TextView>(R.id.tv_live_title)

        // Set title based on purpose and location
        val liveTitle = when {
            liveConfig.livePurpose.isNotBlank() && liveConfig.location.isNotBlank() ->
                "${liveConfig.livePurpose} from ${liveConfig.location}"
            liveConfig.livePurpose.isNotBlank() ->
                liveConfig.livePurpose
            liveConfig.location.isNotBlank() ->
                "Live from ${liveConfig.location}"
            else ->
                "Live Stream"
        }

        liveTitleTextView.text = liveTitle
    }

    private fun startViewersCounter() {
        viewersTimer?.cancel()

        // Start with a small number of viewers
        currentViewersCount = 10
        updateViewersCount()

        // Gradually increase viewers up to the configured count
        viewersTimer = Timer().apply {
            scheduleAtFixedRate(0, 2000) {
                if (currentViewersCount < liveConfig.viewersCount) {
                    // Calculate new viewers (gradually increase faster)
                    val increment = (liveConfig.viewersCount - currentViewersCount) / 10
                    currentViewersCount += if (increment > 0) increment else 1

                    // Cap at configured count
                    if (currentViewersCount > liveConfig.viewersCount) {
                        currentViewersCount = liveConfig.viewersCount
                    }

                    // Update UI
                    runOnUiThread {
                        updateViewersCount()
                    }
                } else {
                    // Once we reach the target, just add small fluctuations
                    val fluctuation = Random().nextInt(5) - 2
                    currentViewersCount += fluctuation

                    // Keep within bounds
                    if (currentViewersCount > liveConfig.viewersCount + 10) {
                        currentViewersCount = liveConfig.viewersCount + 10
                    } else if (currentViewersCount < liveConfig.viewersCount - 10) {
                        currentViewersCount = liveConfig.viewersCount - 10
                    }

                    // Update UI
                    runOnUiThread {
                        updateViewersCount()
                    }
                }
            }
        }
    }

    private fun updateViewersCount() {
        viewersCountTextView.text = currentViewersCount.toString()
    }

    private fun handleMessageReaction(message: FakeMessage, reactionType: FakeMessage.ReactionType) {
        message.toggleReaction(reactionType)

        // In a real app, you might want to broadcast the reaction to other viewers
        // Here we just show a toast for demonstration
        Toast.makeText(
            this,
            when (reactionType) {
                FakeMessage.ReactionType.HEART -> "❤️ Sent heart"
                FakeMessage.ReactionType.LIKE -> "👍 Sent like"
                FakeMessage.ReactionType.CLAP -> "👏 Sent clap"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun allPermissionsGranted() = CameraHelper.allPermissionsGranted(this)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permissions are required for the live stream",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        if (::cameraHelper.isInitialized) {
            cameraHelper.shutdown()
        }

        if (::messageGenerator.isInitialized) {
            messageGenerator.stopGenerating()
        }

        if (::viewerCountManager.isInitialized) {
            viewerCountManager.stop()
        }
    }

    /**
     * Helper method to check and request all required permissions
     */
    private fun checkAndRequestPermissions() {
        // Check camera permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            setupCamera()
        }

        // For Instagram Graph API, permissions are handled through the Facebook SDK
        // We don't need to request them here as they were handled during login
    }
}