package com.fitme.app.data.repository

import com.fitme.app.data.model.Chat
import com.fitme.app.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    suspend fun sendImageMessage(senderId: String, receiverId: String, imageUri: android.net.Uri): Result<Unit> {
        return try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("chat_images/${java.util.UUID.randomUUID()}.jpg")
            
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val chatId = getChatId(senderId, receiverId)
            val message = Message(
                senderId = senderId,
                type = "image",
                imageUrl = downloadUrl,
                timestamp = System.currentTimeMillis()
            )
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(message).await()

            updateChatMetadata(chatId, senderId, receiverId, "[Image]")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVoiceMessage(senderId: String, receiverId: String, audioUri: android.net.Uri, duration: Int): Result<Unit> {
        return try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("chat_audio/${java.util.UUID.randomUUID()}.m4a")
            
            storageRef.putFile(audioUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val chatId = getChatId(senderId, receiverId)
            val message = Message(
                senderId = senderId,
                type = "voice",
                audioUrl = downloadUrl,
                duration = duration,
                timestamp = System.currentTimeMillis()
            )
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(message).await()

            updateChatMetadata(chatId, senderId, receiverId, "🎤 Voice Message (${duration}s)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateChatMetadata(chatId: String, senderId: String, receiverId: String, lastMessage: String) {
        val senderDoc = db.collection("users").document(senderId).get().await()
        val receiverDoc = db.collection("users").document(receiverId).get().await()
        
        val senderName = senderDoc.getString("name") ?: "User"
        val receiverName = receiverDoc.getString("name") ?: "User"
        val senderImage = senderDoc.getString("profileImageUrl") ?: ""
        val receiverImage = receiverDoc.getString("profileImageUrl") ?: ""

        val chatDoc = db.collection("chats").document(chatId).get().await()
        val currentUnread = (chatDoc.get("unreadCount") as? Map<*, *>)?.filterKeys { it is String }?.filterValues { it is Long } as? Map<String, Long> ?: emptyMap()
        val newUnread = currentUnread.toMutableMap()
        newUnread[receiverId] = (newUnread[receiverId] ?: 0L) + 1

        db.collection("chats").document(chatId).set(
            mapOf(
                "chatId" to chatId,
                "participants" to listOf(senderId, receiverId),
                "participantNames" to mapOf(senderId to senderName, receiverId to receiverName),
                "participantImages" to mapOf(senderId to senderImage, receiverId to receiverImage),
                "unreadCount" to newUnread,
                "lastMessage" to lastMessage,
                "lastUpdated" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun sendMessage(senderId: String, receiverId: String, text: String): Result<Unit> {
        return try {
            val chatId = getChatId(senderId, receiverId)
            val message = Message(
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(message).await()

            updateChatMetadata(chatId, senderId, receiverId, text)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(chatId: String, userId: String) {
        try {
            val chatDoc = db.collection("chats").document(chatId).get().await()
            if (chatDoc.exists()) {
                val currentUnread = (chatDoc.get("unreadCount") as? Map<*, *>)?.filterKeys { it is String }?.filterValues { it is Long } as? Map<String, Long> ?: emptyMap()
                val newUnread = currentUnread.toMutableMap()
                newUnread[userId] = 0L
                db.collection("chats").document(chatId).update("unreadCount", newUnread).await()
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun getMessages(userId: String, otherUserId: String): Result<List<Message>> {
        return try {
            val chatId = getChatId(userId, otherUserId)
            val docs = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            val messages = docs.toObjects(Message::class.java)
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToMessages(
        userId: String,
        otherUserId: String,
        onUpdate: (List<Message>) -> Unit
    ) {
        val chatId = getChatId(userId, otherUserId)
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val messages = it.toObjects(Message::class.java)
                    onUpdate(messages)
                }
            }
    }

    suspend fun getRecentChats(userId: String): Result<List<Chat>> {
        return try {
            val docs = db.collection("chats")
                .whereArrayContains("participants", userId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get().await()
            val chats = docs.toObjects(Chat::class.java)
            Result.success(chats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToRecentChats(userId: String, onUpdate: (List<Chat>) -> Unit) {
        db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val chats = it.toObjects(Chat::class.java)
                    onUpdate(chats)
                }
            }
    }

    suspend fun deleteChat(userId: String, otherUserId: String): Result<Unit> {
        return try {
            val chatId = getChatId(userId, otherUserId)
            db.collection("chats").document(chatId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
