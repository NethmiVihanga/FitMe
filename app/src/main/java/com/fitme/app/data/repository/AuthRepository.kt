package com.fitme.app.data.repository

import com.fitme.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String, phone: String, role: String): Result<FirebaseUser> {
        return try {
            // Check if phone number already exists
            val phoneQuery = db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .await()
            
            if (!phoneQuery.isEmpty) {
                return Result.failure(Exception("This phone number is already registered with another account."))
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            // Save user profile to Firestore
            val userModel = User(
                uid = user.uid,
                name = name,
                email = email,
                phone = phone,
                role = role
            )
            db.collection("users").document(user.uid).set(userModel).await()

            // If seamstress, create seamstress profile
            if (role == "seamstress") {
                initializeSeamstressProfile(user.uid)
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String, role: String = ""): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            // Check if user profile exists, if not create one
            val doc = db.collection("users").document(user.uid).get().await()
            if (!doc.exists()) {
                // New Google user - use the provided role
                val userModel = User(
                    uid = user.uid,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "",
                    profileImageUrl = user.photoUrl?.toString() ?: "",
                    role = role
                )
                db.collection("users").document(user.uid).set(userModel).await()

                // If seamstress, initialize profiles
                if (role == "seamstress") {
                    initializeSeamstressProfile(user.uid)
                }
            } else {
                // If user exists but role was empty (fallback), update it if provided
                val existingRole = doc.getString("role") ?: ""
                if (existingRole.isEmpty() && role.isNotEmpty()) {
                    db.collection("users").document(user.uid).update("role", role).await()
                    if (role == "seamstress") {
                        initializeSeamstressProfile(user.uid)
                    }
                }
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun initializeSeamstressProfile(uid: String) {
        val seamstressData = hashMapOf(
            "uid" to uid,
            "bio" to "",
            "specialties" to emptyList<String>(),
            "pricePerItem" to 0.0,
            "rating" to 0.0,
            "totalProjects" to 0,
            "onTimePercent" to 100,
            "portfolioImages" to emptyList<String>(),
            "yearsExperience" to 0
        )
        db.collection("seamstresses").document(uid).set(seamstressData).await()

        val earningsData = hashMapOf(
            "seamstressId" to uid,
            "total" to 0.0,
            "thisMonth" to 0.0,
            "pending" to 0.0
        )
        db.collection("earnings").document(uid).set(earningsData).await()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // Pre-check if user exists to avoid silent failures
            val userQuery = db.collection("users").whereEqualTo("email", email).get().await()
            if (userQuery.isEmpty) {
                return Result.failure(Exception("No account found with this email address."))
            }

            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToUserProfile(uid: String, onUpdate: (User?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            onUpdate(user)
        }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(uid: String, role: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("role", role).await()
            if (role == "seamstress") {
                val seamstressData = hashMapOf(
                    "uid" to uid,
                    "bio" to "",
                    "specialties" to emptyList<String>(),
                    "pricePerItem" to 0.0,
                    "rating" to 0.0,
                    "totalProjects" to 0,
                    "onTimePercent" to 100,
                    "portfolioImages" to emptyList<String>(),
                    "yearsExperience" to 0
                )
                db.collection("seamstresses").document(uid).set(seamstressData).await()

                val earningsData = hashMapOf(
                    "seamstressId" to uid,
                    "total" to 0.0,
                    "thisMonth" to 0.0,
                    "pending" to 0.0
                )
                db.collection("earnings").document(uid).set(earningsData).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val userId = user.uid
                
                // Delete from Auth first (fails if recent login is required)
                // If this fails, we don't delete from Firestore yet.
                user.delete().await()
                
                // Now delete from Firestore
                db.collection("users").document(userId).delete().await()
                
                // Also delete seamstress profile if exists
                db.collection("seamstresses").document(userId).delete().await()
                db.collection("earnings").document(userId).delete().await()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user signed in"))
            }
        } catch (e: Exception) {
            if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                Result.failure(Exception("For security, please log out and log back in before deleting your account."))
            } else {
                Result.failure(e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
