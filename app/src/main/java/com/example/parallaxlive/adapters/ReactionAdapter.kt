package com.example.parallaxlive.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*
import kotlin.random.Random
import com.example.parallaxlive.R

/**
 * Adapter for handling floating emoji reactions
 */
class ReactionAdapter(private val containerView: ViewGroup) {

    private val random = Random
    private val activeAnimations = mutableListOf<View>()

    /**
     * Adds an emoji reaction and animates it from bottom to top
     */
    fun addReaction(emoji: String) {
        // Create emoji view
        val emojiView = createEmojiView(emoji)
        containerView.addView(emojiView)

        // Position at a random X location
        positionRandomly(emojiView)

        // Start animation
        val animation = AnimationUtils.loadAnimation(containerView.context, R.anim.emoji_float_animation)

        // Keep track of active animations
        activeAnimations.add(emojiView)

        // Clean up view after animation
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                containerView.removeView(emojiView)
                activeAnimations.remove(emojiView)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Start animation
        emojiView.startAnimation(animation)
    }

    /**
     * Create a batch of the same emoji for a burst effect
     */
    fun addReactionBurst(emoji: String, count: Int = 5) {
        for (i in 0 until count) {
            // Add with a slight delay for each emoji
            containerView.postDelayed({
                addReaction(emoji)
            }, i * 100L)
        }
    }

    /**
     * Creates the emoji TextView
     */
    private fun createEmojiView(emoji: String): TextView {
        val emojiView = TextView(containerView.context)
        emojiView.text = emoji
        emojiView.textSize = 30f  // Size of emoji
        emojiView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return emojiView
    }

    /**
     * Positions the emoji at a random X position at the bottom of the screen
     */
    private fun positionRandomly(view: View) {
        view.post {
            // Get container dimensions
            val containerWidth = containerView.width

            // Random X position
            val startX = random.nextInt(containerWidth - view.width)

            // Position at bottom with random X
            val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.leftMargin = startX
            layoutParams.bottomMargin = 0
            view.layoutParams = layoutParams
        }
    }

    /**
     * Clear all currently animating emojis
     */
    fun clearAll() {
        for (view in activeAnimations) {
            view.clearAnimation()
            containerView.removeView(view)
        }
        activeAnimations.clear()
    }
}