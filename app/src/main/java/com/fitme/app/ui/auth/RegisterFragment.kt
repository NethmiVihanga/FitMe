package com.fitme.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentRegisterBinding
import com.fitme.app.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private var selectedRole = "customer"
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.signInWithGoogle(it, selectedRole) }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedRole = arguments?.getString("role") ?: "customer"

        setupGoogleSignIn()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupPasswordToggles()

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Please enter a valid email")
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                showError("Password does not meet all requirements")
                return@setOnClickListener
            }

            if (phone.length != 10 || !phone.all { it.isDigit() }) {
                showError("Phone number must be exactly 10 digits")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showError("Passwords do not match")
                return@setOnClickListener
            }

            authViewModel.register(email, password, name, phone, selectedRole)
        }

        binding.btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        observeViewModel()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun observeViewModel() {
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnRegister.isEnabled = !isLoading
            binding.btnGoogle.isEnabled = !isLoading
            binding.btnRegister.text = if (isLoading) "Creating account..." else "Register"
        }

        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                    saveRole(selectedRole)
                    if (selectedRole == "customer") {
                        findNavController().navigate(R.id.action_register_to_customer_home)
                    } else {
                        findNavController().navigate(R.id.action_register_to_seamstress_home)
                    }
                } else {
                    showError("Registration failed: ${it.exceptionOrNull()?.message}")
                }
            }
        }

        // Shared result with LoginFragment
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val user = it.getOrNull() ?: return@observe
                    navigateBasedOnRole(user.uid)
                } else {
                    Toast.makeText(requireContext(), "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateBasedOnRole(uid: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                if (isAdded) {
                    when (role) {
                        "customer" -> {
                            saveRole("customer")
                            findNavController().navigate(R.id.action_register_to_customer_home)
                        }
                        "seamstress" -> {
                            saveRole("seamstress")
                            findNavController().navigate(R.id.action_register_to_seamstress_home)
                        }
                        else -> findNavController().navigate(R.id.action_register_to_role)
                    }
                }
            }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 &&
               password.any { it.isUpperCase() } &&
               password.any { it.isDigit() } &&
               password.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }
    }

    private fun setupPasswordToggles() {
        var passVisible = false
        binding.btnTogglePassword.setOnClickListener {
            passVisible = !passVisible
            binding.etPassword.transformationMethod =
                if (passVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
            
            binding.btnTogglePassword.setImageResource(
                if (passVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye_open
            )
        }

        var confirmVisible = false
        binding.btnToggleConfirmPassword.setOnClickListener {
            confirmVisible = !confirmVisible
            binding.etConfirmPassword.transformationMethod =
                if (confirmVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
            
            binding.btnToggleConfirmPassword.setImageResource(
                if (confirmVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye_open
            )
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

