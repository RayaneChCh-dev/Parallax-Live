package com.example.parallaxlive.utils

import com.example.parallaxlive.R
import com.example.parallaxlive.models.FakeMessage
import com.example.parallaxlive.models.LiveConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MessageGenerator(
    private val config: LiveConfig,
    private val claudeRepository: ClaudeRepository,
    private val onMessageGenerated: (FakeMessage) -> Unit
) {
    private val random = Random()
    private var timer: Timer? = null
    private val profilePics = listOf(
        R.drawable.profile_pic_1,
        R.drawable.profile_pic_2,
        R.drawable.profile_pic_3,
        R.drawable.profile_pic_4,
        R.drawable.profile_pic_5,
        R.drawable.profile_pic_6
    )

    private val usernames = listOf(
        "emma_smith", "john_doe", "sarah_j", "alex_cool",
        "fitness_freak", "travel_lover", "photo_ninja", "food_addict",
        "music_fan", "art_enthusiast", "tech_geek", "fashionista",
        "nature_explorer", "book_worm", "movie_buff", "coffee_lover"
    )

    // Keep a cache of generated messages to avoid excessive API calls
    private val messageCache = mutableListOf<String>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Pre-populate the cache for immediate use
    init {
        prePopulateCache()
    }

    private fun prePopulateCache() {
        // Create context-relevant initial messages
        val defaultMessages = generateContextualDefaultMessages()
        messageCache.addAll(defaultMessages)

        // Start generating more messages in the background
        refillCache()
    }

    private fun generateContextualDefaultMessages(): List<String> {
        // Generate default messages based on context
        val purposeKeywords = config.livePurpose.toLowerCase().split(" ")
        val locationKeywords = config.location.toLowerCase().split(" ")
        val activityKeywords = config.userActivityDescription.toLowerCase().split(" ")

        val contextualMessages = mutableListOf<String>()

        // Add location-based messages
        when {
            locationKeywords.any { it in listOf("beach", "sea", "ocean") } -> {
                contextualMessages.add("The beach looks gorgeous! 🏖️")
                contextualMessages.add("Is the water warm? 🌊")
            }
            locationKeywords.any { it in listOf("restaurant", "cafe", "diner") } -> {
                contextualMessages.add("That food looks delicious! 😋")
                contextualMessages.add("What are you ordering? 🍽️")
            }
            locationKeywords.any { it in listOf("park", "forest", "mountain") } -> {
                contextualMessages.add("The view is breathtaking! 🌳")
                contextualMessages.add("How's the weather there? ☀️")
            }
            locationKeywords.any { it in listOf("city", "downtown", "town") } -> {
                contextualMessages.add("I love that part of town! 🏙️")
                contextualMessages.add("Show us more of the city! 🚶‍♂️")
            }
        }

        // Add activity-based messages
        when {
            activityKeywords.any { it in listOf("cooking", "baking", "chef") } -> {
                contextualMessages.add("What recipe are you making? 👨‍🍳")
                contextualMessages.add("Looks so tasty! 🍳")
            }
            activityKeywords.any { it in listOf("traveling", "exploring", "tour") } -> {
                contextualMessages.add("What's your favorite spot so far? 🧳")
                contextualMessages.add("Take us on a tour! 🗺️")
            }
            activityKeywords.any { it in listOf("workout", "exercise", "fitness") } -> {
                contextualMessages.add("What's your fitness routine? 💪")
                contextualMessages.add("You're motivating me to workout! 🏋️‍♂️")
            }
            activityKeywords.any { it in listOf("shopping", "store", "mall") } -> {
                contextualMessages.add("What are you shopping for? 🛍️")
                contextualMessages.add("I love that store! 🛒")
            }
        }

        // Add purpose-based messages
        when {
            purposeKeywords.any { it in listOf("tutorial", "howto", "guide") } -> {
                contextualMessages.add("This is so helpful! 📝")
                contextualMessages.add("Can you explain that again? 🤔")
            }
            purposeKeywords.any { it in listOf("vlog", "daily", "routine") } -> {
                contextualMessages.add("I love your vlogs! 📹")
                contextualMessages.add("Do you post daily? 📅")
            }
            purposeKeywords.any { it in listOf("music", "singing", "concert") } -> {
                contextualMessages.add("Your voice is amazing! 🎤")
                contextualMessages.add("Can you sing my favorite song? 🎵")
            }
            purposeKeywords.any { it in listOf("game", "gaming", "play") } -> {
                contextualMessages.add("Nice move! 🎮")
                contextualMessages.add("What game is this? 🕹️")
            }
        }

        // If we couldn't generate context-specific messages, add some generic ones
        if (contextualMessages.isEmpty()) {
            contextualMessages.add("This is so cool! 👏")
            contextualMessages.add("How long have you been doing this? 🤔")
            contextualMessages.add("Love the content! ❤️")
            contextualMessages.add("Greetings from NYC! 👋")
        }

        return contextualMessages
    }

    private fun refillCache() {
        // Don't generate too many at once
        if (messageCache.size >= 10) return

        coroutineScope.launch {
            repeat(5) {
                try {
                    val message = claudeRepository.generateMessage(config)
                    messageCache.add(message)
                } catch (e: Exception) {
                    // Silently fail, we have fallbacks
                }
            }
        }
    }

    fun startGenerating() {
        stopGenerating()

        timer = Timer().apply {
            scheduleAtFixedRate(1000, 1500) {
                generateMessage()

                // Make sure we always have messages ready
                if (messageCache.size < 5) {
                    refillCache()
                }
            }
        }
    }

    fun stopGenerating() {
        timer?.cancel()
        timer = null
    }

    private fun generateMessage() {
        val username = usernames.random()
        val profilePic = profilePics.random()

        // Get a message from the cache or use a fallback
        val message = if (messageCache.isNotEmpty()) {
            messageCache.removeAt(0)
        } else {
            val fallbacks = generateContextualDefaultMessages()
            fallbacks.random()
        }

        val fakeMessage = FakeMessage(
            username = username,
            message = message,
            profilePicResId = profilePic
        )

        onMessageGenerated(fakeMessage)
    }
}