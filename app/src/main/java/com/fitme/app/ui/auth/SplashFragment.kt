package com.fitme.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auto-login: if user is already signed in, skip login screen
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: ""
                    if (isAdded) {
                        when (role) {
                            "customer" -> {
                                saveRole("customer")
                                findNavController().navigate(R.id.action_splash_to_customer_home)
                            }
                            "seamstress" -> {
                                saveRole("seamstress")
                                findNavController().navigate(R.id.action_splash_to_seamstress_home)
                            }
                            else -> findNavController().navigate(R.id.action_splash_to_login)
                        }
                    }
                }
                .addOnFailureListener {
                    if (isAdded) findNavController().navigate(R.id.action_splash_to_login)
                }
            return
        }

        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_login)
        }
    }

    private fun saveRole(role: String) {
        val prefs = requireContext().getSharedPreferences("FitMePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("user_role", role).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
