// Create this file at: app/src/main/java/com/example/parallaxlive/adapters/ReactionAdapter.kt
package com.example.parallaxlive.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.parallaxlive.R

/**
 * Adapter for displaying floating reactions in the live stream
 */
class ReactionAdapter : RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder>() {

    // List of reaction emojis
    private val reactions = mutableListOf<ReactionItem>()

    // Available emoji reactions
    private val availableEmojis = listOf("❤️", "👍", "👏", "🔥", "😍", "😂", "🎉", "💯")

    inner class ReactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.tv_emoji)

        fun bind(reactionItem: ReactionItem) {
            // Set emoji
            emojiTextView.text = reactionItem.emoji

            // Apply animation
            applyFloatingAnimation(itemView, reactionItem)
        }

        private fun applyFloatingAnimation(view: View, reactionItem: ReactionItem) {
            // Create vertical translation animation (floating up)
            val translateY = ObjectAnimator.ofFloat(
                view,
                "translationY",
                0f,
                -500f
            )
            translateY.duration = 3000

            // Create alpha animation (fading out)
            val alpha = ObjectAnimator.ofFloat(
                view,
                "alpha",
                1f,
                0f
            )
            alpha.duration = 3000

            // Create scaling animation
            val scaleX = ObjectAnimator.ofFloat(
                view,
                "scaleX",
                1f,
                1.2f
            )
            scaleX.duration = 3000

            val scaleY = ObjectAnimator.ofFloat(
                view,
                "scaleY",
                1f,
                1.2f
            )
            scaleY.duration = 3000

            // Play animations together
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(translateY, alpha, scaleX, scaleY)

            // Remove the reaction item when animation ends
            animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    removeReaction(reactionItem)
                }
            })

            // Start animation
            animatorSet.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_floating_reaction, parent, false)
        return ReactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReactionViewHolder, position: Int) {
        holder.bind(reactions[position])
    }

    override fun getItemCount(): Int = reactions.size

    /**
     * Adds a new reaction to the list
     * @param emoji The emoji to display. If null, a random one will be chosen
     */
    fun addReaction(emoji: String? = null) {
        val reactionEmoji = emoji ?: availableEmojis.random()
        val reaction = ReactionItem(
            id = reactions.size.toString(),
            emoji = reactionEmoji
        )

        reactions.add(reaction)
        notifyItemInserted(reactions.size - 1)
    }

    /**
     * Removes a reaction from the list
     */
    private fun removeReaction(reactionItem: ReactionItem) {
        val position = reactions.indexOf(reactionItem)
        if (position != -1) {
            reactions.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Represents a floating reaction item
     */
    data class ReactionItem(
        val id: String,
        val emoji: String
    )
}