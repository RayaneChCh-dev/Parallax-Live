package com.example.parallaxlive.utils

import com.example.parallaxlive.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ClaudeRepository(private val apiService: ClaudeApiService) {

    suspend fun generateMessage(config: LiveConfig): String {
        // Create a context-rich prompt based on the configuration
        val contextPrompt = buildContextPrompt(config)

        // Create the request
        val request = ClaudeRequest(
            model = "claude-3-haiku-20240307",
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = contextPrompt
                )
            ),
            max_tokens = 100
        )

        // Make API call
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.generateMessage(request)
                if (response.isSuccessful && response.body() != null) {
                    // Extract the text from the response
                    response.body()?.content?.firstOrNull()?.text?.trim() ?: getFallbackMessage(config)
                } else {
                    // Return a fallback message if API call fails
                    getFallbackMessage(config)
                }
            } catch (e: Exception) {
                // Return a fallback message if API call throws an exception
                getFallbackMessage(config)
            }
        }
    }

    private fun buildContextPrompt(config: LiveConfig): String {
        val basePrompt = when (config.messageType) {
            LiveConfig.MessageType.POSITIVE ->
                "Generate a short, positive comment for a social media livestream."

            LiveConfig.MessageType.QUESTIONS ->
                "Generate a short question for a social media livestream."

            LiveConfig.MessageType.CUSTOM ->
                "Generate a short social media comment in this style: ${config.customMessage}."
        }

        return """
            $basePrompt
            
            Important context:
            - The livestreamer is doing: ${config.livePurpose}
            - Current location: ${config.location}
            - Current activity: ${config.userActivityDescription}
            
            Make the message sound authentic and relevant to the context. Keep it under 50 characters and include an emoji occasionally. Make sure it sounds like a real person watching the stream would write it.
            
            Only generate the message text, nothing else.
        """.trimIndent()
    }

    // Fallback messages in case API call fails
    private fun getFallbackMessage(config: LiveConfig): String {
        // Create more contextual fallback messages
        val locationBased = when {
            config.location.contains("beach", ignoreCase = true) -> "The beach looks amazing! 🌊"
            config.location.contains("restaurant", ignoreCase = true) -> "That food looks delicious! 😋"
            config.location.contains("park", ignoreCase = true) -> "The park looks so peaceful! 🌳"
            config.location.contains("home", ignoreCase = true) -> "Your place looks cozy! 🏠"
            else -> "Loving this view of ${config.location}! 📍"
        }

        val activityBased = when {
            config.userActivityDescription.contains("cook", ignoreCase = true) -> "What ingredients are you using? 👨‍🍳"
            config.userActivityDescription.contains("travel", ignoreCase = true) -> "How's the weather there? ☀️"
            config.userActivityDescription.contains("workout", ignoreCase = true) -> "What's your fitness routine? 💪"
            config.userActivityDescription.contains("show", ignoreCase = true) -> "Can you show us more? 👀"
            else -> "What made you start doing this? 🤔"
        }

        return when (config.messageType) {
            LiveConfig.MessageType.POSITIVE -> locationBased
            LiveConfig.MessageType.QUESTIONS -> activityBased
            LiveConfig.MessageType.CUSTOM -> config.customMessage.takeIf { it.isNotBlank() } ?: "Great stream today! 👍"
        }
    }
}