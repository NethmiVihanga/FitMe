package com.fitme.app.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fitme.app.R
import com.fitme.app.adapter.MessageAdapter
import com.fitme.app.databinding.FragmentChatBinding
import com.fitme.app.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.fitme.app.data.model.Chat

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private var chatListAdapter: com.fitme.app.adapter.ChatListAdapter? = null
    private var allRecentChats: List<Chat> = emptyList()
    private var tailorId: String = ""
    private var currentUserId: String = ""

    private var mediaRecorder: android.media.MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var recordingStartTime: Long = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Toast.makeText(requireContext(), "Sending image...", Toast.LENGTH_SHORT).show()
            chatViewModel.sendImageMessage(currentUserId, tailorId, it)
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            Toast.makeText(requireContext(), "Sending image...", Toast.LENGTH_SHORT).show()
            val uri = saveBitmapToTempFile(it)
            uri?.let { tempUri ->
                chatViewModel.sendImageMessage(currentUserId, tailorId, tempUri)
            }
        }
    }

    private fun saveBitmapToTempFile(bitmap: android.graphics.Bitmap): android.net.Uri? {
        return try {
            val file = java.io.File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val out = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        tailorId = arguments?.getString("tailorId") ?: ""

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnAttach.setOnClickListener { pickImage.launch("image/*") }
        binding.btnCamera.setOnClickListener { takePhoto.launch(null) }

        setupSearch()
        setupVoiceRecording()

        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.btnSend.visibility = View.GONE
                    binding.btnVoice.visibility = View.VISIBLE
                } else {
                    binding.btnSend.visibility = View.VISIBLE
                    binding.btnVoice.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        chatViewModel.sendResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                // Done
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to send: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Load tailor info for header
        if (tailorId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users").document(tailorId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "User"
                    val imgUrl = doc.getString("profileImageUrl") ?: ""
                    
                    binding.tvChatTitle.text = name
                    binding.tvChatSubtitle.text = "Online"

                    messageAdapter.updateReceivedUser(imgUrl, name)
                }
        }

        messageAdapter = MessageAdapter(currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter

        if (tailorId.isNotEmpty()) {
            // Single chat mode
            binding.layoutInput.visibility = View.VISIBLE
            binding.etSearch.visibility = View.GONE
            
            val chatId = if (currentUserId < tailorId) "${currentUserId}_${tailorId}" else "${tailorId}_${currentUserId}"
            chatViewModel.markAsRead(chatId, currentUserId)
            
            if (currentUserId.isNotEmpty()) {
                chatViewModel.listenToMessages(currentUserId, tailorId)
            }
            
            chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
                messageAdapter.submitList(messages)
                if (messages.isEmpty()) {
                    binding.tvEmptyChat.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyChat.visibility = View.GONE
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                    chatViewModel.markAsRead(chatId, currentUserId)
                }
            }

            binding.btnSend.setOnClickListener {
                val text = binding.etMessage.text.toString().trim()
                if (text.isNotEmpty() && tailorId.isNotEmpty() && currentUserId.isNotEmpty()) {
                    chatViewModel.sendMessage(currentUserId, tailorId, text)
                    binding.etMessage.setText("")
                }
            }
        } else {
            // Chat list mode
            binding.layoutInput.visibility = View.GONE
            binding.btnBack.visibility = View.GONE
            (binding.rvMessages.layoutManager as? LinearLayoutManager)?.stackFromEnd = false
            
            binding.tvChatTitle.text = "Recent Chats"
            binding.tvChatSubtitle.text = "Your active conversations"
            
            if (currentUserId.isNotEmpty()) {
                chatViewModel.loadRecentChats(currentUserId)
            }

            chatViewModel.recentChats.observe(viewLifecycleOwner) { chats ->
                allRecentChats = chats
                if (chats.isEmpty()) {
                    binding.tvEmptyChat.visibility = View.VISIBLE
                    binding.tvEmptyChat.text = "No recent chats yet"
                } else {
                    binding.tvEmptyChat.visibility = View.GONE
                    chatListAdapter = com.fitme.app.adapter.ChatListAdapter(
                        currentUserId, 
                        chats, 
                        { otherId ->
                            val bundle = Bundle().apply { putString("tailorId", otherId) }
                            findNavController().navigate(R.id.chatFragment, bundle)
                        },
                        { otherId ->
                            showDeleteChatDialog(otherId)
                        }
                    )
                    binding.rvMessages.adapter = chatListAdapter
                }
            }
        }
    }

    private fun setupVoiceRecording() {
        binding.btnVoice.setOnLongClickListener {
            if (checkPermission()) {
                startRecording()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
            true
        }

        binding.btnVoice.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && isRecording) {
                stopRecording(true)
            }
            false
        }
    }

    private fun checkPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        audioFilePath = "${requireContext().cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.m4a"
        mediaRecorder = android.media.MediaRecorder().apply {
            setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
                recordingStartTime = System.currentTimeMillis()
                binding.etMessage.hint = "Recording..."
                binding.btnVoice.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).start()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Recording failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording(send: Boolean) {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {}
        }
        mediaRecorder = null
        isRecording = false
        binding.etMessage.hint = "Message"
        binding.btnVoice.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()

        if (send && audioFilePath != null) {
            val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            if (duration < 1) {
                Toast.makeText(requireContext(), "Message too short", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = android.net.Uri.fromFile(java.io.File(audioFilePath!!))
            chatViewModel.sendVoiceMessage(currentUserId, tailorId, uri, duration)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChats(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterChats(query: String) {
        if (tailorId.isNotEmpty()) return

        if (query.isEmpty()) {
            chatListAdapter?.updateList(allRecentChats)
            return
        }

        val filtered = allRecentChats.filter { chat ->
            val otherId = chat.participants.find { it != currentUserId } ?: ""
            val otherName = chat.participantNames[otherId] ?: ""
            otherName.contains(query, ignoreCase = true)
        }
        chatListAdapter?.updateList(filtered)
    }

    private fun showDeleteChatDialog(otherId: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat history?")
            .setPositiveButton("Delete") { _, _ ->
                chatViewModel.deleteChat(currentUserId, otherId)
                Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        _binding = null
    }
}
