package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.fitme.app.R
import com.fitme.app.data.model.User
import com.fitme.app.databinding.FragmentCustomerProfileBinding
import com.fitme.app.viewmodel.AuthViewModel

class SeamstressProfileFragment : Fragment() {

    private var _binding: FragmentCustomerProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.loadUserProfile()
        authViewModel.userProfile.observe(viewLifecycleOwner) { result ->
            binding.loadingOverlay.visibility = View.GONE
            result?.getOrNull()?.let { user ->
                populateProfile(user)
            }
        }

        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_profile_to_edit)
        }

        binding.btnAccountSettings.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_profile_to_account_settings)
        }

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_profile_to_change_password)
        }

        binding.btnLogout.setOnClickListener {
            clearSavedRole()
            authViewModel.signOut()
            findNavController().navigate(R.id.action_seamstress_profile_to_login)
        }

        binding.btnDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Account?")
                .setMessage("Are you sure you want to delete your account permanently? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    authViewModel.deleteAccount()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        authViewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    clearSavedRole()
                    Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_seamstress_profile_to_login)
                } else {
                    Toast.makeText(requireContext(), it.exceptionOrNull()?.message ?: "Error deleting account", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun clearSavedRole() {
        val prefs = requireContext().getSharedPreferences("FitMePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("user_role").apply()
    }

    private fun populateProfile(user: User) {
        binding.tvName.text = user.name
        binding.tvEmail.text = user.email
        binding.tvPhone.text = user.phone.ifEmpty { "Not set" }
        binding.tvLocation.text = user.location.ifEmpty { "Not set" }
        
        Glide.with(this)
            .load(user.profileImageUrl)
            .placeholder(android.R.drawable.ic_menu_myplaces)
            .circleCrop()
            .into(binding.ivAvatar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
