package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitme.app.R
import com.fitme.app.data.model.Chat
import com.fitme.app.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val currentUserId: String,
    private var chats: List<Chat>,
    private val onChatClick: (String) -> Unit,
    private val onChatLongClick: (String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    fun updateList(newList: List<Chat>) {
        chats = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        val otherId = chat.participants.find { it != currentUserId } ?: ""
        val otherName = chat.participantNames[otherId] ?: "User"
        val otherImage = chat.participantImages[otherId] ?: ""
        
        holder.binding.tvName.text = otherName
        holder.binding.tvLastMessage.text = chat.lastMessage.ifEmpty { "Tap to chat" }
        
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.binding.tvTime.text = sdf.format(Date(chat.lastUpdated))

        val unreadCount = chat.unreadCount[currentUserId] ?: 0
        if (unreadCount > 0) {
            holder.binding.tvUnreadCount.text = unreadCount.toString()
            holder.binding.tvUnreadCount.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvUnreadCount.visibility = android.view.View.GONE
        }

        val avatarUrl = if (otherImage.isNotEmpty()) {
            otherImage
        } else {
            "https://ui-avatars.com/api/?name=${otherName.replace(" ", "+")}&background=random&color=fff&size=128"
        }

        // Load image (using chat metadata first)
        Glide.with(holder.itemView.context)
            .load(avatarUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(holder.binding.ivAvatar)

        // Try to fetch latest image from Firestore for real-time accuracy
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(otherId).get()
            .addOnSuccessListener { doc ->
                val latestImage = doc.getString("profileImageUrl") ?: ""
                val latestName = doc.getString("name") ?: otherName
                
                if (latestName != otherName) {
                    holder.binding.tvName.text = latestName
                }

                if (latestImage.isNotEmpty() && latestImage != otherImage) {
                    Glide.with(holder.itemView.context)
                        .load(latestImage)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(holder.binding.ivAvatar)
                } else if (latestImage.isEmpty()) {
                    // Use initials avatar if profile image is still empty
                    val initialsUrl = "https://ui-avatars.com/api/?name=${latestName.replace(" ", "+")}&background=random&color=fff&size=128"
                    Glide.with(holder.itemView.context)
                        .load(initialsUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(holder.binding.ivAvatar)
                }
            }
        
        holder.binding.root.setOnClickListener {
            onChatClick(otherId)
        }
        
        holder.binding.root.setOnLongClickListener {
            onChatLongClick(otherId)
            true
        }
    }

    override fun getItemCount() = chats.size

    class ViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)
}
