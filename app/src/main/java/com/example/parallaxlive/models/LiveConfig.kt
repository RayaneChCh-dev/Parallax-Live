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
<<<<<<< HEAD
    val userActivityDescription: String = "", // What the user is doing (e.g., "Showing street food")
    val username: String = "Host"
=======
    val userActivityDescription: String = "", // What the user is doing (e.g., "Showing street food"
>>>>>>> e0d72cc7e0f7c5e7db4bb519ec8e4ff4971aa64e
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

/**
 * Configuration for the fake live stream
 */
@Parcelize
data class ViewerConfig(
    val username: String? = null,
    val feeling: String,
    val messageMax: Int

) : Parcelable {

    fun isValid(): Boolean {
        return feeling.isNotBlank()
    }
}
