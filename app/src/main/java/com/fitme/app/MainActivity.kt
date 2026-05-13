package com.fitme.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fitme.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var userRole: String? = null
    private var authListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    // Destinations that show bottom nav
    private val bottomNavDestinations = setOf(
        R.id.customerHomeFragment,
        R.id.myOrdersFragment,
        R.id.chatFragment,
        R.id.customerProfileFragment,
        R.id.seamstressHomeFragment,
        R.id.manageOrdersFragment,
        R.id.earningsFragment,
        R.id.seamstressProfileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Initialize bottom nav
        binding.bottomNav.setupWithNavController(navController)

        // Listen for auth changes to refresh role
        authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                fetchUserRole()
            } else {
                userRole = null
                // Clear saved role when logged out
                getSharedPreferences("FitMePrefs", android.content.Context.MODE_PRIVATE)
                    .edit().remove("user_role").apply()
                binding.bottomNav.visibility = View.GONE
            }
        }
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(authListener!!)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in bottomNavDestinations) {
                binding.bottomNav.visibility = View.VISIBLE
                updateBottomNavForRole(destination.id)
            } else {
                binding.bottomNav.visibility = View.GONE
            }
        }

        // Initial role fetch
        fetchUserRole()
    }

    override fun onDestroy() {
        super.onDestroy()
        authListener?.let { com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(it) }
    }

    private fun getSavedRole(): String? {
        val prefs = getSharedPreferences("FitMePrefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("user_role", null)
    }

    private fun saveRole(role: String) {
        val prefs = getSharedPreferences("FitMePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("user_role", role).apply()
    }

    private fun fetchUserRole() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Use saved role immediately for better UX
            val savedRole = getSavedRole()
            if (savedRole != null) {
                userRole = savedRole
                updateBottomNavForRole(navController.currentDestination?.id ?: -1)
            }

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role")
                    if (role != null) {
                        userRole = role
                        saveRole(role)
                        // Refresh menu for current destination
                        navController.currentDestination?.id?.let { updateBottomNavForRole(it) }
                    }
                }
        }
    }

    private fun updateBottomNavForRole(destinationId: Int) {
        val currentRole = userRole ?: getSavedRole()
        
        val isSeamstress = when {
            currentRole == "seamstress" -> true
            currentRole == "customer" -> false
            // Fallback for seamstress destinations
            destinationId == R.id.seamstressHomeFragment ||
            destinationId == R.id.manageOrdersFragment ||
            destinationId == R.id.earningsFragment ||
            destinationId == R.id.seamstressProfileFragment -> true
            else -> false
        }

        val menuRes = if (isSeamstress) R.menu.menu_bottom_nav_seamstress else R.menu.menu_bottom_nav
        
        // Only inflate if the menu has changed to avoid unnecessary flickering and loss of state
        val currentMenuRes = binding.bottomNav.tag as? Int
        if (currentMenuRes != menuRes) {
            binding.bottomNav.menu.clear()
            binding.bottomNav.inflateMenu(menuRes)
            binding.bottomNav.tag = menuRes
            // IMPORTANT: Re-setup with NavController after inflating new menu
            binding.bottomNav.setupWithNavController(navController)
        }
    }
}
