package com.fitme.app.data.repository

import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = ordersCollection.add(order).await()
            // Update the orderId in the document
            ordersCollection.document(docRef.id)
                .update("orderId", docRef.id)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("customerId", customerId)
                .get()
                .await()
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }.sortedByDescending { it.createdAt }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToCustomerOrders(customerId: String, onUpdate: (Result<List<Order>>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return ordersCollection
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(Result.failure(e))
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                onUpdate(Result.success(orders))
            }
    }

    fun listenToSeamstressOrders(seamstressId: String, onUpdate: (Result<List<Order>>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        // Combined listener for both assigned and open orders
        return ordersCollection
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(Result.failure(e))
                    return@addSnapshotListener
                }
                
                val allOrders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                } ?: emptyList()
                
                val filtered = allOrders.filter { 
                    it.seamstressId == seamstressId || (it.seamstressId.isEmpty() && it.status == OrderStatus.PENDING)
                }.sortedByDescending { it.createdAt }
                
                onUpdate(Result.success(filtered))
            }
    }

    suspend fun getSeamstressOrders(seamstressId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection.get().await()
            val allOrders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }
            val filtered = allOrders.filter { 
                it.seamstressId == seamstressId || (it.seamstressId.isEmpty() && it.status == OrderStatus.PENDING)
            }.sortedByDescending { it.createdAt }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptOrder(orderId: String, seamstressId: String, seamstressName: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "seamstressId" to seamstressId,
                        "seamstressName" to seamstressName,
                        "status" to OrderStatus.ACCEPTED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendQuote(orderId: String, seamstressId: String, seamstressName: String, quotedPrice: Double): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "seamstressId" to seamstressId,
                        "seamstressName" to seamstressName,
                        "quotedPrice" to quotedPrice,
                        "status" to OrderStatus.QUOTED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            
            // Notify customer via chat
            val order = ordersCollection.document(orderId).get().await().toObject(Order::class.java)
            if (order != null) {
                val chatRepo = ChatRepository()
                val message = "Hello! I have reviewed your design request and quoted Rs. ${quotedPrice.toInt()}. Please check your 'Pending' orders to approve it."
                chatRepo.sendMessage(seamstressId, order.customerId, message)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveQuote(orderId: String, finalPrice: Double): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "price" to finalPrice,
                        "status" to OrderStatus.ACCEPTED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNewOrdersForSeamstress(seamstressId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("seamstressId", seamstressId)
                .whereEqualTo("status", OrderStatus.PENDING)
                .get()
                .await()
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

            // Update earnings if delivered
            if (status == OrderStatus.DELIVERED) {
                val order = ordersCollection.document(orderId).get().await().toObject(Order::class.java)
                order?.let { updateSeamstressEarnings(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateSeamstressEarnings(order: Order) {
        try {
            val earningsRef = db.collection("earnings").document(order.seamstressId)
            val earningsDoc = earningsRef.get().await()
            val currentTotal = earningsDoc.getDouble("total") ?: 0.0
            val currentMonth = earningsDoc.getDouble("thisMonth") ?: 0.0

            val data = mapOf(
                "total" to currentTotal + order.price,
                "thisMonth" to currentMonth + order.price,
                "seamstressId" to order.seamstressId
            )
            earningsRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()

            // Add transaction record
            val transaction = hashMapOf(
                "transactionId" to "",
                "customerId" to order.customerId,
                "customerName" to order.customerName,
                "orderId" to order.orderId,
                "amount" to order.price,
                "status" to "completed",
                "date" to System.currentTimeMillis()
            )
            val txRef = db.collection("transactions").add(transaction).await()
            db.collection("transactions").document(txRef.id).update("transactionId", txRef.id).await()

        } catch (e: Exception) {
            // Log but don't fail
        }
    }

    suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            val order = doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            if (order != null) Result.success(order)
            else Result.failure(Exception("Order not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToOrderDetails(orderId: String, onUpdate: (Result<Order>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return ordersCollection.document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(Result.failure(e))
                    return@addSnapshotListener
                }
                val order = snapshot?.toObject(Order::class.java)?.copy(orderId = snapshot.id)
                if (order != null) {
                    onUpdate(Result.success(order))
                } else {
                    onUpdate(Result.failure(Exception("Order not found")))
                }
            }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendOrder(orderId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to OrderStatus.PENDING,
                "price" to 0.0,
                "quotedPrice" to 0.0,
                "updatedAt" to System.currentTimeMillis()
            )
            ordersCollection.document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
