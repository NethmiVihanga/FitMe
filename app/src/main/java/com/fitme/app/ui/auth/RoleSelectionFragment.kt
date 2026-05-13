package com.fitme.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentRoleSelectionBinding
import com.fitme.app.viewmodel.AuthViewModel

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardCustomer.setOnClickListener {
            handleRoleSelection("customer")
        }

        binding.cardSeamstress.setOnClickListener {
            handleRoleSelection("seamstress")
        }
    }

    private fun handleRoleSelection(role: String) {
        val user = authViewModel.currentUser
        if (user != null) {
            // Google user - update role in Firestore
            authViewModel.updateRole(user.uid, role)
            saveRole(role)
            if (role == "customer") {
                findNavController().navigate(R.id.action_role_to_customer_home)
            } else {
                findNavController().navigate(R.id.action_role_to_seamstress_home)
            }
        } else {
            // New email registration - proceed to RegisterFragment
            val bundle = Bundle().apply { putString("role", role) }
            findNavController().navigate(R.id.action_role_to_register, bundle)
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
