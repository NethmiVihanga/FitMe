package com.fitme.app.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "customer" or "seamstress"
    val location: String = "",
    val profileImageUrl: String = "",
    val isPro: Boolean = false,
    val proExpiry: Long = 0,
    val hasSeededData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SeamstressProfile(
    val uid: String = "",
    val bio: String = "",
    val specialties: List<String> = emptyList(),
    val pricePerItem: Double = 0.0,
    val rating: Double = 0.0,
    val totalProjects: Int = 0,
    val onTimePercent: Int = 0,
    val portfolioImages: List<String> = emptyList(),
    val yearsExperience: Int = 0
)

data class Order(
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val seamstressId: String = "",
    val seamstressName: String = "",
    val designImageUrl: String = "",
    val description: String = "",
    val fabricType: String = "",
    val size: String = "",
    val budgetMin: Double = 0.0,
    val budgetMax: Double = 0.0,
    val price: Double = 0.0,
    val quotedPrice: Double = 0.0,
    val deliveryDate: String = "",
    val status: String = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object OrderStatus {
    const val PENDING = "pending"
    const val QUOTED = "quoted"      // Seamstress sent a price quote, waiting for customer
    const val ACCEPTED = "accepted"  // Customer approved and paid
    const val IN_PROGRESS = "in_progress"
    const val READY = "ready"
    const val DELIVERED = "delivered"
    const val CANCELLED = "cancelled"
    const val DECLINED = "declined"
}

data class Portfolio(
    val portfolioId: String = "",
    val seamstressId: String = "",
    val imageUrl: String = "",
    val dressType: String = "",
    val description: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text", // "text", "image", or "voice"
    val imageUrl: String = "",
    val audioUrl: String = "",
    val duration: Int = 0, // in seconds
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantImages: Map<String, String> = emptyMap(),
    val unreadCount: Map<String, Int> = emptyMap(),
    val lastMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Transaction(
    val transactionId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val orderId: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    val date: Long = System.currentTimeMillis()
)

data class Earnings(
    val seamstressId: String = "",
    val total: Double = 0.0,
    val thisMonth: Double = 0.0,
    val pending: Double = 0.0,
    val transactions: List<Transaction> = emptyList()
)

data class Review(
    val reviewId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val seamstressId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// 3D Tailor Annotation: A pin dropped on the 3D viewer canvas by the customer
data class DressAnnotation(
    val annotationId: String = "",
    val orderId: String = "",         // optional, if linked to an order
    val customerId: String = "",
    val customerName: String = "",
    val xPercent: Float = 0f,          // x position as % of canvas width (0..1)
    val yPercent: Float = 0f,          // y position as % of canvas height (0..1)
    val note: String = "",
    val partLabel: String = "",        // e.g. "Sleeve", "Collar", "Bodice"
    val createdAt: Long = System.currentTimeMillis()
)
