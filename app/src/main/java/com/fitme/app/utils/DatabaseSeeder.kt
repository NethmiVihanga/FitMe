package com.fitme.app.utils

import android.content.Context
import android.widget.Toast
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.Portfolio
import com.fitme.app.data.model.Review
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.fitme.app.data.model.OrderStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object DatabaseSeeder {

    fun seedSampleOrdersForUser(userId: String, userName: String) {
        val firestore = FirebaseFirestore.getInstance()
        val baseTime = System.currentTimeMillis()

        val sampleOrders = listOf(
            Order(
                orderId = "sample_" + java.util.UUID.randomUUID().toString(),
                customerId = userId,
                customerName = userName,
                seamstressName = "Sarah Johnson",
                seamstressId = "tailor_sarah_123",
                description = "Floral Summer Dress (Sample)",
                status = OrderStatus.ACCEPTED,
                designImageUrl = "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&w=400&q=80",
                price = 4500.0,
                deliveryDate = "Oct 25, 2024",
                createdAt = baseTime - 100000
            ),
            Order(
                orderId = "sample_" + java.util.UUID.randomUUID().toString(),
                customerId = userId,
                customerName = userName,
                seamstressName = "Maria Silva",
                seamstressId = "tailor_maria_456",
                description = "Custom Blazer (Sample)",
                status = OrderStatus.IN_PROGRESS,
                designImageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=400&q=80",
                price = 6500.0,
                deliveryDate = "Nov 02, 2024",
                createdAt = baseTime - 200000
            ),
            Order(
                orderId = "sample_" + java.util.UUID.randomUUID().toString(),
                customerId = userId,
                customerName = userName,
                seamstressName = "Nisha Fernando",
                seamstressId = "tailor_nisha_789",
                description = "Wedding Reception Saree (Sample)",
                status = OrderStatus.READY,
                designImageUrl = "https://images.unsplash.com/photo-1583391733958-6928da5e1c8b?auto=format&fit=crop&w=400&q=80",
                price = 15000.0,
                deliveryDate = "Oct 20, 2024",
                createdAt = baseTime - 300000
            )
        )

        val batch = firestore.batch()
        sampleOrders.forEach { order ->
            val ref = firestore.collection("orders").document(order.orderId)
            batch.set(ref, order)
        }

        // Update user flag
        val userRef = firestore.collection("users").document(userId)
        batch.update(userRef, "hasSeededData", true)

        batch.commit()
    }

    fun seedDatabase(context: Context) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid ?: ""
        
        // Seed tailors (No login required)
        val sarahId = "tailor_sarah_123"
        val sarahUser = User(
            uid = sarahId, name = "Sarah Johnson", email = "sarah@fitme.com", 
            role = "seamstress", location = "Colombo",
            profileImageUrl = "https://i.pravatar.cc/300?img=47"
        )
        val sarahProfile = SeamstressProfile(
            uid = sarahId, specialties = listOf("Online", "Normal Tailor"),
            pricePerItem = 3500.0, rating = 4.2, yearsExperience = 8,
            bio = "I specialize in modern floral dresses and summer wear.",
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1550639525-c97d455acf70?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1539008835657-9e8e9680c956?auto=format&fit=crop&w=400&q=80"
            )
        )

        // 2. Seed Tailor "Maria Silva"
        val mariaId = "tailor_maria_456"
        val mariaUser = User(
            uid = mariaId, name = "Maria Silva", email = "maria@fitme.com", 
            role = "seamstress", location = "Colombo",
            profileImageUrl = "https://i.pravatar.cc/300?img=5"
        )
        val mariaProfile = SeamstressProfile(
            uid = mariaId, specialties = listOf("Offline Work", "Traditional"),
            pricePerItem = 2500.0, rating = 3.0, yearsExperience = 10,
            bio = "Expert in traditional wear and custom blazers.",
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1583391733958-6928da5e1c8b?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1594938298596-70f56fb3cecb?auto=format&fit=crop&w=400&q=80"
            )
        )

        // 3. Seed Tailor "Nisha Fernando"
        val nishaId = "tailor_nisha_789"
        val nishaUser = User(
            uid = nishaId, name = "Nisha Fernando", email = "nisha@fitme.com", 
            role = "seamstress", location = "Colombo",
            profileImageUrl = "https://i.pravatar.cc/300?img=9"
        )
        val nishaProfile = SeamstressProfile(
            uid = nishaId, specialties = listOf("Bridal", "Evening Wear"),
            pricePerItem = 12000.0, rating = 4.9, yearsExperience = 7,
            bio = "Bridal expert with 7 years of high-end fashion experience.",
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1566810141639-65b1d9ed3015?auto=format&fit=crop&w=400&q=80"
            )
        )

        // 4. Seed Tailor "Elena Rodriguez"
        val elenaId = "tailor_elena_001"
        val elenaUser = User(
            uid = elenaId, name = "Elena Rodriguez", email = "elena@fitme.com", 
            role = "seamstress", location = "Galle",
            profileImageUrl = "https://i.pravatar.cc/300?img=32"
        )
        val elenaProfile = SeamstressProfile(
            uid = elenaId, specialties = listOf("Elegant Gowns", "Formal Wear"),
            pricePerItem = 18000.0, rating = 4.8, yearsExperience = 12,
            bio = "Master of silk and lace. specialized in wedding and gala attire.",
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1574291813946-ed3e362d3943?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1598559069352-3d8437b0d427?auto=format&fit=crop&w=400&q=80"
            )
        )

        // 5. Seed Tailor "Maya Patel"
        val mayaId = "tailor_maya_002"
        val mayaUser = User(
            uid = mayaId, name = "Maya Patel", email = "maya@fitme.com", 
            role = "seamstress", location = "Kandy",
            profileImageUrl = "https://i.pravatar.cc/300?img=26"
        )
        val mayaProfile = SeamstressProfile(
            uid = mayaId, specialties = listOf("Casual Wear", "Custom Skirts"),
            pricePerItem = 1500.0, rating = 3.5, yearsExperience = 4,
            bio = "I create comfortable and stylish daily wear.",
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1589156280159-27698a70f29e?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1591369822096-ffd140ec948f?auto=format&fit=crop&w=400&q=80"
            )
        )

        // Push Users
        firestore.collection("users").document(sarahId).set(sarahUser)
        firestore.collection("users").document(mariaId).set(mariaUser)
        firestore.collection("users").document(nishaId).set(nishaUser)
        firestore.collection("users").document(elenaId).set(elenaUser)
        firestore.collection("users").document(mayaId).set(mayaUser)
        
        // Push Profiles (Fixed collection name to match repository)
        firestore.collection("seamstresses").document(sarahId).set(sarahProfile)
        firestore.collection("seamstresses").document(mariaId).set(mariaProfile)
        firestore.collection("seamstresses").document(nishaId).set(nishaProfile)
        firestore.collection("seamstresses").document(elenaId).set(elenaProfile)
        firestore.collection("seamstresses").document(mayaId).set(mayaProfile)

        // Seed Recent Orders for evaluating user
        val baseTime = System.currentTimeMillis()
        
        val order1Id = "mock_order_1"
        val order1 = Order(
            orderId = order1Id, customerId = currentUserId, 
            seamstressId = sarahId, seamstressName = "Sarah Johnson",
            description = "Floral Summer Dress", status = "accepted",
            designImageUrl = "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&w=400&q=80",
            price = 4500.0, size = "M",
            deliveryDate = "Oct 25, 2024", createdAt = baseTime - 100000
        )

        val order2Id = "mock_order_2"
        val order2 = Order(
            orderId = order2Id, customerId = currentUserId, 
            seamstressId = mariaId, seamstressName = "Maria Silva",
            description = "Custom Blazer", status = "in_progress",
            designImageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=400&q=80",
            price = 6500.0, size = "L",
            deliveryDate = "Nov 02, 2024", createdAt = baseTime - 200000
        )

        val order3Id = "mock_order_3"
        val order3 = Order(
            orderId = order3Id, customerId = currentUserId, 
            seamstressId = nishaId, seamstressName = "Nisha Fernando",
            description = "Wedding Reception Saree", status = "ready",
            designImageUrl = "https://images.unsplash.com/photo-1583391733958-6928da5e1c8b?auto=format&fit=crop&w=400&q=80",
            price = 15000.0, size = "L",
            deliveryDate = "Oct 20, 2024", createdAt = baseTime - 300000
        )

        val order4Id = "mock_order_4"
        val order4 = Order(
            orderId = order4Id, customerId = currentUserId,
            seamstressId = elenaId, seamstressName = "Elena Rodriguez",
            description = "Silk Evening Gown", status = "delivered",
            designImageUrl = "https://images.unsplash.com/photo-1574291813946-ed3e362d3943?auto=format&fit=crop&w=400&q=80",
            price = 18000.0, size = "S",
            deliveryDate = "Oct 10, 2024", createdAt = baseTime - 400000
        )

        val order5Id = "mock_order_5"
        val order5 = Order(
            orderId = order5Id, customerId = currentUserId,
            seamstressId = mayaId, seamstressName = "Maya Patel",
            description = "Cotton Summer Skirt", status = "pending",
            designImageUrl = "https://images.unsplash.com/photo-1591369822096-ffd140ec948f?auto=format&fit=crop&w=400&q=80",
            price = 2200.0, size = "M",
            deliveryDate = "Nov 15, 2024", createdAt = baseTime - 500000
        )

        val order6Id = "mock_order_6"
        val order6 = Order(
            orderId = order6Id, customerId = currentUserId,
            seamstressId = sarahId, seamstressName = "Sarah Johnson",
            description = "Modern Office Wear", status = "pending",
            designImageUrl = "https://images.unsplash.com/photo-1539008835657-9e8e9680c956?auto=format&fit=crop&w=400&q=80",
            price = 5500.0, size = "L",
            deliveryDate = "Nov 20, 2024", createdAt = baseTime - 50000
        )

        firestore.collection("orders").document(order1Id).set(order1)
        firestore.collection("orders").document(order2Id).set(order2)
        firestore.collection("orders").document(order3Id).set(order3)
        firestore.collection("orders").document(order4Id).set(order4)
        firestore.collection("orders").document(order5Id).set(order5)
        firestore.collection("orders").document(order6Id).set(order6)

        // Seed Portfolio items (Separate collection)
        val portfolios = listOf(
            Portfolio("p1", sarahId, sarahProfile.portfolioImages[0], "Summer Dress", "Cotton floral dress"),
            Portfolio("p2", sarahId, sarahProfile.portfolioImages[1], "Evening Wear", "Silk finish"),
            Portfolio("p3", mariaId, mariaProfile.portfolioImages[0], "Formal Blazer", "Perfect for office"),
            Portfolio("p4", nishaId, nishaProfile.portfolioImages[0], "Bridal Saree", "Handcrafted lace"),
            Portfolio("p5", elenaId, elenaProfile.portfolioImages[0], "Gala Gown", "Red carpet ready")
        )
        portfolios.forEach { firestore.collection("portfolio").document(it.portfolioId).set(it) }

        // Seed Reviews
        val reviews = listOf(
            Review("rev1", "user1", "John Doe", sarahId, 5, "Excellent fit and fast delivery!"),
            Review("rev2", "user2", "Emily S.", sarahId, 4, "Great quality, highly recommend."),
            Review("rev3", "user3", "Michael B.", mariaId, 5, "The blazer fits like a glove."),
            Review("rev4", "user4", "Sophia L.", nishaId, 5, "Nisha is a true artist with fabric.")
        )
        reviews.forEach { firestore.collection("reviews").document(it.reviewId).set(it) }
        
        Toast.makeText(context, "Data synced successfully!", Toast.LENGTH_SHORT).show()
    }
}
