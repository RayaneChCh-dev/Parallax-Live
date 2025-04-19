package com.example.parallaxlive.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.parallaxlive.R
import com.example.parallaxlive.models.FakeMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying fake messages in the live stream
 */
class MessageAdapter(
    private val messages: MutableList<FakeMessage> = mutableListOf(),
    private val onReactionClicked: (FakeMessage, FakeMessage.ReactionType) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: ImageView = itemView.findViewById(R.id.iv_profile)
        private val usernameTextView: TextView = itemView.findViewById(R.id.tv_username)
        private val messageTextView: TextView = itemView.findViewById(R.id.tv_message)
        private val timeTextView: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val heartIcon: ImageView = itemView.findViewById(R.id.iv_heart)
        private val likeIcon: ImageView = itemView.findViewById(R.id.iv_like)
        private val clapIcon: ImageView = itemView.findViewById(R.id.iv_clap)

        fun bind(message: FakeMessage) {
            // Set user profile and info
            profileImageView.setImageResource(message.profilePicResId)
            usernameTextView.text = message.username
            messageTextView.text = message.message

            // Set timestamp
            timeTextView.text = timeFormat.format(Date(message.timestamp))

            // Set reaction states
            updateReactionIcons(message)

            // Set reaction click listeners
            heartIcon.setOnClickListener {
                onReactionClicked(message, FakeMessage.ReactionType.HEART)
                updateReactionIcons(message)
            }

            likeIcon.setOnClickListener {
                onReactionClicked(message, FakeMessage.ReactionType.LIKE)
                updateReactionIcons(message)
            }

            clapIcon.setOnClickListener {
                onReactionClicked(message, FakeMessage.ReactionType.CLAP)
                updateReactionIcons(message)
            }
        }

        private fun updateReactionIcons(message: FakeMessage) {
            heartIcon.setImageResource(
                if (message.hasHeart) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )

            likeIcon.setImageResource(
                if (message.hasLike) R.drawable.ic_like_filled
                else R.drawable.ic_like_outline
            )

            clapIcon.setImageResource(
                if (message.hasClap) R.drawable.ic_clap_filled
                else R.drawable.ic_clap_outline
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    /**
     * Adds a new message to the list and notifies the adapter
     */
    fun addMessage(message: FakeMessage) {
        // Add at position 0 to show newest messages at top
        messages.add(0, message)
        notifyItemInserted(0)

        // Keep only the last 20 messages
        if (messages.size > 20) {
            messages.removeAt(messages.size - 1)
            notifyItemRemoved(messages.size)
        }
    }
}