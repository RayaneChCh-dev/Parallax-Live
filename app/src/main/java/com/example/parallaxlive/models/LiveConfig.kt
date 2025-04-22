package com.example.parallaxlive.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for the fake live stream
 */
@Parcelize
data class LiveConfig(
    val viewersCount: Int,
    val messageType: MessageType,
    val customMessage: String = "",
    val livePurpose: String = "", // Purpose of the live stream (e.g., "Cooking show", "Travel vlog")
    val location: String = "",    // User's current location (e.g., "Paris, France")
    val userActivityDescription: String = "", // What the user is doing (e.g., "Showing street food")
    val username: String = "Host" // 👈 Add this line
) : Parcelable {
    enum class MessageType {
        POSITIVE,
        QUESTIONS,
        CUSTOM
    }

    fun isValid(): Boolean {
        return viewersCount >= 10 &&
                (messageType != MessageType.CUSTOM || customMessage.isNotBlank()) &&
                livePurpose.isNotBlank() &&
                location.isNotBlank()
    }
}
