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
    val customMessage: String = ""
) : Parcelable {

    enum class MessageType {
        POSITIVE,
        QUESTIONS,
        CUSTOM
    }

    /**
     * Validates that the configuration is valid
     */
    fun isValid(): Boolean {
        return viewersCount >= 10 &&
                (messageType != MessageType.CUSTOM || customMessage.isNotBlank())
    }
}