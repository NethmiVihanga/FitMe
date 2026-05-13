package com.fitme.app.data.repository

import com.fitme.app.data.model.Portfolio
import com.fitme.app.data.model.Review
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TailorRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllSeamstresses(): Result<List<Pair<User, SeamstressProfile>>> {
        return try {
            val seamstressDocs = db.collection("seamstresses").get().await()
            val result = mutableListOf<Pair<User, SeamstressProfile>>()

            for (doc in seamstressDocs) {
                val profile = doc.toObject(SeamstressProfile::class.java)
                val userDoc = db.collection("users").document(doc.id).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null && user.role == "seamstress") {
                    result.add(Pair(user, profile))
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeamstressProfile(uid: String): Result<SeamstressProfile> {
        return try {
            val doc = db.collection("seamstresses").document(uid).get().await()
            val profile = doc.toObject(SeamstressProfile::class.java)
            if (profile != null) Result.success(profile)
            else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSeamstressProfile(profile: SeamstressProfile): Result<Unit> {
        return try {
            db.collection("seamstresses").document(profile.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPortfolio(seamstressId: String): Result<List<Portfolio>> {
        return try {
            val docs = db.collection("portfolio")
                .whereEqualTo("seamstressId", seamstressId)
                .get().await()
            val portfolio = docs.toObjects(Portfolio::class.java)
            Result.success(portfolio)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPortfolioItem(portfolio: Portfolio): Result<String> {
        return try {
            val docRef = db.collection("portfolio").add(portfolio).await()
            db.collection("portfolio").document(docRef.id)
                .update("portfolioId", docRef.id).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePortfolioItem(portfolioId: String): Result<Unit> {
        return try {
            db.collection("portfolio").document(portfolioId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePortfolioItem(portfolio: Portfolio): Result<Unit> {
        return try {
            db.collection("portfolio").document(portfolio.portfolioId).set(portfolio).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviews(seamstressId: String): Result<List<Review>> {
        return try {
            val docs = db.collection("reviews")
                .whereEqualTo("seamstressId", seamstressId)
                .get().await()
            val reviews = docs.toObjects(Review::class.java)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSeamstresses(query: String): Result<List<Pair<User, SeamstressProfile>>> {
        return try {
            val allResult = getAllSeamstresses()
            if (allResult.isSuccess) {
                val all = allResult.getOrNull() ?: emptyList()
                val filtered = all.filter { (user, _) ->
                    user.name.contains(query, ignoreCase = true) ||
                    user.location.contains(query, ignoreCase = true)
                }
                Result.success(filtered)
            } else {
                allResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToEarnings(seamstressId: String, onUpdate: (Result<Map<String, Any?>>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("earnings").document(seamstressId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(Result.failure(e))
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: emptyMap()
                onUpdate(Result.success(data))
            }
    }

    suspend fun getEarnings(seamstressId: String): Result<Map<String, Any?>> {
        return try {
            val allOrders = db.collection("orders")
                .whereEqualTo("seamstressId", seamstressId)
                .get().await()
            
            val seamstressOrders = allOrders.documents.filter { it.getString("status") == "delivered" }
            val pendingOrders = allOrders.documents.filter { 
                val status = it.getString("status")
                status in listOf("pending", "accepted", "in_progress", "ready") 
            }

            var total = 0.0
            var thisMonth = 0.0
            var pending = 0.0

            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            val completedOrdersCount = seamstressOrders.size

            for (doc in seamstressOrders) {
                val price = doc.getDouble("price") ?: 0.0
                total += price
                
                val updatedAt = doc.getLong("updatedAt") ?: 0L
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = updatedAt
                if (cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear) {
                    thisMonth += price
                }
            }

            for (doc in pendingOrders) {
                pending += (doc.getDouble("price") ?: 0.0)
            }

            val map = mapOf(
                "total" to total,
                "thisMonth" to thisMonth,
                "pending" to pending,
                "completedCount" to completedOrdersCount
            )
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactions(seamstressId: String): Result<List<Map<String, Any?>>> {
        return try {
            val allOrders = db.collection("orders")
                .whereEqualTo("seamstressId", seamstressId)
                .get().await()

            val seamstressOrders = allOrders.documents.filter { it.getString("status") == "delivered" }

            val txList = seamstressOrders.map { doc ->
                mapOf(
                    "customerName" to (doc.getString("customerName") ?: ""),
                    "amount" to (doc.getDouble("price") ?: 0.0),
                    "date" to (doc.getLong("updatedAt") ?: 0L),
                    "status" to "completed"
                )
            }
            Result.success(txList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentMethod(uid: String): Result<Map<String, String>> {
        return try {
            val doc = db.collection("payment_methods").document(uid).get().await()
            val userDoc = db.collection("users").document(uid).get().await()
            val userName = userDoc.getString("name") ?: "Unknown User"
            
            val map = mapOf(
                "bankName" to (doc.getString("bankName") ?: "Not Set"),
                "accountNumber" to (doc.getString("accountNumber") ?: "****"),
                "userName" to userName
            )
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentMethod(uid: String, bankName: String, accountNumber: String): Result<Unit> {
        return try {
            val map = mapOf("bankName" to bankName, "accountNumber" to accountNumber)
            db.collection("payment_methods").document(uid).set(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
