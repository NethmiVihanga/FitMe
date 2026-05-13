package com.fitme.app.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.ReviewAdapter
import com.fitme.app.databinding.FragmentTailorProfileBinding
import com.fitme.app.viewmodel.AuthViewModel
import com.fitme.app.viewmodel.TailorViewModel
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TailorProfileFragment : Fragment() {

    private var _binding: FragmentTailorProfileBinding? = null
    private val binding get() = _binding!!
    private val tailorViewModel: TailorViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var tailorId: String = ""
    private var tailorName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTailorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tailorId = arguments?.getString("tailorId") ?: ""

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Load tailor user info
        FirebaseFirestore.getInstance().collection("users").document(tailorId).get()
            .addOnSuccessListener { doc ->
                tailorName = doc.getString("name") ?: ""
                binding.tvTailorName.text = tailorName
                binding.tvTailorLocation.text = doc.getString("location") ?: "Sri Lanka"
                val imgUrl = doc.getString("profileImageUrl") ?: ""
                if (imgUrl.isNotEmpty()) {
                    Glide.with(this).load(imgUrl).into(binding.ivTailorAvatar)
                }
            }

        // Load profile
        tailorViewModel.loadTailorProfile(tailorId)
        tailorViewModel.tailorProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { profile ->
                binding.tvYearsExp.text = "${profile.yearsExperience}"
                binding.tvProjects.text = "${profile.totalProjects}+"
                binding.tvOnTime.text = "${profile.onTimePercent}%"
                binding.tvAbout.text = profile.bio.ifEmpty {
                    "Experienced seamstress specializing in cotton, formal wear and alterations. I take pride in delivering quality work that brings your unique style to life."
                }

                // Add specialties chips
                binding.chipGroupSpecialties.removeAllViews()
                profile.specialties.forEach { specialty ->
                    val chip = Chip(requireContext())
                    chip.text = specialty
                    chip.isClickable = false
                    binding.chipGroupSpecialties.addView(chip)
                }
            }
        }

        // Load reviews
        val reviewAdapter = ReviewAdapter()
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = reviewAdapter

        tailorViewModel.loadReviews(tailorId)
        tailorViewModel.reviews.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { reviewAdapter.submitList(it) }
        }

        // Chat with Tailor (envelope button)
        binding.btnChat.setOnClickListener {
            val bundle = Bundle().apply {
                putString("tailorId", tailorId)
            }
            findNavController().navigate(R.id.action_tailor_profile_to_chat, bundle)
        }

        // Place Order
        binding.btnPlaceOrder.setOnClickListener {
            val bundle = Bundle().apply {
                putString("seamstressId", tailorId)
            }
            findNavController().navigate(R.id.action_profile_to_upload, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
