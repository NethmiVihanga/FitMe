package com.fitme.app.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentForgotPasswordBinding
import com.fitme.app.viewmodel.AuthViewModel

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                showError("Please enter your email")
                return@setOnClickListener
            }

            authViewModel.sendPasswordReset(email)
        }

        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnReset.isEnabled = !isLoading
            binding.btnReset.text = if (isLoading) "Sending..." else "Send Reset Link"
        }

        authViewModel.resetResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Reset Email Sent")
                        .setMessage("A password reset link has been sent to your email. Please follow the instructions in the email to reset your password.")
                        .setPositiveButton("OK") { _, _ ->
                            authViewModel.clearResetResult()
                            findNavController().navigateUp()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    showError("Error: ${it.exceptionOrNull()?.message}")
                    authViewModel.clearResetResult()
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
