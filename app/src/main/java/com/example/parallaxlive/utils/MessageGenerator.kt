package com.example.parallaxlive.utils

import com.example.parallaxlive.R
import com.example.parallaxlive.models.FakeMessage
import com.example.parallaxlive.models.LiveConfig
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MessageGenerator(
    private val config: LiveConfig,
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
    private val positiveMessages = listOf(
        "Love your content! 😍",
        "You're killing it! 🔥",
        "This is amazing!",
        "Keep up the great work!",
        "Wow, so inspirational!",
        "You look gorgeous today! ✨",
        "Your energy is contagious! ⚡",
        "I'm your biggest fan!",
        "This made my day! 🙌",
        "You are so talented!",
        "Sending love from NYC! ❤️",
        "This is exactly what I needed today",
        "Can't stop watching! 👀",
        "You're my role model!",
        "Absolutely fantastic content! 👏"
    )

    private val questionMessages = listOf(
        "How do you stay motivated?",
        "What's your favorite song right now?",
        "Can you share your skincare routine?",
        "Where did you get that outfit?",
        "How often do you go live?",
        "What camera are you using?",
        "Any tips for beginners?",
        "What's your zodiac sign?",
        "Can you do a house tour next time?",
        "How long have you been doing this?",
        "What's your favorite place to travel?",
        "Do you have any pets?",
        "What are you having for dinner?",
        "Can you say hi to me? 🙏",
        "Will you be doing a collab soon?"
    )
    fun startGenerating() {
        stopGenerating()

        timer = Timer().apply {
            scheduleAtFixedRate(1000, 1500) {
                generateMessage()
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

        val message = when (config.messageType) {
            LiveConfig.MessageType.POSITIVE -> positiveMessages.random()
            LiveConfig.MessageType.QUESTIONS -> questionMessages.random()
            LiveConfig.MessageType.CUSTOM -> config.customMessage
        }

        val fakeMessage = FakeMessage(
            username = username,
            message = message,
            profilePicResId = profilePic
        )

        onMessageGenerated(fakeMessage)
    }
}