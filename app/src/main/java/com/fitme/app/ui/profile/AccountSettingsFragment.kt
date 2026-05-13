package com.fitme.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.data.model.User
import com.fitme.app.databinding.FragmentAccountSettingsBinding
import com.fitme.app.viewmodel.AuthViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        authViewModel.loadUserProfile()
        authViewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { user ->
                currentUser = user
                binding.etName.setText(user.name)
                binding.etPhone.setText(user.phone)
                binding.etLocation.setText(user.location)
            }
        }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnResetData.setOnClickListener {
            showResetConfirmation()
        }
    }

    private fun showResetConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset All Data?")
            .setMessage("This will delete all users, orders, and chats from the database. This cannot be undone.")
            .setPositiveButton("Reset Everything") { _, _ ->
                resetDatabase()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetDatabase() {
        lifecycleScope.launch {
            val dbManager = com.fitme.app.data.repository.DatabaseManager()
            val result = dbManager.resetDatabase()
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Database Wiped Successfully", Toast.LENGTH_LONG).show()
                authViewModel.signOut()
                findNavController().navigate(R.id.loginFragment)
            } else {
                Toast.makeText(requireContext(), "Error resetting database", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveChanges() {
        val user = currentUser ?: return
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_empty_fields)
            return
        }

        if (phone.isNotEmpty() && (phone.length != 10 || !phone.all { it.isDigit() })) {
            binding.etPhone.error = "Phone must be exactly 10 digits"
            return
        }

        val updatedUser = user.copy(
            name = name,
            phone = phone,
            location = location
        )

        authViewModel.updateProfile(updatedUser)
        Toast.makeText(requireContext(), R.string.update_success, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
