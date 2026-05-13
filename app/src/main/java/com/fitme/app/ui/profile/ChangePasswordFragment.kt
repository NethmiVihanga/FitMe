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
import com.fitme.app.databinding.FragmentChangePasswordBinding
import com.fitme.app.viewmodel.AuthViewModel

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnUpdate.setOnClickListener {
            changePassword()
        }

        authViewModel.changePasswordResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(requireContext(), R.string.password_change_success, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }

        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnUpdate.isEnabled = !isLoading
            binding.btnUpdate.text = if (isLoading) "Updating..." else "Update Password"
        }
    }

    private fun changePassword() {
        val oldPass = binding.etOldPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            binding.etConfirmPassword.error = getString(R.string.error_password_mismatch)
            return
        }

        if (newPass.length < 6) {
            binding.etNewPassword.error = getString(R.string.error_short_password)
            return
        }

        authViewModel.changePassword(oldPass, newPass)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
