package com.example.parallaxlive.activities

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import com.example.parallaxlive.models.User
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        const val EXTRA_LIVE_CONFIG = "extra_live_config"
    }

    // Google User

    private lateinit var usernameTextView: TextView
    private lateinit var userProfileImageView: ImageView

    // UI components

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var endLiveButton: ImageButton
    private lateinit var flipCameraButton: ImageButton
    private lateinit var viewersCountTextView: TextView
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomSheet: View

    // Adapters
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var reactionAdapter: ReactionAdapter
    private lateinit var emojiContainer: ConstraintLayout
    private lateinit var commentEditText: EditText
    private lateinit var sendCommentButton: ImageButton

    // Helpers
    private lateinit var viewerCountManager: ViewerCountManager
    private lateinit var cameraHelper: CameraHelper
    private lateinit var messageGenerator: MessageGenerator
    private lateinit var firebaseAuthHelper: FirebaseAuthHelper
    private lateinit var databaseHelper: DatabaseHelper

    // State
    private lateinit var liveConfig: LiveConfig
    private lateinit var user: User
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

        initializeUser()

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

    private fun initializeUser() {
        // Option 1: Get user from FirebaseAuth
        val currentUser = firebaseAuthHelper.getCurrentUser()
        if (currentUser != null) {
            user = User(
                id = currentUser.id,
                username = currentUser.username,
                fullName = currentUser.fullName,
                profilePicUrl = currentUser.profilePicUrl.toString(),
                accessToken = "" // You might need to get this from somewhere else
            )
        } else {
            // Option 2: Use a mock user for testing
            user = User.createMockUser()
        }

        // Optional: Load user profile image
        if (user.profilePicUrl.isNotEmpty()) {
            // Use an image loading library like Glide or Picasso to load the image
            // For example with Glide:
            // Glide.with(this).load(user.profilePicUrl).into(userProfileImageView)
        }
    }

    private fun initUI() {

        // Find views
        usernameTextView = findViewById(R.id.tv_username)
        userProfileImageView = findViewById(R.id.img_user_profile)
        cameraPreviewView = findViewById(R.id.preview_view_camera)
        endLiveButton = findViewById(R.id.btn_end_live)
        flipCameraButton = findViewById(R.id.btn_flip_camera)
        viewersCountTextView = findViewById(R.id.tv_viewers_count)
        messageRecyclerView = findViewById(R.id.recycler_messages)
        bottomSheet = findViewById(R.id.bottom_sheet)

        usernameTextView.text = user.username

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

        if (user.profilePicUrl.isNotEmpty()) {
            // Example with Glide
            Glide.with(this)
                .load(user.profilePicUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(userProfileImageView)
        }
        val layoutManager = messageRecyclerView.layoutManager as LinearLayoutManager
        messageRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                messageRecyclerView.post {
                    if (layoutManager.findFirstVisibleItemPosition() < 3) {
                        scrollToLatestMessage()
                    }
                }
            }
        }

        // Setup reaction adapter (for floating emojis)
        emojiContainer = findViewById(R.id.emoji_container)
        commentEditText = findViewById(R.id.et_comment)
        sendCommentButton = findViewById(R.id.btn_send_comment)

        reactionAdapter = ReactionAdapter(findViewById(R.id.main_container))

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

    private fun setupBottomSheet() {
        // Find bottom sheet components
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Set initial state
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Set callback
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // No specific actions needed on state change
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // No specific actions needed on slide
            }
        })

        // Set up comment sending
        setupCommentFunctionality()
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
                // Get the emoji from the button
                val emoji = (it as android.widget.Button).text.toString()

                // Create burst effect with multiple emojis
                reactionAdapter.addReactionBurst(emoji, 5)

                // Optionally collapse the bottom sheet after selecting an emoji
                // bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // Show a toast for demonstration
                Toast.makeText(this, "Sent $emoji", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCommentFunctionality() {
        // Set up send button click listener
        sendCommentButton.setOnClickListener {
            sendComment()
        }

        // Set up EditText action listener for when user presses "Send" on keyboard
        commentEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun sendComment() {
        val commentText = commentEditText.text.toString().trim()
        if (commentText.isNotEmpty()) {
            // Create a message from the current user
            val userComment = FakeMessage(
                username = user.username,
                message = commentText,
                profilePicResId = R.drawable.ic_profile_placeholder  // Or use a proper profile pic
            )

            // Add message to the list
            messageAdapter.addMessage(userComment)

            // Clear input field
            commentEditText.setText("")

            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(commentEditText.windowToken, 0)

            // Optionally collapse bottom sheet after sending
            // bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setupCamera() {
        // Initialize camera helper
        cameraHelper = CameraHelper(this, this, cameraPreviewView)
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
                scrollToLatestMessage()
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

    private fun scrollToLatestMessage() {
        if (messageAdapter.itemCount > 0) {
            // Comme vous utilisez reverseLayout=true, la position 0 est le message le plus récent
            messageRecyclerView.smoothScrollToPosition(0)
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
    }
}