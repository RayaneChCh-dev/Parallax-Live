// Create this file at: app/src/main/java/com/example/parallaxlive/models/FakeMessage.kt
package com.example.parallaxlive.models

import java.util.*

/**
 * Represents a fake message in the live stream
 */
data class FakeMessage(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val message: String,
    val profilePicResId: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Messages can have reactions
    var hasHeart: Boolean = false
    var hasLike: Boolean = false
    var hasClap: Boolean = false

    /**
     * Toggle a reaction
     */
    fun toggleReaction(reactionType: ReactionType) {
        when (reactionType) {
            ReactionType.HEART -> hasHeart = !hasHeart
            ReactionType.LIKE -> hasLike = !hasLike
            ReactionType.CLAP -> hasClap = !hasClap
        }
    }

    enum class ReactionType {
        HEART,
        LIKE,
        CLAP
    }
}