package com.example.parallaxlive.utils

import com.example.parallaxlive.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.mutableListOf
import retrofit2.Response

class ClaudeRepository(private val apiService: ClaudeApiService) {

    suspend fun generateMessage(live_config: LiveConfig, viewer_config: ViewerConfig, messagesHistory: MutableList<String>): String {

        // Create a context-rich prompt based on the configuration
        val contextPrompt = buildContextPrompt(live_config, viewer_config, messagesHistory)

        // Create the request
        val request = ClaudeRequest(
            model = "claude-3-haiku-20240307",
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = contextPrompt
                )
            ),
            max_tokens = 100,
            temperature = 0.9
        )

        // Make API call
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.generateMessage(request)

                if (response.isSuccessful && response.body() != null) {
                    // Extract the text from the response
                    val responseBody = response.body()
                    val textContent = responseBody?.content?.firstOrNull { it.type == "text" }?.text
                    val trimmedContent = textContent?.trim()
                    val cleanedContent = trimmedContent?.let { removeSurroundingQuotes(it) }
                    return@withContext cleanedContent ?: getFallbackMessage(live_config)
                } else {
                    println("Erreur API: ${response.code()} - ${response.message()}")
                    // Return a fallback message if API call fails
                    getFallbackMessage(live_config)
                }
            } catch (e: Exception) {
                // Return a fallback message if API call throws an exception
                println("Erreur lors de la requête API : ${e.message}")
                e.printStackTrace()
                getFallbackMessage(live_config)
            }
        }
    }

    private fun removeSurroundingQuotes(input: String): String {
        // Vérifiez si la chaîne commence et se termine par un guillemet
        if (input.startsWith('"') && input.endsWith('"')) {
            // Retirez le premier et le dernier caractère
            return input.substring(1, input.length - 1)
        }
        return input
    }

    private fun buildContextPrompt(config: LiveConfig, viewer_config: ViewerConfig, messagesHistory: MutableList<String>): String {
        val instructPrompt = when (config.messageType) {
            LiveConfig.MessageType.POSITIVE ->
                "Generate a short, positive comment for a social media livestream."

            LiveConfig.MessageType.QUESTIONS ->
                "Generate a short question for a social media livestream."

            LiveConfig.MessageType.CUSTOM ->
                "Generate a short social media comment in this style: ${config.customMessage}."
        }

        return """
            ## Context:
            You're a viewer with feeling "${viewer_config.feeling}"
            Your goal is to send a message to the livestreamer and see its reaction or reactions of other viewers.
            You base your message on the chat history.
            
            ## Stream Context:
            - The livestreamer name: @${config.username}
            - The livestreamer is doing: ${config.livePurpose}
            - Current location: ${config.location}
            - Current activity: ${config.userActivityDescription}
            - Current viewers count on the live: ${config.viewersCount}

            ## Stream Messages History Context:
            - ${ if (messagesHistory.isEmpty()) { "Vous êtes le premier commentaire" } else { messagesHistory.joinToString(separator = "\n - ") } }\n
            
            ## Instructions
            $instructPrompt
            Make the message sound authentic and relevant to the context. Keep it under ${viewer_config.messageMax.toString()} characters. Make sure it sounds like human chatting on a livestream.
            Your message can provide tags or emojis.
            
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