package com.fitme.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.fitme.app.databinding.FragmentEditProfileBinding
import com.fitme.app.viewmodel.AuthViewModel
import com.fitme.app.viewmodel.TailorViewModel
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val tailorViewModel: TailorViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null
    private var seamstressProfile: SeamstressProfile? = null
    private var isImageRemoved = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            isImageRemoved = false
            Glide.with(this).load(it).into(binding.ivProfileImage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            isImageRemoved = true
            binding.ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        authViewModel.loadUserProfile()
        authViewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { user ->
                currentUser = user
                populateUserFields(user)
                
                if (user.role == "seamstress") {
                    binding.layoutSeamstressFields.visibility = View.VISIBLE
                    tailorViewModel.loadTailorProfile(user.uid)
                }
            }
        }

        tailorViewModel.tailorProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { profile ->
                seamstressProfile = profile
                populateSeamstressFields(profile)
            }
        }

        binding.btnSave.setOnClickListener { saveChanges() }
    }

    private fun populateUserFields(user: User) {
        binding.etName.setText(user.name)
        binding.etPhone.setText(user.phone)
        binding.etLocation.setText(user.location)
        if (user.profileImageUrl.isNotEmpty() && !isImageRemoved) {
            Glide.with(this).load(user.profileImageUrl).into(binding.ivProfileImage)
        }
    }

    private fun populateSeamstressFields(profile: SeamstressProfile) {
        binding.etBio.setText(profile.bio)
        binding.etSpecialties.setText(profile.specialties.joinToString(", "))
    }

    private fun saveChanges() {
        val user = currentUser ?: return
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name required"
            return
        }

        if (phone.isNotEmpty() && (phone.length != 10 || !phone.all { it.isDigit() })) {
            binding.etPhone.error = "Phone must be exactly 10 digits"
            return
        }

        if (selectedImageUri != null) {
            uploadImageAndSave(user, name, phone, location)
        } else {
            val finalImageUrl = if (isImageRemoved) "" else user.profileImageUrl
            finalizeUserUpdate(user, name, phone, location, finalImageUrl)
        }
    }

    private fun uploadImageAndSave(user: User, name: String, phone: String, location: String) {
        val uri = selectedImageUri ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    finalizeUserUpdate(user, name, phone, location, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                // Fallback to local URI string if upload fails
                finalizeUserUpdate(user, name, phone, location, uri.toString())
            }
    }

    private fun finalizeUserUpdate(user: User, name: String, phone: String, location: String, imageUrl: String) {
        val updatedUser = user.copy(
            name = name,
            phone = phone,
            location = location,
            profileImageUrl = imageUrl
        )
        authViewModel.updateProfile(updatedUser)

        if (user.role == "seamstress") {
            val bio = binding.etBio.text.toString().trim()
            val specialties = binding.etSpecialties.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val updatedProfile = (seamstressProfile ?: SeamstressProfile(uid = user.uid)).copy(
                bio = bio,
                specialties = specialties
            )
            tailorViewModel.updateProfile(updatedProfile)
        }

        Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
