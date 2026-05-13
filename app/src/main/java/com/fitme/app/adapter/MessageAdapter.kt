package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitme.app.R
import com.fitme.app.data.model.Message
import com.fitme.app.databinding.ItemMessageBinding

class MessageAdapter(
    private val currentUserId: String,
    var receivedUserImageUrl: String = "",
    var receivedUserName: String = "User"
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var playingMessageId: String? = null
    private var handler = android.os.Handler(android.os.Looper.getMainLooper())

    fun updateReceivedUser(url: String, name: String) {
        receivedUserImageUrl = url
        receivedUserName = name
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var progressUpdater: Runnable? = null

        fun bind(message: Message) {
            val isSent = message.senderId == currentUserId

            if (isSent) {
                binding.layoutSent.visibility = View.VISIBLE
                binding.layoutReceived.visibility = View.GONE
                
                binding.ivSentImage.visibility = if (message.type == "image") View.VISIBLE else View.GONE
                binding.tvSentMessage.visibility = if (message.type == "text") View.VISIBLE else View.GONE
                binding.layoutSentVoice.visibility = if (message.type == "voice") View.VISIBLE else View.GONE

                when (message.type) {
                    "image" -> Glide.with(binding.root.context).load(message.imageUrl).into(binding.ivSentImage)
                    "text" -> binding.tvSentMessage.text = message.text
                    "voice" -> setupVoicePlayer(message, binding.btnPlaySent, binding.seekbarSent, binding.tvSentVoiceDuration)
                }
                
                val tickColor = if (message.read) "#3B82F6" else "#94A3B8"
                binding.tvTicks.setTextColor(android.graphics.Color.parseColor(tickColor))
            } else {
                binding.layoutReceived.visibility = View.VISIBLE
                binding.layoutSent.visibility = View.GONE
                
                binding.ivReceivedImage.visibility = if (message.type == "image") View.VISIBLE else View.GONE
                binding.tvReceivedMessage.visibility = if (message.type == "text") View.VISIBLE else View.GONE
                binding.layoutReceivedVoice.visibility = if (message.type == "voice") View.VISIBLE else View.GONE

                when (message.type) {
                    "image" -> Glide.with(binding.root.context).load(message.imageUrl).into(binding.ivReceivedImage)
                    "text" -> binding.tvReceivedMessage.text = message.text
                    "voice" -> setupVoicePlayer(message, binding.btnPlayReceived, binding.seekbarReceived, binding.tvReceivedVoiceDuration)
                }
            }
        }

        private fun setupVoicePlayer(message: Message, playBtn: android.widget.ImageButton, seekBar: android.widget.SeekBar, durationText: android.widget.TextView) {
            durationText.text = formatDuration(message.duration)
            seekBar.max = message.duration * 1000
            
            if (playingMessageId == message.messageId) {
                playBtn.setImageResource(android.R.drawable.ic_media_pause)
                startProgressUpdate(seekBar, durationText)
            } else {
                playBtn.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
            }

            playBtn.setOnClickListener {
                if (playingMessageId == message.messageId) {
                    stopPlayback()
                } else {
                    startPlayback(message)
                }
            }
        }

        private fun startPlayback(message: Message) {
            stopPlayback()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(message.audioUrl)
                prepareAsync()
                setOnPreparedListener { 
                    start()
                    playingMessageId = message.messageId
                    notifyDataSetChanged()
                }
                setOnCompletionListener { 
                    stopPlayback()
                    notifyDataSetChanged()
                }
            }
        }

        private fun stopPlayback() {
            mediaPlayer?.release()
            mediaPlayer = null
            playingMessageId = null
            handler.removeCallbacksAndMessages(null)
            notifyDataSetChanged()
        }

        private fun startProgressUpdate(seekBar: android.widget.SeekBar, durationText: android.widget.TextView) {
            progressUpdater = object : Runnable {
                override fun run() {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            seekBar.progress = it.currentPosition
                            durationText.text = formatDuration(it.currentPosition / 1000)
                            handler.postDelayed(this, 100)
                        }
                    }
                }
            }
            handler.post(progressUpdater!!)
        }

        private fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format("%d:%02d", mins, secs)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.messageId == newItem.messageId
    override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
}
