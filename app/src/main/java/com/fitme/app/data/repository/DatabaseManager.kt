package com.fitme.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DatabaseManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun resetDatabase(): Result<Unit> {
        return try {
            val collections = listOf(
                "users", "seamstresses", "orders", "chats", 
                "earnings", "transactions", "reviews", "portfolio"
            )

            for (collection in collections) {
                val snapshot = db.collection(collection).get().await()
                for (doc in snapshot.documents) {
                    // For chats, we also need to delete sub-collections
                    if (collection == "chats") {
                        val messages = db.collection("chats").document(doc.id).collection("messages").get().await()
                        for (msg in messages.documents) {
                            msg.reference.delete().await()
                        }
                    }
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
